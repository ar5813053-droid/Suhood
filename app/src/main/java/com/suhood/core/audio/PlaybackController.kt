package com.suhood.core.audio

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.suhood.core.audio.filter.IslamicContentClassifier
import com.suhood.core.model.AudioTrack
import com.suhood.core.model.PlaybackState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Shared Media3 ExoPlayer controller. All player mutations run on the main thread.
 * Stream queues (Quran) never fall back to the local sample catalog.
 */
class PlaybackController(
    private val context: Context,
    private val catalog: AudioCatalogProvider = LocalSampleCatalogProvider()
) {
    val player: ExoPlayer = ExoPlayer.Builder(context).build()

    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var positionJob: Job? = null

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.update { it.copy(isPlaying = isPlaying, positionMs = player.currentPosition) }
            if (isPlaying) startPositionUpdates() else positionJob?.cancel()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_ENDED -> {
                    runCatching {
                        if (player.hasNextMediaItem()) {
                            player.seekToNextMediaItem()
                            player.playWhenReady = true
                        } else {
                            _state.update { it.copy(isPlaying = false) }
                        }
                    }
                }
                Player.STATE_BUFFERING, Player.STATE_READY, Player.STATE_IDLE -> {
                    _state.update { it.copy(positionMs = player.currentPosition) }
                }
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val id = mediaItem?.mediaId ?: return
            val track = _state.value.queue.find { it.id == id }
            if (track != null) {
                _state.update {
                    it.copy(currentTrack = track, positionMs = 0L, isPlaying = player.isPlaying)
                }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            _error.value = error.message ?: "Playback error"
            _state.update { it.copy(isPlaying = false) }
            // Try to skip to next item instead of crashing
            runCatching {
                if (player.hasNextMediaItem()) {
                    player.seekToNextMediaItem()
                    player.prepare()
                    player.playWhenReady = true
                }
            }
        }
    }

    init {
        player.addListener(listener)
    }

    private fun onMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runCatching(block)
        } else {
            mainHandler.post { runCatching(block) }
        }
    }

    fun play(track: AudioTrack, queue: List<AudioTrack> = catalog.allTracks()) {
        onMain {
            _error.value = null
            val uri = catalog.resolveUri(context, track)
            if (uri == null) {
                _error.value = "Track unavailable"
                return@onMain
            }
            val meta = MediaMetadata.Builder()
                .setTitle(track.title.ifBlank { "Islamic audio" })
                .setArtist(track.creator.ifBlank { "Suhood" })
                .setAlbumTitle(track.collection)
                .setIsPlayable(true)
                .build()
            player.setMediaItem(
                MediaItem.Builder()
                    .setUri(uri)
                    .setMediaId(track.id)
                    .setMediaMetadata(meta)
                    .build()
            )
            player.prepare()
            player.playWhenReady = true
            _state.value = PlaybackState(
                currentTrack = track,
                isPlaying = true,
                positionMs = 0L,
                queue = queue,
                isDemoPlayback = true
            )
            startPositionUpdates()
            startServiceSafe()
        }
    }

    fun togglePlayPause() {
        onMain {
            if (player.isPlaying) player.pause() else player.play()
        }
    }

    fun pause() {
        onMain {
            player.pause()
            _state.update { it.copy(isPlaying = false) }
        }
    }

    fun stopAndClear() {
        onMain {
            positionJob?.cancel()
            player.stop()
            player.clearMediaItems()
            _state.value = PlaybackState()
        }
    }

    fun seekTo(positionMs: Long) {
        onMain {
            player.seekTo(positionMs.coerceAtLeast(0))
            _state.update { it.copy(positionMs = player.currentPosition) }
        }
    }

    fun next() {
        onMain {
            if (player.mediaItemCount > 0 && player.hasNextMediaItem()) {
                player.seekToNextMediaItem()
                player.playWhenReady = true
                return@onMain
            }
            val s = _state.value
            if (!s.isDemoPlayback) {
                player.pause()
                _state.update { it.copy(isPlaying = false) }
                return@onMain
            }
            val q = s.queue.ifEmpty { catalog.allTracks() }
            val track = s.currentTrack ?: return@onMain
            val i = q.indexOfFirst { it.id == track.id }.let { if (it < 0 || it >= q.lastIndex) 0 else it + 1 }
            play(q[i], q)
        }
    }

    fun previous() {
        onMain {
            if (player.currentPosition > 3000) {
                seekTo(0)
                return@onMain
            }
            if (player.mediaItemCount > 0 && player.hasPreviousMediaItem()) {
                player.seekToPreviousMediaItem()
                player.playWhenReady = true
                return@onMain
            }
            val s = _state.value
            if (!s.isDemoPlayback) {
                seekTo(0)
                return@onMain
            }
            val q = s.queue.ifEmpty { catalog.allTracks() }
            val track = s.currentTrack ?: return@onMain
            val i = q.indexOfFirst { it.id == track.id }.let { if (it <= 0) q.lastIndex else it - 1 }
            play(q[i], q)
        }
    }

    fun playUri(
        mediaId: String,
        title: String,
        artist: String,
        uriString: String,
        categoryId: String = "nasheeds"
    ) {
        onMain {
            val track = AudioTrack(
                id = mediaId,
                title = title,
                creator = artist,
                collection = categoryId,
                categoryId = categoryId,
                language = "",
                durationSeconds = 0,
                isDemo = false
            )
            if (!IslamicContentClassifier.isPlayable(track) &&
                IslamicContentClassifier.classify(track).state ==
                com.suhood.core.audio.filter.ContentClass.BLOCKED
            ) {
                _error.value = "Content outside Islamic preferences"
                return@onMain
            }
            if (uriString.isBlank()) {
                _error.value = "Invalid audio URL"
                return@onMain
            }
            _error.value = null
            val meta = MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setIsPlayable(true)
                .build()
            runCatching {
                player.stop()
                player.clearMediaItems()
                player.setMediaItem(
                    MediaItem.Builder()
                        .setUri(uriString)
                        .setMediaId(mediaId)
                        .setMediaMetadata(meta)
                        .build()
                )
                player.prepare()
                player.playWhenReady = true
                _state.value = PlaybackState(
                    currentTrack = track,
                    isPlaying = true,
                    positionMs = 0L,
                    queue = listOf(track),
                    isDemoPlayback = false
                )
                startPositionUpdates()
                startServiceSafe()
            }.onFailure {
                _error.value = it.message ?: "Playback failed"
                _state.update { s -> s.copy(isPlaying = false) }
            }
        }
    }

    fun playQueueUris(
        items: List<Triple<String, String, String>>,
        artist: String,
        startIndex: Int = 0
    ) {
        onMain {
            if (items.isEmpty()) {
                _error.value = "No audio items"
                return@onMain
            }
            val tracks = items.map { (id, title, _) ->
                AudioTrack(
                    id = id, title = title, creator = artist, collection = "Quran",
                    categoryId = "tilawat", language = "Arabic", durationSeconds = 0, isDemo = false
                )
            }
            val mediaItems = items.mapNotNull { (id, title, uri) ->
                if (uri.isBlank()) null
                else MediaItem.Builder()
                    .setUri(uri)
                    .setMediaId(id)
                    .setMediaMetadata(
                        MediaMetadata.Builder().setTitle(title).setArtist(artist).build()
                    )
                    .build()
            }
            if (mediaItems.isEmpty()) {
                _error.value = "No valid audio URLs"
                return@onMain
            }
            val idx = startIndex.coerceIn(0, mediaItems.lastIndex)
            runCatching {
                player.stop()
                player.clearMediaItems()
                player.setMediaItems(mediaItems, idx, 0L)
                player.prepare()
                player.playWhenReady = true
                _state.value = PlaybackState(
                    currentTrack = tracks.getOrNull(idx),
                    isPlaying = true,
                    queue = tracks,
                    isDemoPlayback = false
                )
                startPositionUpdates()
                startServiceSafe()
            }.onFailure {
                _error.value = it.message ?: "Playback failed"
                _state.update { s -> s.copy(isPlaying = false) }
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    private fun startPositionUpdates() {
        positionJob?.cancel()
        positionJob = scope.launch {
            while (isActive) {
                if (player.isPlaying) {
                    _state.update {
                        it.copy(isPlaying = true, positionMs = player.currentPosition)
                    }
                }
                delay(400)
            }
        }
    }

    private fun startServiceSafe() {
        runCatching {
            val intent = Intent(context, MediaPlaybackService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }
        // Ignore — audio still plays via in-process ExoPlayer even if service fails
    }

    fun release() {
        positionJob?.cancel()
        runCatching {
            player.removeListener(listener)
            player.release()
        }
    }
}

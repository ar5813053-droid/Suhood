package com.suhood.core.audio

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.suhood.core.model.AudioTrack
import com.suhood.core.model.PlaybackState
import com.suhood.core.audio.filter.IslamicContentClassifier
import androidx.media3.common.MediaMetadata
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
 * Shared Media3 ExoPlayer controller for the whole app (single instance via Application).
 *
 * Progress comes from the real player — not a fake timer.
 * Sample tones are test audio only ([isDemoPlayback] = true).
 */
class PlaybackController(
    private val context: Context,
    private val catalog: AudioCatalogProvider = LocalSampleCatalogProvider()
) {
    val player: ExoPlayer = ExoPlayer.Builder(context).build()

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
                Player.STATE_ENDED -> next()
                Player.STATE_BUFFERING, Player.STATE_READY, Player.STATE_IDLE -> {
                    _state.update { it.copy(positionMs = player.currentPosition) }
                }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            _error.value = error.message ?: "Playback error"
            _state.update { it.copy(isPlaying = false) }
        }
    }

    init {
        player.addListener(listener)
    }

    fun play(track: AudioTrack, queue: List<AudioTrack> = catalog.allTracks()) {
        _error.value = null
        val uri = catalog.resolveUri(context, track)
        if (uri == null) {
            _error.value = "Track unavailable"
            return
        }
        player.setMediaItem(MediaItem.fromUri(uri))
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
        startService()
    }

    fun togglePlayPause() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs.coerceAtLeast(0))
        _state.update { it.copy(positionMs = player.currentPosition) }
    }

    fun next() {
        val s = _state.value
        val q = s.queue.ifEmpty { catalog.allTracks() }
        val track = s.currentTrack ?: return
        val i = q.indexOfFirst { it.id == track.id }.let { if (it < 0 || it >= q.lastIndex) 0 else it + 1 }
        play(q[i], q)
    }

    fun previous() {
        val s = _state.value
        val q = s.queue.ifEmpty { catalog.allTracks() }
        val track = s.currentTrack ?: return
        if (player.currentPosition > 3000) {
            seekTo(0)
            return
        }
        val i = q.indexOfFirst { it.id == track.id }.let { if (it <= 0) q.lastIndex else it - 1 }
        play(q[i], q)
    }


    /**
     * Play a direct stream/file URI (Archive, Quran CDN, etc.).
     */
    fun playUri(
        mediaId: String,
        title: String,
        artist: String,
        uriString: String,
        categoryId: String = "nasheeds"
    ) {
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
            return
        }
        _error.value = null
        val meta = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setIsPlayable(true)
            .build()
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
        startService()
    }

    fun playQueueUris(
        items: List<Triple<String, String, String>>,
        artist: String,
        startIndex: Int = 0
    ) {
        if (items.isEmpty()) return
        val tracks = items.map { (id, title, _) ->
            AudioTrack(
                id = id, title = title, creator = artist, collection = "Quran",
                categoryId = "tilawat", language = "Arabic", durationSeconds = 0, isDemo = false
            )
        }
        val mediaItems = items.map { (id, title, uri) ->
            MediaItem.Builder()
                .setUri(uri)
                .setMediaId(id)
                .setMediaMetadata(
                    MediaMetadata.Builder().setTitle(title).setArtist(artist).build()
                )
                .build()
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
            startService()
        }.onFailure {
            _error.value = it.message ?: "Playback failed"
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
                        it.copy(
                            isPlaying = true,
                            positionMs = player.currentPosition
                        )
                    }
                }
                delay(400)
            }
        }
    }

    private fun startService() {
        runCatching {
            val intent = Intent(context, MediaPlaybackService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }
    }

    fun release() {
        positionJob?.cancel()
        player.removeListener(listener)
        player.release()
    }
}

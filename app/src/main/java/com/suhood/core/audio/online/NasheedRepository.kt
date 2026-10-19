package com.suhood.core.audio.online

import com.suhood.core.audio.PlaybackController
import com.suhood.core.model.AudioTrack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Coordinates online search → stream resolve → shared [PlaybackController].
 */
class NasheedRepository(
    private val searchProvider: IslamicAudioSearchProvider = InternetArchiveNasheedProvider(),
    private val resolver: AudioStreamResolver = InternetArchiveNasheedProvider(),
    private val playback: PlaybackController
) {
    private val _lastSearch = MutableStateFlow<SearchOutcome?>(null)
    val lastSearch: StateFlow<SearchOutcome?> = _lastSearch.asStateFlow()

    private val _resolving = MutableStateFlow(false)
    val resolving: StateFlow<Boolean> = _resolving.asStateFlow()

    suspend fun search(query: String): SearchOutcome {
        val outcome = searchProvider.search(query)
        _lastSearch.value = outcome
        return outcome
    }

    suspend fun playHit(hit: OnlineSearchHit): ResolveOutcome {
        _resolving.value = true
        return try {
            when (val resolved = resolver.resolve(hit)) {
                is ResolveOutcome.Ready -> {
                    val s = resolved.stream
                    playback.playUri(
                        mediaId = s.hit.id,
                        title = s.hit.title,
                        artist = s.hit.artist,
                        uriString = s.streamUrl,
                        categoryId = "nasheeds"
                    )
                    resolved
                }
                is ResolveOutcome.Failed -> resolved
            }
        } finally {
            _resolving.value = false
        }
    }

    fun hitAsTrack(hit: OnlineSearchHit): AudioTrack = AudioTrack(
        id = hit.id,
        title = hit.title,
        creator = hit.artist,
        collection = hit.providerId,
        categoryId = "nasheeds",
        language = "",
        durationSeconds = hit.durationSeconds,
        isDemo = false
    )
}

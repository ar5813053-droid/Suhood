package com.suhood.feature.nasheedify

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.suhood.core.audio.AudioCatalogProvider
import com.suhood.core.audio.LibraryRepository
import com.suhood.core.audio.PlaybackController
import com.suhood.core.audio.online.InternetArchiveNasheedProvider
import com.suhood.core.audio.online.NasheedRepository
import com.suhood.core.audio.online.OnlineSearchHit
import com.suhood.core.audio.online.ResolveOutcome
import com.suhood.core.audio.online.SearchOutcome
import com.suhood.core.model.AudioTrack
import com.suhood.core.model.PlaybackState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NasheedifyViewModel(
    private val playback: PlaybackController,
    private val library: LibraryRepository,
    private val catalog: AudioCatalogProvider,
    private val nasheedRepo: NasheedRepository
) : ViewModel() {

    val playbackState: StateFlow<PlaybackState> = playback.state
    val error: StateFlow<String?> = playback.error
    val favorites: StateFlow<List<String>> = library.observeFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val recent: StateFlow<List<String>> = library.observeRecent()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _onlineHits = MutableStateFlow<List<OnlineSearchHit>>(emptyList())
    val onlineHits: StateFlow<List<OnlineSearchHit>> = _onlineHits.asStateFlow()

    private val _searchMessage = MutableStateFlow<String?>(null)
    val searchMessage: StateFlow<String?> = _searchMessage.asStateFlow()

    private val _searching = MutableStateFlow(false)
    val searching: StateFlow<Boolean> = _searching.asStateFlow()

    private val _resolving = MutableStateFlow(false)
    val resolving: StateFlow<Boolean> = _resolving.asStateFlow()

    fun play(track: AudioTrack, queue: List<AudioTrack> = catalog.allTracks()) {
        playback.play(track, queue)
        viewModelScope.launch { library.addRecent(track.id) }
    }

    fun togglePlayPause() = playback.togglePlayPause()
    fun next() = playback.next()
    fun previous() = playback.previous()
    fun seekTo(ms: Long) = playback.seekTo(ms)
    fun toggleFavorite(trackId: String) {
        viewModelScope.launch { library.toggleFavorite(trackId) }
    }
    fun searchLocal(query: String): List<AudioTrack> = catalog.search(query)
    fun allTracks(): List<AudioTrack> = catalog.allTracks()
    fun clearError() = playback.clearError()

    /** Free Islamic online search (Internet Archive, filtered). */
    fun searchOnline(query: String) {
        viewModelScope.launch {
            _searching.value = true
            _searchMessage.value = null
            when (val outcome = nasheedRepo.search(query)) {
                is SearchOutcome.Success -> {
                    _onlineHits.value = outcome.hits
                    _searchMessage.value = if (outcome.hits.isEmpty()) "No results" else null
                }
                is SearchOutcome.Empty -> {
                    _onlineHits.value = emptyList()
                    _searchMessage.value = outcome.message
                }
                is SearchOutcome.Blocked -> {
                    _onlineHits.value = emptyList()
                    _searchMessage.value = outcome.message
                }
                is SearchOutcome.Error -> {
                    _onlineHits.value = emptyList()
                    _searchMessage.value = outcome.message
                }
            }
            _searching.value = false
        }
    }

    fun browseIslamic(category: String) {
        viewModelScope.launch {
            _searching.value = true
            _searchMessage.value = null
            val provider = InternetArchiveNasheedProvider()
            when (val outcome = provider.browseCategory(category)) {
                is SearchOutcome.Success -> {
                    _onlineHits.value = outcome.hits
                    _searchMessage.value = null
                }
                is SearchOutcome.Empty -> {
                    _onlineHits.value = emptyList()
                    _searchMessage.value = outcome.message
                }
                is SearchOutcome.Blocked -> {
                    _onlineHits.value = emptyList()
                    _searchMessage.value = outcome.message
                }
                is SearchOutcome.Error -> {
                    _onlineHits.value = emptyList()
                    _searchMessage.value = outcome.message
                }
            }
            _searching.value = false
        }
    }

    fun playOnlineHit(hit: OnlineSearchHit) {
        viewModelScope.launch {
            _resolving.value = true
            _searchMessage.value = null
            when (val r = nasheedRepo.playHit(hit)) {
                is ResolveOutcome.Ready -> {
                    library.addRecent(hit.id)
                }
                is ResolveOutcome.Failed -> {
                    _searchMessage.value = r.message
                }
            }
            _resolving.value = false
        }
    }

    class Factory(
        private val playback: PlaybackController,
        private val library: LibraryRepository,
        private val catalog: AudioCatalogProvider,
        private val nasheedRepo: NasheedRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NasheedifyViewModel(playback, library, catalog, nasheedRepo) as T
        }
    }
}

package com.suhood.feature.nasheedify

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.suhood.core.audio.AudioCatalogProvider
import com.suhood.core.audio.LibraryRepository
import com.suhood.core.audio.PlaybackController
import com.suhood.core.model.AudioTrack
import com.suhood.core.model.PlaybackState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NasheedifyViewModel(
    private val playback: PlaybackController,
    private val library: LibraryRepository,
    private val catalog: AudioCatalogProvider
) : ViewModel() {

    val playbackState: StateFlow<PlaybackState> = playback.state
    val error: StateFlow<String?> = playback.error
    val favorites: StateFlow<List<String>> = library.observeFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val recent: StateFlow<List<String>> = library.observeRecent()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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
    fun search(query: String): List<AudioTrack> = catalog.search(query)
    fun allTracks(): List<AudioTrack> = catalog.allTracks()
    fun clearError() = playback.clearError()

    class Factory(
        private val playback: PlaybackController,
        private val library: LibraryRepository,
        private val catalog: AudioCatalogProvider
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NasheedifyViewModel(playback, library, catalog) as T
        }
    }
}

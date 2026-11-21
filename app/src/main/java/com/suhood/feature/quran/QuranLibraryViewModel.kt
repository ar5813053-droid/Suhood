package com.suhood.feature.quran

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.suhood.core.quran.EditionDownloadState
import com.suhood.core.quran.FawazEdition
import com.suhood.core.quran.FawazLanguageGroup
import com.suhood.core.quran.LibraryEditionStatus
import com.suhood.core.quran.QuranLibraryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class QuranLibraryViewModel(
    app: Application,
    private val repo: QuranLibraryRepository = QuranLibraryRepository.get(app)
) : AndroidViewModel(app) {

    val loading = repo.loading
    val error = repo.error
    val languages = repo.languages
    val catalogue = repo.catalogue
    val downloadProgress = repo.downloadProgress

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _refreshTick = MutableStateFlow(0)
    val myLibrary: StateFlow<List<FawazEdition>> =
        combine(catalogue, _refreshTick) { _, _ -> repo.myLibraryEditions() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        viewModelScope.launch { repo.ensureCatalogue() }
    }

    fun setQuery(q: String) {
        _query.value = q
    }

    fun searchResults(): List<FawazEdition> = repo.search(_query.value)

    fun trending(): List<FawazEdition> = repo.trending()

    fun languageGroup(name: String): FawazLanguageGroup? =
        repo.languages.value.find { it.language.equals(name, true) }

    fun status(id: String): LibraryEditionStatus = repo.status(id)

    fun refreshStatuses() {
        _refreshTick.value = _refreshTick.value + 1
    }

    fun add(id: String) {
        repo.addToMyLibrary(id)
        refreshStatuses()
        _message.value = "Added to My Translations"
    }

    fun remove(id: String) {
        repo.removeFromMyLibrary(id)
        refreshStatuses()
    }

    fun download(edition: FawazEdition) {
        viewModelScope.launch {
            _message.value = "Downloading ${edition.language}…"
            val result = repo.downloadEdition(edition)
            refreshStatuses()
            _message.value = result.fold(
                onSuccess = { "Downloaded · available offline" },
                onFailure = { "Download failed: ${it.message}" }
            )
        }
    }

    fun removeDownload(id: String) {
        repo.removeDownload(id)
        refreshStatuses()
    }

    fun clearMessage() {
        _message.value = null
    }

    fun reloadCatalogue() {
        viewModelScope.launch { repo.ensureCatalogue(force = true) }
    }

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            QuranLibraryViewModel(app) as T
    }
}

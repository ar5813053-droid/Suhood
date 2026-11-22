package com.suhood.feature.quran

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.suhood.core.quran.LanguageTranslationOption
import com.suhood.core.quran.QuranLibraryStore
import com.suhood.core.quran.TranslationEditions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Quran Translations library — one preferred translation per language.
 * Selection persists and is applied in the existing Quran reader (no download gate).
 */
class QuranLibraryViewModel(
    app: Application
) : AndroidViewModel(app) {

    private val store = QuranLibraryStore.get(app)

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _languages = MutableStateFlow(TranslationEditions.preferredLanguages())
    val languages: StateFlow<List<LanguageTranslationOption>> = _languages.asStateFlow()

    private val _popular = MutableStateFlow(TranslationEditions.popularLanguages())
    val popular: StateFlow<List<LanguageTranslationOption>> = _popular.asStateFlow()

    private val _activeId = MutableStateFlow(store.activeTranslationId())
    val activeTranslationId: StateFlow<String> = _activeId.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun setQuery(q: String) {
        _query.value = q
        _languages.value = TranslationEditions.searchLanguages(q)
    }

    /**
     * Activate a language's preferred translation immediately.
     * No download required — reader loads on demand from AlQuran.Cloud.
     */
    fun selectLanguage(option: LanguageTranslationOption): String {
        val id = option.edition.id
        store.setActiveTranslationId(id)
        _activeId.value = id
        _message.value = "${option.language} translation selected"
        return id
    }

    fun clearMessage() {
        _message.value = null
    }

    fun refresh() {
        viewModelScope.launch {
            _activeId.value = store.activeTranslationId()
            _popular.value = TranslationEditions.popularLanguages()
            _languages.value = TranslationEditions.searchLanguages(_query.value)
        }
    }

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            QuranLibraryViewModel(app) as T
    }
}

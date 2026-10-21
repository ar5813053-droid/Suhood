package com.suhood.feature.quran

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.suhood.core.audio.PlaybackController
import com.suhood.core.database.QuranBookmarkEntity
import com.suhood.core.database.QuranLastReadEntity
import com.suhood.core.database.SuhoodDao
import com.suhood.core.quran.AlQuranCloudProvider
import com.suhood.core.quran.PageContent
import com.suhood.core.quran.SurahContent
import com.suhood.core.quran.SurahMeta
import com.suhood.core.quran.VerifiedQaris
import com.suhood.core.quran.TranslationEditions
import com.suhood.core.quran.TranslationEdition
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class QuranViewModel(
    private val dao: SuhoodDao,
    private val playback: PlaybackController,
    private val provider: AlQuranCloudProvider = AlQuranCloudProvider()
) : ViewModel() {

    private val _surahs = MutableStateFlow<List<SurahMeta>>(emptyList())
    val surahs: StateFlow<List<SurahMeta>> = _surahs.asStateFlow()

    private val _content = MutableStateFlow<SurahContent?>(null)
    val content: StateFlow<SurahContent?> = _content.asStateFlow()

    private val _page = MutableStateFlow<PageContent?>(null)
    val page: StateFlow<PageContent?> = _page.asStateFlow()

    private val _pageNumber = MutableStateFlow(1)
    val pageNumber: StateFlow<Int> = _pageNumber.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _showTranslation = MutableStateFlow(true)
    val showTranslation: StateFlow<Boolean> = _showTranslation.asStateFlow()

    /** Text translation edition id (e.g. en.sahih, ur.jalandhry). */
    private val _translationTextId = MutableStateFlow("en.sahih")
    val translationTextId: StateFlow<String> = _translationTextId.asStateFlow()

    /** When true, queue plays Arabic ayah then translation audio then next ayah. */
    private val _playTranslationAudio = MutableStateFlow(false)
    val playTranslationAudio: StateFlow<Boolean> = _playTranslationAudio.asStateFlow()

    /** Audio translation edition (e.g. en.walk). Only used when playTranslationAudio is true. */
    private val _translationAudioId = MutableStateFlow("en.walk")
    val translationAudioId: StateFlow<String> = _translationAudioId.asStateFlow()

    private val _qariId = MutableStateFlow(VerifiedQaris.list.first().id)
    val qariId: StateFlow<String> = _qariId.asStateFlow()

    val bookmarks = dao.observeBookmarks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        refreshSurahList()
    }

    fun refreshSurahList() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            runCatching { provider.listSurahs() }
                .onSuccess {
                    _surahs.value = it
                    if (it.size != 114) _error.value = "Unexpected surah count: ${it.size}"
                }
                .onFailure { _error.value = it.message ?: "Failed to load surah list" }
            _loading.value = false
        }
    }

    fun openSurah(number: Int) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            runCatching {
                    if (_showTranslation.value) {
                        provider.loadSurahWithTranslation(number, _translationTextId.value)
                    } else {
                        provider.loadSurahArabic(number)
                    }
                }
                .onSuccess {
                    _content.value = it
                    dao.setLastRead(QuranLastReadEntity(surah = number, ayah = 1))
                }
                .onFailure { _error.value = it.message ?: "Failed to load surah" }
            _loading.value = false
        }
    }

    fun toggleTranslationVisible() {
        _showTranslation.value = !_showTranslation.value
        val surah = _content.value?.meta?.number ?: return
        viewModelScope.launch { openSurah(surah) }
    }

    fun setTranslationTextEdition(id: String) {
        _translationTextId.value = id
        _showTranslation.value = true
        // Keep translation *audio* language in sync with text language
        val matched = TranslationEditions.audioForTextEdition(id)
        if (matched != null) {
            _translationAudioId.value = matched.id
        }
        // If user had audio on but this language has no audio, turn audio off
        if (_playTranslationAudio.value && matched == null) {
            _playTranslationAudio.value = false
        }
        val surah = _content.value?.meta?.number ?: return
        viewModelScope.launch { openSurah(surah) }
    }

    fun setPlayTranslationAudio(enabled: Boolean) {
        if (enabled) {
            val matched = TranslationEditions.audioForTextEdition(_translationTextId.value)
            if (matched == null) {
                _error.value =
                    "Translation audio is not available for this language yet. " +
                        "Available audio: English, Urdu. Text translation still works."
                _playTranslationAudio.value = false
                return
            }
            _translationAudioId.value = matched.id
            _playTranslationAudio.value = true
        } else {
            _playTranslationAudio.value = false
        }
        runCatching { playback.stopAndClear() }
    }

    fun setTranslationAudioEdition(id: String) {
        val ed = TranslationEditions.audioById(id) ?: return
        _translationAudioId.value = ed.id
        // Prefer a text edition in the same language so text matches audio
        val sameLangText = TranslationEditions.textEditions.find { it.langCode == ed.langCode }
        if (sameLangText != null && TranslationEditions.langCodeOfText(_translationTextId.value) != ed.langCode) {
            _translationTextId.value = sameLangText.id
            _showTranslation.value = true
            val surah = _content.value?.meta?.number
            if (surah != null) viewModelScope.launch { openSurah(surah) }
        }
        runCatching { playback.stopAndClear() }
    }

    /** Resolved audio folder language for UI (e.g. "Urdu"). */
    fun translationAudioLanguageLabel(): String =
        TranslationEditions.audioById(_translationAudioId.value)?.language
            ?: TranslationEditions.audioForTextEdition(_translationTextId.value)?.language
            ?: "—"

    fun setQari(id: String) {
        if (id.isBlank()) return
        if (VerifiedQaris.list.none { it.id == id }) return
        _qariId.value = id
        // Clear queue so next play uses the new reciter URLs (avoids crash / wrong audio)
        runCatching { playback.stopAndClear() }
    }

    fun bookmark(surah: Int, ayah: Int, note: String = "") {
        viewModelScope.launch {
            dao.upsertBookmark(
                QuranBookmarkEntity(
                    id = UUID.randomUUID().toString(),
                    surah = surah,
                    ayah = ayah,
                    note = note
                )
            )
        }
    }

    fun saveLastRead(surah: Int, ayah: Int) {
        viewModelScope.launch {
            dao.setLastRead(QuranLastReadEntity(surah = surah, ayah = ayah))
        }
    }

    fun playAyah(globalNumber: Int, surah: Int, ayahInSurah: Int) {
        if (globalNumber <= 0) {
            _error.value = "Invalid ayah"
            return
        }
        val qariId = _qariId.value.ifBlank { VerifiedQaris.list.first().id }
        val qari = VerifiedQaris.list.find { it.id == qariId }?.englishName ?: qariId
        val arUrl = provider.ayahAudioUrl(qariId, globalNumber)
        try {
            if (_playTranslationAudio.value) {
                val matched = TranslationEditions.audioForTextEdition(_translationTextId.value)
                val audioId = matched?.id ?: _translationAudioId.value
                if (matched != null) _translationAudioId.value = matched.id
                val trUrl = provider.translationAudioUrl(
                    audioId, surah, ayahInSurah
                )
                playback.playQueueUris(
                    listOf(
                        Triple("quran:$globalNumber", "Ayah $ayahInSurah", arUrl),
                        Triple("quran-tr:$globalNumber", "Ayah $ayahInSurah · Translation", trUrl)
                    ),
                    artist = qari,
                    startIndex = 0
                )
            } else {
                playback.playUri(
                    mediaId = "quran:$globalNumber",
                    title = "Surah $surah · Ayah $ayahInSurah",
                    artist = qari,
                    uriString = arUrl,
                    categoryId = "tilawat"
                )
            }
            saveLastRead(surah, ayahInSurah)
        } catch (t: Throwable) {
            _error.value = t.message ?: "Unable to play audio"
        }
    }

    fun playSurahFrom(startAyahIndex: Int = 0) {
        val ayahs = _content.value?.ayahs
        if (ayahs.isNullOrEmpty()) {
            _error.value = "Ayahs not loaded yet — wait a moment"
            return
        }
        val qariId = _qariId.value.ifBlank { VerifiedQaris.list.first().id }
        val qari = VerifiedQaris.list.find { it.id == qariId }?.englishName ?: qariId
        val withTr = _playTranslationAudio.value
        // Always prefer audio matching the selected text language
        val matchedAudio = TranslationEditions.audioForTextEdition(_translationTextId.value)
        val trEdition = matchedAudio?.id ?: _translationAudioId.value
        if (withTr && matchedAudio != null) {
            _translationAudioId.value = matchedAudio.id
        }
        val items = mutableListOf<Triple<String, String, String>>()
        for (a in ayahs) {
            items += Triple(
                "quran:${a.globalNumber}",
                "Ayah ${a.numberInSurah}",
                provider.ayahAudioUrl(qariId, a.globalNumber)
            )
            if (withTr) {
                val surahNum = _content.value?.meta?.number ?: 1
                items += Triple(
                    "quran-tr:${a.globalNumber}",
                    "Ayah ${a.numberInSurah} · Translation",
                    provider.translationAudioUrl(trEdition, surahNum, a.numberInSurah)
                )
            }
        }
        val factor = if (withTr) 2 else 1
        val safeIndex = (startAyahIndex.coerceIn(0, ayahs.lastIndex) * factor).coerceIn(0, items.lastIndex)
        runCatching {
            playback.playQueueUris(items, artist = qari, startIndex = safeIndex)
        }.onFailure {
            _error.value = it.message ?: "Unable to start playback"
        }
    }


    fun translatorNote(): String = provider.translatorAttribution()

    fun openPage(page: Int) {
        val p = page.coerceIn(1, 604)
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            _pageNumber.value = p
            runCatching { provider.loadPage(p) }
                .onSuccess { _page.value = it }
                .onFailure { _error.value = it.message ?: "Failed to load page $p" }
            _loading.value = false
        }
    }

    fun nextPage() = openPage(_pageNumber.value + 1)
    fun previousPage() = openPage(_pageNumber.value - 1)

    class Factory(
        private val dao: SuhoodDao,
        private val playback: PlaybackController
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            QuranViewModel(dao, playback) as T
    }
}

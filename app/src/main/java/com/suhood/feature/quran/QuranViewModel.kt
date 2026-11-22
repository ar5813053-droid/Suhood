package com.suhood.feature.quran

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.suhood.core.audio.PlaybackController
import com.suhood.core.database.QuranBookmarkEntity
import com.suhood.core.database.QuranLastReadEntity
import com.suhood.core.database.SuhoodDao
import com.suhood.core.quran.AlQuranCloudProvider
import com.suhood.core.quran.CanonicalAyahId
import com.suhood.core.quran.PageContent
import com.suhood.core.quran.QuranLibraryRepository
import com.suhood.core.quran.QuranLibraryStore
import com.suhood.core.quran.SurahContent
import com.suhood.core.quran.SurahMeta
import com.suhood.core.quran.TranslationEditions
import com.suhood.core.quran.VerifiedQaris
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

    /** Primary text translation edition id (e.g. en.sahih, ur.jalandhry). */
    private val _translationTextId = MutableStateFlow("en.sahih")
    val translationTextId: StateFlow<String> = _translationTextId.asStateFlow()

    /**
     * Ordered list of selected translation edition ids shown under each Ayah.
     * First entry is the primary [translationTextId].
     */
    private val _selectedTranslationIds = MutableStateFlow(listOf("en.sahih"))
    val selectedTranslationIds: StateFlow<List<String>> = _selectedTranslationIds.asStateFlow()

    /** When true, queue plays Arabic ayah then translation audio then next ayah. */
    private val _playTranslationAudio = MutableStateFlow(false)
    val playTranslationAudio: StateFlow<Boolean> = _playTranslationAudio.asStateFlow()

    /** Audio translation edition (e.g. en.walk). Only used when playTranslationAudio is true. */
    private val _translationAudioId = MutableStateFlow("en.walk")
    val translationAudioId: StateFlow<String> = _translationAudioId.asStateFlow()

    private val _qariId = MutableStateFlow(VerifiedQaris.list.first().id)

    /** Bumped on every new queue so stale async work cannot attach the wrong translation. */
    private val playbackGeneration = java.util.concurrent.atomic.AtomicLong(0L)
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
        // Stop previous surah queue so a prior translation cannot play against new text
        playbackGeneration.incrementAndGet()
        runCatching { playback.stopAndClear() }
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val base = if (_showTranslation.value) {
                    provider.loadSurahWithTranslations(
                        number,
                        _selectedTranslationIds.value.ifEmpty { listOf(_translationTextId.value) }
                    )
                } else {
                    provider.loadSurahArabic(number)
                }
                val merged = mergeFawazLibrary(base)
                _content.value = merged
                dao.setLastRead(QuranLastReadEntity(surah = number, ayah = 1))
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load surah"
            }
            _loading.value = false
        }
    }


    fun toggleTranslationVisible() {
        _showTranslation.value = !_showTranslation.value
        val surah = _content.value?.meta?.number ?: return
        viewModelScope.launch { openSurah(surah) }
    }

    fun setTranslationTextEdition(id: String) {
        val ed = TranslationEditions.textById(id)
        if (ed != null && !ed.available) {
            _error.value = ed.language + " is not available from a verified translation source yet."
            return
        }
        // One translation at a time — never stack multiple languages under each ayah.
        _translationTextId.value = id
        _selectedTranslationIds.value = listOf(id)
        _showTranslation.value = true
        libraryAppContext?.let { ctx ->
            runCatching { QuranLibraryStore.get(ctx).setActiveTranslationId(id) }
        }
        val matched = TranslationEditions.audioForTextEdition(id)
        if (matched != null) {
            _translationAudioId.value = matched.id
        }
        val surah = _content.value?.meta?.number
        if (surah != null) viewModelScope.launch { openSurah(surah) }
    }

    fun toggleTranslationEdition(id: String) {
        // One translation per language policy — selecting replaces the active edition.
        setTranslationTextEdition(id)
    }

    fun removeTranslationEdition(id: String) {
        val cur = _selectedTranslationIds.value.toMutableList()
        if (cur.size <= 1) return
        cur.remove(id)
        _selectedTranslationIds.value = cur
        _translationTextId.value = cur.first()
        val surah = _content.value?.meta?.number
        if (surah != null) viewModelScope.launch { openSurah(surah) }
    }

    fun moveTranslationEdition(id: String, up: Boolean) {
        val cur = _selectedTranslationIds.value.toMutableList()
        val i = cur.indexOf(id)
        if (i < 0) return
        val j = if (up) i - 1 else i + 1
        if (j !in cur.indices) return
        val tmp = cur[i]
        cur[i] = cur[j]
        cur[j] = tmp
        _selectedTranslationIds.value = cur
        _translationTextId.value = cur.first()
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
        val identity = runCatching {
            CanonicalAyahId(surah = surah, ayahInSurah = ayahInSurah, globalNumber = globalNumber)
        }.getOrElse {
            _error.value = "Invalid ayah identity (surah=$surah ayah=$ayahInSurah global=$globalNumber)"
            return
        }
        val qariId = _qariId.value.ifBlank { VerifiedQaris.list.first().id }
        val qari = VerifiedQaris.list.find { it.id == qariId }?.englishName ?: qariId
        val arUrl = provider.ayahAudioUrl(qariId, identity.globalNumber)
        val gen = playbackGeneration.incrementAndGet()
        try {
            if (_playTranslationAudio.value) {
                val matched = TranslationEditions.audioForTextEdition(_translationTextId.value)
                val audioId = matched?.id ?: _translationAudioId.value
                if (matched != null) _translationAudioId.value = matched.id
                val trUrl = provider.translationAudioUrlOrNull(
                    audioId, identity.surah, identity.ayahInSurah
                )
                if (trUrl == null) {
                    _error.value =
                        "No verified translation audio for Surah ${identity.surah} Ayah ${identity.ayahInSurah}"
                    // Still play Arabic only — never substitute another verse's translation
                    playback.playUri(
                        mediaId = identity.arabicMediaId(),
                        title = "Surah ${identity.surah} · Ayah ${identity.ayahInSurah}",
                        artist = qari,
                        uriString = arUrl,
                        categoryId = "tilawat"
                    )
                } else if (gen == playbackGeneration.get()) {
                    playback.playQueueUris(
                        listOf(
                            Triple(
                                identity.arabicMediaId(),
                                "Ayah ${identity.ayahInSurah}",
                                arUrl
                            ),
                            Triple(
                                identity.translationMediaId(),
                                "Ayah ${identity.ayahInSurah} · Translation",
                                trUrl
                            )
                        ),
                        artist = qari,
                        startIndex = 0
                    )
                }
            } else {
                playback.playUri(
                    mediaId = identity.arabicMediaId(),
                    title = "Surah ${identity.surah} · Ayah ${identity.ayahInSurah}",
                    artist = qari,
                    uriString = arUrl,
                    categoryId = "tilawat"
                )
            }
            saveLastRead(identity.surah, identity.ayahInSurah)
        } catch (t: Throwable) {
            _error.value = t.message ?: "Unable to play audio"
        }
    }

    fun playSurahFrom(startAyahIndex: Int = 0) {
        val content = _content.value
        val ayahs = content?.ayahs
        val surahNum = content?.meta?.number
        if (ayahs.isNullOrEmpty() || surahNum == null) {
            _error.value = "Ayahs not loaded yet — wait a moment"
            return
        }
        val qariId = _qariId.value.ifBlank { VerifiedQaris.list.first().id }
        val qari = VerifiedQaris.list.find { it.id == qariId }?.englishName ?: qariId
        val withTr = _playTranslationAudio.value
        val matchedAudio = TranslationEditions.audioForTextEdition(_translationTextId.value)
        val trEdition = matchedAudio?.id ?: _translationAudioId.value
        if (withTr && matchedAudio != null) {
            _translationAudioId.value = matchedAudio.id
        }
        playbackGeneration.incrementAndGet()
        val items = mutableListOf<Triple<String, String, String>>()
        for (a in ayahs) {
            val id = runCatching {
                CanonicalAyahId(surahNum, a.numberInSurah, a.globalNumber)
            }.getOrNull() ?: continue
            items += Triple(
                id.arabicMediaId(),
                "Ayah ${id.ayahInSurah}",
                provider.ayahAudioUrl(qariId, id.globalNumber)
            )
            if (withTr) {
                val trUrl = provider.translationAudioUrlOrNull(trEdition, id.surah, id.ayahInSurah)
                if (trUrl != null) {
                    items += Triple(
                        id.translationMediaId(),
                        "Ayah ${id.ayahInSurah} · Translation",
                        trUrl
                    )
                }
                // Skip missing translation audio rather than attach another ayah's file
            }
        }
        if (items.isEmpty()) {
            _error.value = "No playable audio for this surah"
            return
        }
        // Start at Arabic of requested ayah; count only arabic slots when translation pairs vary
        var arabicOrdinal = 0
        var startMediaIndex = 0
        val targetOrdinal = startAyahIndex.coerceIn(0, ayahs.lastIndex)
        for ((i, item) in items.withIndex()) {
            if (!item.first.startsWith("quran-tr:")) {
                if (arabicOrdinal == targetOrdinal) {
                    startMediaIndex = i
                    break
                }
                arabicOrdinal++
            }
        }
        runCatching {
            playback.playQueueUris(items, artist = qari, startIndex = startMediaIndex)
        }.onFailure {
            _error.value = it.message ?: "Unable to start playback"
        }
    }


    
    /** Play all ayahs on the current Mushaf page (Arabic, optional translation audio). */
    fun playCurrentPage(startIndex: Int = 0) {
        val ayahs = _page.value?.ayahs
        if (ayahs.isNullOrEmpty()) {
            _error.value = "Page not loaded yet"
            return
        }
        val qariId = _qariId.value.ifBlank { VerifiedQaris.list.first().id }
        val qari = VerifiedQaris.list.find { it.id == qariId }?.englishName ?: qariId
        val withTr = _playTranslationAudio.value
        val matchedAudio = TranslationEditions.audioForTextEdition(_translationTextId.value)
        val trEdition = matchedAudio?.id ?: _translationAudioId.value
        if (withTr && matchedAudio != null) {
            _translationAudioId.value = matchedAudio.id
        }
        playbackGeneration.incrementAndGet()
        val items = mutableListOf<Triple<String, String, String>>()
        for (a in ayahs) {
            val id = runCatching {
                CanonicalAyahId(a.surahNumber, a.numberInSurah, a.globalNumber)
            }.getOrNull() ?: continue
            items += Triple(
                id.arabicMediaId(),
                "Surah ${id.surah} · ${id.ayahInSurah}",
                provider.ayahAudioUrl(qariId, id.globalNumber)
            )
            if (withTr) {
                val trUrl = provider.translationAudioUrlOrNull(trEdition, id.surah, id.ayahInSurah)
                if (trUrl != null) {
                    items += Triple(
                        id.translationMediaId(),
                        "Surah ${id.surah} · ${id.ayahInSurah} · TR",
                        trUrl
                    )
                }
            }
        }
        if (items.isEmpty()) {
            _error.value = "No playable audio for this page"
            return
        }
        var arabicOrdinal = 0
        var startMediaIndex = 0
        val targetOrdinal = startIndex.coerceIn(0, ayahs.lastIndex)
        for ((i, item) in items.withIndex()) {
            if (!item.first.startsWith("quran-tr:")) {
                if (arabicOrdinal == targetOrdinal) {
                    startMediaIndex = i
                    break
                }
                arabicOrdinal++
            }
        }
        runCatching {
            playback.playQueueUris(items, artist = qari, startIndex = startMediaIndex)
        }.onFailure {
            _error.value = it.message ?: "Unable to play page"
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


    @Volatile
    var libraryAppContext: android.content.Context? = null
        set(value) {
            field = value
            // Restore the user's preferred single translation (one per language policy).
            if (value != null) {
                val saved = runCatching { QuranLibraryStore.get(value).activeTranslationId() }.getOrNull()
                if (!saved.isNullOrBlank() && saved != _translationTextId.value) {
                    val ed = TranslationEditions.textById(saved)
                    if (ed == null || ed.available) {
                        _translationTextId.value = saved
                        _selectedTranslationIds.value = listOf(saved)
                        TranslationEditions.audioForTextEdition(saved)?.let {
                            _translationAudioId.value = it.id
                        }
                    }
                }
            }
        }

    /**
     * Fawaz offline merge is intentionally a no-op for the primary reader path.
     * The library uses one preferred AlQuran.Cloud edition per language and loads
     * on demand — stacking extra catalogue ids would show technical identifiers.
     */
    private suspend fun mergeFawazLibrary(base: SurahContent): SurahContent = base

    class Factory(
        private val dao: SuhoodDao,
        private val playback: PlaybackController
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            QuranViewModel(dao, playback) as T
    }
}

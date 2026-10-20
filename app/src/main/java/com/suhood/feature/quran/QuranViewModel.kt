package com.suhood.feature.quran

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.suhood.core.audio.PlaybackController
import com.suhood.core.database.QuranBookmarkEntity
import com.suhood.core.database.QuranLastReadEntity
import com.suhood.core.database.SuhoodDao
import com.suhood.core.quran.AlQuranCloudProvider
import com.suhood.core.quran.SurahContent
import com.suhood.core.quran.SurahMeta
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

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _showUrdu = MutableStateFlow(true)
    val showUrdu: StateFlow<Boolean> = _showUrdu.asStateFlow()

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
            runCatching { provider.loadSurahWithUrdu(number) }
                .onSuccess {
                    _content.value = it
                    dao.setLastRead(QuranLastReadEntity(surah = number, ayah = 1))
                }
                .onFailure { _error.value = it.message ?: "Failed to load surah" }
            _loading.value = false
        }
    }

    fun toggleUrdu() {
        _showUrdu.value = !_showUrdu.value
    }

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
        if (globalNumber <= 0) return
        val qariId = _qariId.value.ifBlank { VerifiedQaris.list.first().id }
        val url = provider.ayahAudioUrl(qariId, globalNumber)
        val qari = VerifiedQaris.list.find { it.id == qariId }?.englishName ?: qariId
        runCatching {
            playback.playUri(
                mediaId = "quran:$globalNumber",
                title = "Surah $surah · Ayah $ayahInSurah",
                artist = qari,
                uriString = url,
                categoryId = "tilawat"
            )
            saveLastRead(surah, ayahInSurah)
        }.onFailure {
            _error.value = it.message ?: "Unable to play audio"
        }
    }

    fun playSurahFrom(startAyahIndex: Int = 0) {
        val c = _content.value
        if (c == null || c.ayahs.isEmpty()) {
            _error.value = "Ayahs not loaded yet"
            return
        }
        val qariId = _qariId.value.ifBlank { VerifiedQaris.list.first().id }
        val qari = VerifiedQaris.list.find { it.id == qariId }?.englishName ?: qariId
        val items = c.ayahs.map { a ->
            Triple(
                "quran:${a.globalNumber}",
                "Ayah ${a.numberInSurah}",
                provider.ayahAudioUrl(qariId, a.globalNumber)
            )
        }
        val safeIndex = startAyahIndex.coerceIn(0, items.lastIndex)
        runCatching {
            playback.playQueueUris(items, artist = qari, startIndex = safeIndex)
        }.onFailure {
            _error.value = it.message ?: "Unable to start playback"
        }
    }

    fun translatorNote(): String = provider.translatorAttribution()

    class Factory(
        private val dao: SuhoodDao,
        private val playback: PlaybackController
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            QuranViewModel(dao, playback) as T
    }
}

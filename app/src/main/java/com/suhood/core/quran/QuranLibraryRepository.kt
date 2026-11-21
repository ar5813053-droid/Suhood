package com.suhood.core.quran

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject

class QuranLibraryRepository(context: Context) {
    private val store = QuranLibraryStore.get(context)
    private val app = context.applicationContext

    private val _catalogue = MutableStateFlow<List<FawazEdition>>(emptyList())
    val catalogue: StateFlow<List<FawazEdition>> = _catalogue.asStateFlow()

    private val _languages = MutableStateFlow<List<FawazLanguageGroup>>(emptyList())
    val languages: StateFlow<List<FawazLanguageGroup>> = _languages.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Map<String, Int>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Int>> = _downloadProgress.asStateFlow()

    suspend fun ensureCatalogue(force: Boolean = false): List<FawazEdition> = withContext(Dispatchers.IO) {
        if (!force && _catalogue.value.isNotEmpty()) return@withContext _catalogue.value
        _loading.value = true
        _error.value = null
        try {
            val cached = if (!force && store.catalogueAgeMs() < 7L * 24 * 3600_000L) {
                store.loadCachedCatalogueJson()
            } else null
            val editions = if (cached != null) {
                parseCatalogueJson(cached)
            } else {
                val json = HttpJson.get(FawazQuranProvider.EDITIONS_URL, timeoutMs = 30_000)
                store.saveCatalogueJson(json)
                parseCatalogueJson(json)
            }
            _catalogue.value = editions
            _languages.value = FawazQuranProvider.groupByLanguage(editions)
            editions
        } catch (e: Exception) {
            // fallback to any disk cache
            val fallback = store.loadCachedCatalogueJson()?.let { parseCatalogueJson(it) }.orEmpty()
            if (fallback.isNotEmpty()) {
                _catalogue.value = fallback
                _languages.value = FawazQuranProvider.groupByLanguage(fallback)
                _error.value = "Using cached catalogue (network: ${e.message})"
                fallback
            } else {
                _error.value = e.message ?: "Failed to load translation catalogue"
                emptyList()
            }
        } finally {
            _loading.value = false
        }
    }

    private fun parseCatalogueJson(json: String): List<FawazEdition> {
        val root = JSONObject(json)
        val out = ArrayList<FawazEdition>(root.length())
        val keys = root.keys()
        while (keys.hasNext()) {
            val id = keys.next()
            val o = root.optJSONObject(id) ?: continue
            val linkMin = o.optString("linkmin").ifBlank {
                "https://cdn.jsdelivr.net/gh/fawazahmed0/quran-api@1/editions/$id.min.json"
            }
            out.add(
                FawazEdition(
                    id = id,
                    name = o.optString("name").orEmpty(),
                    author = o.optString("author").orEmpty(),
                    language = o.optString("language").orEmpty().ifBlank { "Unknown" },
                    direction = o.optString("direction").orEmpty().ifBlank { "ltr" },
                    source = o.optString("source").orEmpty(),
                    comments = o.optString("comments").orEmpty(),
                    linkMin = linkMin,
                    linkFull = o.optString("link").orEmpty()
                )
            )
        }
        return out.sortedWith(compareBy({ it.language.lowercase() }, { it.author.lowercase() }))
    }

    fun search(query: String): List<FawazEdition> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return _catalogue.value
        return _catalogue.value.filter {
            it.language.lowercase().contains(q) ||
                it.author.lowercase().contains(q) ||
                it.name.lowercase().contains(q) ||
                it.id.lowercase().contains(q) ||
                it.source.lowercase().contains(q) ||
                (q.contains("roman") && it.isLikelyRomanScript)
        }
    }

    fun trending(): List<FawazEdition> {
        val all = _catalogue.value
        if (all.isEmpty()) return emptyList()
        val byId = all.associateBy { it.id }
        val preferred = mutableListOf<FawazEdition>()
        // Match loosely: preferred stems against catalogue ids
        val stems = listOf(
            "eng_saheeh", "eng_mustafa", "eng_abdel", "eng_clear",
            "urd_fateh", "urd_abulaalamaududi_la", "urd_muhammadjunagar_la",
            "hin_suhelfarooqkhan_la", "hin_muhammadfarooqk_la",
            "ben_muhiuddin", "fra_muhammadhamid", "spa_", "tur_diyanet",
            "ind_indonesian", "rus_", "per_naser"
        )
        for (e in all) {
            if (preferred.size >= 12) break
            if (stems.any { e.id.lowercase().contains(it.lowercase().trimEnd('_')) }) {
                if (preferred.none { it.id == e.id }) preferred.add(e)
            }
        }
        // Always try to surface Roman Urdu / Roman Hindi when present
        all.filter { it.isLikelyRomanScript && (it.language.contains("Urdu", true) || it.language.contains("Hindi", true)) }
            .forEach { e ->
                if (preferred.none { it.id == e.id }) preferred.add(0, e)
            }
        return preferred.take(12).ifEmpty { all.filter { it.language in listOf("English", "Urdu", "Hindi") }.take(8) }
    }

    fun myLibraryEditions(): List<FawazEdition> {
        val ids = store.myLibraryIds().toSet()
        return _catalogue.value.filter { it.id in ids }
    }

    fun status(id: String): LibraryEditionStatus {
        val downloading = _downloadProgress.value.containsKey(id)
        val pct = _downloadProgress.value[id] ?: 0
        val state = when {
            downloading -> EditionDownloadState.DOWNLOADING
            store.isDownloaded(id) -> EditionDownloadState.DOWNLOADED
            else -> EditionDownloadState.NOT_DOWNLOADED
        }
        return LibraryEditionStatus(
            editionId = id,
            inMyLibrary = store.isInLibrary(id),
            downloadState = state,
            progressPercent = pct
        )
    }

    fun addToMyLibrary(id: String) = store.addToLibrary(id)
    fun removeFromMyLibrary(id: String) = store.removeFromLibrary(id)

    suspend fun downloadEdition(edition: FawazEdition): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _downloadProgress.value = _downloadProgress.value + (edition.id to 5)
            val url = edition.linkMin.ifBlank {
                "https://cdn.jsdelivr.net/gh/fawazahmed0/quran-api@1/editions/${edition.id}.min.json"
            }
            _downloadProgress.value = _downloadProgress.value + (edition.id to 25)
            val json = HttpJson.get(url, timeoutMs = 90_000)
            _downloadProgress.value = _downloadProgress.value + (edition.id to 70)
            // Basic verification: must parse verses
            val verses = FawazQuranProvider.parseEditionJson(json)
            if (verses.size < 100) {
                error("Edition payload incomplete (${verses.size} verses)")
            }
            store.writeDownloadedJson(edition.id, json)
            store.addToLibrary(edition.id)
            _downloadProgress.value = _downloadProgress.value - edition.id
            Result.success(Unit)
        } catch (e: Exception) {
            _downloadProgress.value = _downloadProgress.value - edition.id
            Result.failure(e)
        }
    }

    fun removeDownload(id: String) {
        store.removeDownload(id)
    }

    /**
     * Load verse map for an edition: offline file first, else network (without forcing save).
     */
    suspend fun verseMapFor(editionId: String): Map<Pair<Int, Int>, String> = withContext(Dispatchers.IO) {
        val local = store.readDownloadedJson(editionId)
        val json = local ?: run {
            val ed = _catalogue.value.find { it.id == editionId } ?: return@withContext emptyMap()
            val url = ed.linkMin.ifBlank {
                "https://cdn.jsdelivr.net/gh/fawazahmed0/quran-api@1/editions/$editionId.min.json"
            }
            HttpJson.get(url, timeoutMs = 90_000)
        }
        FawazQuranProvider.verseMap(FawazQuranProvider.parseEditionJson(json))
    }

    companion object {
        @Volatile private var instance: QuranLibraryRepository? = null
        fun get(context: Context): QuranLibraryRepository =
            instance ?: synchronized(this) {
                instance ?: QuranLibraryRepository(context.applicationContext).also { instance = it }
            }
    }
}

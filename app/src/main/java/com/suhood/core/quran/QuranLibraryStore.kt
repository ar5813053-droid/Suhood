package com.suhood.core.quran

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Local persistence for Quran Library:
 * - selected (My Translations) edition ids
 * - cached catalogue JSON
 * - downloaded edition files under filesDir/quran_editions/
 *
 * Does not auto-download the full catalogue's text bodies.
 */
class QuranLibraryStore(context: Context) {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("quran_library_v1", Context.MODE_PRIVATE)
    private val editionsDir = File(app.filesDir, "quran_editions").apply { mkdirs() }
    private val catalogueFile = File(app.cacheDir, "fawaz_editions.min.json")

    fun myLibraryIds(): List<String> {
        val raw = prefs.getString(KEY_MY, "") ?: ""
        return raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }.distinct()
    }

    fun setMyLibraryIds(ids: List<String>) {
        prefs.edit().putString(KEY_MY, ids.distinct().joinToString(",")).apply()
    }

    fun addToLibrary(id: String) {
        val cur = myLibraryIds().toMutableList()
        if (id !in cur) {
            cur.add(id)
            setMyLibraryIds(cur)
        }
    }

    fun removeFromLibrary(id: String) {
        setMyLibraryIds(myLibraryIds().filter { it != id })
    }

    fun isInLibrary(id: String): Boolean = id in myLibraryIds()

    /** Active single translation edition id for the Quran reader (e.g. en.sahih). */
    fun activeTranslationId(): String =
        prefs.getString(KEY_ACTIVE_TRANSLATION, "en.sahih") ?: "en.sahih"

    fun setActiveTranslationId(id: String) {
        prefs.edit().putString(KEY_ACTIVE_TRANSLATION, id).apply()
    }

    fun saveCatalogueJson(json: String) {
        runCatching { catalogueFile.writeText(json) }
        prefs.edit().putLong(KEY_CATALOGUE_AT, System.currentTimeMillis()).apply()
    }

    fun loadCachedCatalogueJson(): String? {
        if (!catalogueFile.exists() || catalogueFile.length() < 32) return null
        return runCatching { catalogueFile.readText() }.getOrNull()
    }

    fun catalogueAgeMs(): Long {
        val at = prefs.getLong(KEY_CATALOGUE_AT, 0L)
        if (at <= 0L) return Long.MAX_VALUE
        return System.currentTimeMillis() - at
    }

    fun editionFile(id: String): File =
        File(editionsDir, id.replace(Regex("[^a-zA-Z0-9._-]"), "_") + ".min.json")

    fun isDownloaded(id: String): Boolean {
        val f = editionFile(id)
        return f.exists() && f.length() > 64
    }

    fun readDownloadedJson(id: String): String? {
        val f = editionFile(id)
        if (!f.exists()) return null
        return runCatching { f.readText() }.getOrNull()
    }

    fun writeDownloadedJson(id: String, json: String) {
        editionFile(id).writeText(json)
    }

    fun removeDownload(id: String) {
        editionFile(id).delete()
    }

    fun downloadedIds(): List<String> =
        editionsDir.listFiles()?.mapNotNull { f ->
            if (f.isFile && f.name.endsWith(".min.json") && f.length() > 64)
                f.name.removeSuffix(".min.json")
            else null
        }.orEmpty()

    companion object {
        private const val KEY_MY = "my_library_ids"
        private const val KEY_CATALOGUE_AT = "catalogue_fetched_at"
        private const val KEY_ACTIVE_TRANSLATION = "active_translation_id"

        @Volatile private var instance: QuranLibraryStore? = null
        fun get(context: Context): QuranLibraryStore =
            instance ?: synchronized(this) {
                instance ?: QuranLibraryStore(context.applicationContext).also { instance = it }
            }
    }
}

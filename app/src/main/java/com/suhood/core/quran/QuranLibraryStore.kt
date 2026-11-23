package com.suhood.core.quran

import android.content.Context

/**
 * Lightweight persistence for the active Quran translation edition id.
 * (Named historically; no longer manages a downloadable translation library.)
 */
class QuranLibraryStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("quran_library_v1", Context.MODE_PRIVATE)

    fun activeTranslationId(): String =
        prefs.getString(KEY_ACTIVE_TRANSLATION, "en.sahih") ?: "en.sahih"

    fun setActiveTranslationId(id: String) {
        prefs.edit().putString(KEY_ACTIVE_TRANSLATION, id).apply()
    }

    companion object {
        private const val KEY_ACTIVE_TRANSLATION = "active_translation_id"

        @Volatile
        private var instance: QuranLibraryStore? = null

        fun get(context: Context): QuranLibraryStore =
            instance ?: synchronized(this) {
                instance ?: QuranLibraryStore(context.applicationContext).also { instance = it }
            }
    }
}

package com.suhood.feature.quest

import android.content.Context

/**
 * Editable 30-day challenge preferences. Local-only.
 */
object QuestPrefs {
    private const val PREFS = "suhood_quest_prefs_v2"

    data class State(
        val setupDone: Boolean = false,
        val goals: Set<String> = emptySet(),
        val quranLevel: String = "beginner",
        val challengeDifficulty: String = "normal",
        val salahConsistency: String = "mixed",
        val preferredMinutes: Int = 15
    )

    private fun prefs(ctx: Context) =
        ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isSetup(ctx: Context): Boolean = prefs(ctx).getBoolean("setup_done", false)

    fun load(ctx: Context): State {
        val p = prefs(ctx)
        return State(
            setupDone = p.getBoolean("setup_done", false),
            goals = p.getStringSet("goals", emptySet())?.toSet().orEmpty(),
            quranLevel = p.getString("quran_level", "beginner") ?: "beginner",
            challengeDifficulty = p.getString("difficulty", "normal") ?: "normal",
            salahConsistency = p.getString("salah_consistency", "mixed") ?: "mixed",
            preferredMinutes = p.getInt("preferred_minutes", 15)
        )
    }

    fun save(ctx: Context, state: State) {
        prefs(ctx).edit()
            .putBoolean("setup_done", true)
            .putStringSet("goals", state.goals)
            .putString("quran_level", state.quranLevel)
            .putString("difficulty", state.challengeDifficulty)
            .putString("salah_consistency", state.salahConsistency)
            .putInt("preferred_minutes", state.preferredMinutes)
            .commit()
    }
}

package com.suhood.feature.focus

import android.content.Context
import android.content.SharedPreferences
import java.util.Calendar

/**
 * Max 3 Focus challenges per calendar day. Completing a challenge grants a short pass
 * so the user can use the blocked app without immediate re-prompt.
 */
class FocusChallengeStore(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun challengesUsedToday(): Int {
        rollDayIfNeeded()
        return prefs.getInt(KEY_USED, 0)
    }

    fun challengesRemaining(): Int = (MAX_PER_DAY - challengesUsedToday()).coerceAtLeast(0)

    fun canOfferChallenge(): Boolean = challengesRemaining() > 0

    /** Call when user completes a mini-game successfully. */
    fun recordChallengeCompleted() {
        rollDayIfNeeded()
        val used = prefs.getInt(KEY_USED, 0) + 1
        prefs.edit()
            .putInt(KEY_USED, used.coerceAtMost(MAX_PER_DAY))
            .putLong(KEY_PASS_UNTIL, System.currentTimeMillis() + PASS_DURATION_MS)
            .commit()
    }

    fun hasActivePass(): Boolean {
        rollDayIfNeeded()
        return System.currentTimeMillis() < prefs.getLong(KEY_PASS_UNTIL, 0L)
    }

    fun clearPass() {
        prefs.edit().putLong(KEY_PASS_UNTIL, 0L).commit()
    }

    private fun rollDayIfNeeded() {
        val today = dayKey()
        val stored = prefs.getString(KEY_DAY, "")
        if (stored != today) {
            prefs.edit()
                .putString(KEY_DAY, today)
                .putInt(KEY_USED, 0)
                .putLong(KEY_PASS_UNTIL, 0L)
                .commit()
        }
    }

    private fun dayKey(): String {
        val c = Calendar.getInstance()
        return "%04d-%02d-%02d".format(
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH) + 1,
            c.get(Calendar.DAY_OF_MONTH)
        )
    }

    companion object {
        const val MAX_PER_DAY = 3
        const val PASS_DURATION_MS = 3 * 60 * 1000L // 3 minutes after completing challenge
        private const val PREFS = "focus_challenge_v1"
        private const val KEY_DAY = "day"
        private const val KEY_USED = "used"
        private const val KEY_PASS_UNTIL = "pass_until"

        @Volatile private var instance: FocusChallengeStore? = null
        fun get(context: Context): FocusChallengeStore =
            instance ?: synchronized(this) {
                instance ?: FocusChallengeStore(context.applicationContext).also { instance = it }
            }
    }
}

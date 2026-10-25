package com.suhood.feature.onboarding

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * Local-only onboarding answers + resume step. Not uploaded.
 */
class OnboardingProfileStore(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var step: Int
        get() = prefs.getInt(KEY_STEP, 0)
        set(v) { prefs.edit().putInt(KEY_STEP, v.coerceIn(0, 11)).commit() }

    var ageGroup: String
        get() = prefs.getString(KEY_AGE, "") ?: ""
        set(v) { prefs.edit().putString(KEY_AGE, v).apply() }

    var gender: String
        get() = prefs.getString(KEY_GENDER, "") ?: ""
        set(v) { prefs.edit().putString(KEY_GENDER, v).apply() }

    var dailyLife: String
        get() = prefs.getString(KEY_LIFE, "") ?: ""
        set(v) { prefs.edit().putString(KEY_LIFE, v).apply() }

    var language: String
        get() = prefs.getString(KEY_LANG, "English") ?: "English"
        set(v) { prefs.edit().putString(KEY_LANG, v).apply() }

    var goals: Set<String>
        get() = prefs.getStringSet(KEY_GOALS, emptySet()) ?: emptySet()
        set(v) { prefs.edit().putStringSet(KEY_GOALS, v).apply() }

    var phoneUsage: String
        get() = prefs.getString(KEY_PHONE, "") ?: ""
        set(v) { prefs.edit().putString(KEY_PHONE, v).apply() }

    var socialUsage: String
        get() = prefs.getString(KEY_SOCIAL, "") ?: ""
        set(v) { prefs.edit().putString(KEY_SOCIAL, v).apply() }

    var lateNight: String
        get() = prefs.getString(KEY_LATE, "") ?: ""
        set(v) { prefs.edit().putString(KEY_LATE, v).apply() }

    var checkFrequency: String
        get() = prefs.getString(KEY_CHECK, "") ?: ""
        set(v) { prefs.edit().putString(KEY_CHECK, v).apply() }

    var notifInterrupt: String
        get() = prefs.getString(KEY_NOTIF_INT, "") ?: ""
        set(v) { prefs.edit().putString(KEY_NOTIF_INT, v).apply() }

    var hardestPrayer: String
        get() = prefs.getString(KEY_HARD_PRAYER, "") ?: ""
        set(v) { prefs.edit().putString(KEY_HARD_PRAYER, v).apply() }

    var reminderStyle: String
        get() = prefs.getString(KEY_REMINDER, "Balanced") ?: "Balanced"
        set(v) { prefs.edit().putString(KEY_REMINDER, v).apply() }

    fun prayerStatus(name: String): String =
        prefs.getString("prayer_$name", "") ?: ""

    fun setPrayerStatus(name: String, status: String) {
        prefs.edit().putString("prayer_$name", status).apply()
    }

    var quranMinutes: Int
        get() = prefs.getInt(KEY_QURAN_MIN, 10)
        set(v) { prefs.edit().putInt(KEY_QURAN_MIN, v).apply() }

    var dhikrMinutes: Int
        get() = prefs.getInt(KEY_DHIKR_MIN, 5)
        set(v) { prefs.edit().putInt(KEY_DHIKR_MIN, v).apply() }

    var quranReminder: Boolean
        get() = prefs.getBoolean(KEY_QURAN_REM, true)
        set(v) { prefs.edit().putBoolean(KEY_QURAN_REM, v).apply() }

    companion object {
        private const val PREFS = "onboarding_profile_v1"
        private const val KEY_STEP = "step"
        private const val KEY_AGE = "age"
        private const val KEY_GENDER = "gender"
        private const val KEY_LIFE = "life"
        private const val KEY_LANG = "lang"
        private const val KEY_GOALS = "goals"
        private const val KEY_PHONE = "phone"
        private const val KEY_SOCIAL = "social"
        private const val KEY_LATE = "late"
        private const val KEY_CHECK = "check"
        private const val KEY_NOTIF_INT = "notif_int"
        private const val KEY_HARD_PRAYER = "hard_prayer"
        private const val KEY_REMINDER = "reminder"
        private const val KEY_QURAN_MIN = "quran_min"
        private const val KEY_DHIKR_MIN = "dhikr_min"
        private const val KEY_QURAN_REM = "quran_rem"

        @Volatile private var instance: OnboardingProfileStore? = null
        fun get(context: Context): OnboardingProfileStore =
            instance ?: synchronized(this) {
                instance ?: OnboardingProfileStore(context.applicationContext).also { instance = it }
            }
    }
}

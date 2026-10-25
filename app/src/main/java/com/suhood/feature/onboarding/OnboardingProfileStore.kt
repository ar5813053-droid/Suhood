package com.suhood.feature.onboarding

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * Local-only onboarding answers, resume step, and generated plan. Not uploaded.
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

    /** When distractions are strongest (maps from HTML disttime). */
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

    var consistency: String
        get() = prefs.getString(KEY_CONSISTENCY, "") ?: ""
        set(v) { prefs.edit().putString(KEY_CONSISTENCY, v).apply() }

    /** quran | dhikr | both | none */
    var practice: String
        get() = prefs.getString(KEY_PRACTICE, "both") ?: "both"
        set(v) { prefs.edit().putString(KEY_PRACTICE, v).apply() }

    var dailyMinutes: Int
        get() = prefs.getInt(KEY_DAILY_MIN, 10)
        set(v) { prefs.edit().putInt(KEY_DAILY_MIN, v.coerceIn(5, 60)).apply() }

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

    var planJson: String
        get() = prefs.getString(KEY_PLAN, "") ?: ""
        set(v) { prefs.edit().putString(KEY_PLAN, v).commit() }

    fun toAnswers(appLabels: List<String> = emptyList()): OnboardingAnswers = OnboardingAnswers(
        goals = goals,
        hardestPrayer = hardestPrayer,
        consistency = consistency.ifBlank { checkFrequency },
        reminderStyle = reminderStyle,
        phoneUsage = phoneUsage,
        disttime = socialUsage,
        selectedAppLabels = appLabels,
        practice = practice,
        dailyMinutes = dailyMinutes.takeIf { it > 0 } ?: quranMinutes.coerceAtLeast(5),
        lifestyle = dailyLife,
        language = language
    )

    fun savePlan(plan: PersonalizedPlan) {
        val o = JSONObject()
        o.put("mainFocus", plan.mainFocus)
        o.put("mainFocusReason", plan.mainFocusReason)
        o.put("dailyRoutine", JSONArray(plan.dailyRoutine))
        o.put("salahSupport", JSONArray(plan.salahSupport))
        o.put("distractionStrategy", JSONArray(plan.distractionStrategy))
        o.put("worshipRoutine", JSONArray(plan.worshipRoutine))
        o.put("weeklyProgression", JSONArray(plan.weeklyProgression))
        val actions = JSONArray()
        plan.firstDayActions.forEach { a ->
            actions.put(
                JSONObject()
                    .put("id", a.id)
                    .put("title", a.title)
                    .put("detail", a.detail)
                    .put("feature", a.feature)
                    .put("reason", a.reason)
            )
        }
        o.put("firstDayActions", actions)
        planJson = o.toString()
    }

    fun loadPlan(): PersonalizedPlan? {
        val raw = planJson
        if (raw.isBlank()) return null
        return runCatching {
            val o = JSONObject(raw)
            fun arr(key: String): List<String> {
                val a = o.optJSONArray(key) ?: return emptyList()
                return (0 until a.length()).map { a.getString(it) }
            }
            val actionsJson = o.optJSONArray("firstDayActions") ?: JSONArray()
            val actions = (0 until actionsJson.length()).map { i ->
                val a = actionsJson.getJSONObject(i)
                PlanAction(
                    id = a.optString("id"),
                    title = a.optString("title"),
                    detail = a.optString("detail"),
                    feature = a.optString("feature"),
                    reason = a.optString("reason")
                )
            }
            PersonalizedPlan(
                mainFocus = o.optString("mainFocus"),
                mainFocusReason = o.optString("mainFocusReason"),
                dailyRoutine = arr("dailyRoutine"),
                salahSupport = arr("salahSupport"),
                distractionStrategy = arr("distractionStrategy"),
                worshipRoutine = arr("worshipRoutine"),
                firstDayActions = actions,
                weeklyProgression = arr("weeklyProgression")
            )
        }.getOrNull()
    }

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
        private const val KEY_CONSISTENCY = "consistency"
        private const val KEY_PRACTICE = "practice"
        private const val KEY_DAILY_MIN = "daily_min"
        private const val KEY_QURAN_MIN = "quran_min"
        private const val KEY_DHIKR_MIN = "dhikr_min"
        private const val KEY_QURAN_REM = "quran_rem"
        private const val KEY_PLAN = "plan_json"

        @Volatile private var instance: OnboardingProfileStore? = null
        fun get(context: Context): OnboardingProfileStore =
            instance ?: synchronized(this) {
                instance ?: OnboardingProfileStore(context.applicationContext).also { instance = it }
            }
    }
}

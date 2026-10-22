package com.suhood.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.suhood.core.designsystem.theme.ThemeMode
import com.suhood.core.model.UserPreferences
import com.suhood.core.prayer.PrayerTimesCalculator
import com.suhood.core.prayer.PrayerTimesCache
import com.suhood.widgets.WidgetBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "suhood_prefs")

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val LANGUAGE = stringPreferencesKey("language")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_completed")
        val REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
        val LAT = doublePreferencesKey("latitude")
        val LNG = doublePreferencesKey("longitude")
        val PRAYER_METHOD = stringPreferencesKey("prayer_method")
        val LOCATION_LABEL = stringPreferencesKey("location_label")
        val QURAN_DAILY_MINUTES = stringPreferencesKey("quran_daily_minutes") // "5"|"10"|"20"
        val QURAN_REMINDER_HOUR = stringPreferencesKey("quran_reminder_hour")
        val QURAN_REMINDER_MINUTE = stringPreferencesKey("quran_reminder_minute")
        val QURAN_REMINDER_ENABLED = booleanPreferencesKey("quran_reminder_enabled")
        val QURAN_COMPANION_SETUP = booleanPreferencesKey("quran_companion_setup")
        val QURAN_TODAY_DATE = stringPreferencesKey("quran_today_date")
        val QURAN_TODAY_PAGES = stringPreferencesKey("quran_today_pages")
        val QURAN_LAST_PAGE = stringPreferencesKey("quran_last_page")
    }

    val preferences: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            themeMode = prefs[Keys.THEME] ?: "DARK",
            language = prefs[Keys.LANGUAGE] ?: "en",
            onboardingCompleted = prefs[Keys.ONBOARDING_DONE] ?: false,
            reducedMotion = prefs[Keys.REDUCED_MOTION] ?: false
        )
    }

    val themeMode: Flow<ThemeMode> = preferences.map {
        when (it.themeMode) {
            "LIGHT" -> ThemeMode.LIGHT
            "SYSTEM" -> ThemeMode.SYSTEM
            else -> ThemeMode.DARK
        }
    }

    val onboardingCompleted: Flow<Boolean> = preferences.map { it.onboardingCompleted }

    val quranDailyMinutes: Flow<Int> = context.dataStore.data.map {
        (it[Keys.QURAN_DAILY_MINUTES] ?: "10").toIntOrNull() ?: 10
    }
    val quranReminderHour: Flow<Int> = context.dataStore.data.map {
        (it[Keys.QURAN_REMINDER_HOUR] ?: "9").toIntOrNull() ?: 9
    }
    val quranReminderMinute: Flow<Int> = context.dataStore.data.map {
        (it[Keys.QURAN_REMINDER_MINUTE] ?: "0").toIntOrNull() ?: 0
    }
    val quranReminderEnabled: Flow<Boolean> = context.dataStore.data.map {
        it[Keys.QURAN_REMINDER_ENABLED] ?: false
    }
    val quranCompanionSetupDone: Flow<Boolean> = context.dataStore.data.map {
        it[Keys.QURAN_COMPANION_SETUP] ?: false
    }
    val quranTodayPages: Flow<Int> = context.dataStore.data.map {
        it[Keys.QURAN_TODAY_PAGES]?.toIntOrNull() ?: 0
    }
    val quranTodayDate: Flow<String> = context.dataStore.data.map {
        it[Keys.QURAN_TODAY_DATE] ?: ""
    }
    val quranLastPage: Flow<Int> = context.dataStore.data.map {
        it[Keys.QURAN_LAST_PAGE]?.toIntOrNull() ?: 1
    }

    /** Default: Makkah approx if user has not set location. */
    val latitude: Flow<Double> = context.dataStore.data.map { it[Keys.LAT] ?: 21.4225 }
    val longitude: Flow<Double> = context.dataStore.data.map { it[Keys.LNG] ?: 39.8262 }
    val locationLabel: Flow<String> = context.dataStore.data.map {
        it[Keys.LOCATION_LABEL] ?: "Makkah (default — change in Settings)"
    }
    val prayerMethod: Flow<PrayerTimesCalculator.Method> = context.dataStore.data.map {
        runCatching {
            PrayerTimesCalculator.Method.valueOf(it[Keys.PRAYER_METHOD] ?: "MWL")
        }.getOrDefault(PrayerTimesCalculator.Method.MWL)
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME] = mode.name }
    }

    suspend fun setLanguage(lang: String) {
        context.dataStore.edit { it[Keys.LANGUAGE] = lang }
    }

    suspend fun setOnboardingCompleted(done: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_DONE] = done }
    }

    suspend fun setReducedMotion(enabled: Boolean) {
        context.dataStore.edit { it[Keys.REDUCED_MOTION] = enabled }
    }

    suspend fun setLocation(lat: Double, lng: Double, label: String) {
        context.dataStore.edit {
            it[Keys.LAT] = lat
            it[Keys.LNG] = lng
            it[Keys.LOCATION_LABEL] = label
        }
        withContext(Dispatchers.IO) {
            val method = prayerMethod.first()
            val result = PrayerTimesCalculator.calculate(lat, lng, method)
            PrayerTimesCache.save(context, lat, lng, method, label, result)
            WidgetBridge.refreshAll(context)
        }
    }

    suspend fun setPrayerMethod(method: PrayerTimesCalculator.Method) {
        context.dataStore.edit { it[Keys.PRAYER_METHOD] = method.name }
        withContext(Dispatchers.IO) {
            val lat = latitude.first()
            val lng = longitude.first()
            val label = locationLabel.first()
            val result = PrayerTimesCalculator.calculate(lat, lng, method)
            PrayerTimesCache.save(context, lat, lng, method, label, result)
            WidgetBridge.refreshAll(context)
        }
    }

    suspend fun setQuranDailyMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.QURAN_DAILY_MINUTES] = minutes.toString() }
    }

    suspend fun setQuranReminder(hour: Int, minute: Int, enabled: Boolean) {
        context.dataStore.edit {
            it[Keys.QURAN_REMINDER_HOUR] = hour.coerceIn(0, 23).toString()
            it[Keys.QURAN_REMINDER_MINUTE] = minute.coerceIn(0, 59).toString()
            it[Keys.QURAN_REMINDER_ENABLED] = enabled
        }
    }

    suspend fun setQuranCompanionSetupDone(done: Boolean) {
        context.dataStore.edit { it[Keys.QURAN_COMPANION_SETUP] = done }
    }

    suspend fun setQuranLastPage(page: Int) {
        context.dataStore.edit { it[Keys.QURAN_LAST_PAGE] = page.coerceIn(1, 604).toString() }
    }

    /** Record pages read today; resets count when the calendar day changes. */
    suspend fun addQuranPagesRead(pages: Int) {
        if (pages <= 0) return
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            .format(java.util.Date())
        context.dataStore.edit { prefs ->
            val storedDate = prefs[Keys.QURAN_TODAY_DATE]
            val current = if (storedDate == today) {
                prefs[Keys.QURAN_TODAY_PAGES]?.toIntOrNull() ?: 0
            } else 0
            prefs[Keys.QURAN_TODAY_DATE] = today
            prefs[Keys.QURAN_TODAY_PAGES] = (current + pages).toString()
        }
    }

    suspend fun markQuranDayComplete(targetPages: Int) {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            .format(java.util.Date())
        context.dataStore.edit {
            it[Keys.QURAN_TODAY_DATE] = today
            it[Keys.QURAN_TODAY_PAGES] = targetPages.toString()
        }
    }

    suspend fun resetDemoData() {
        context.dataStore.edit { it.clear() }
    }
}

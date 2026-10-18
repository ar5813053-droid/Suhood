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
import kotlinx.coroutines.flow.Flow
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
    }

    suspend fun setPrayerMethod(method: PrayerTimesCalculator.Method) {
        context.dataStore.edit { it[Keys.PRAYER_METHOD] = method.name }
    }

    suspend fun resetDemoData() {
        context.dataStore.edit { it.clear() }
    }
}

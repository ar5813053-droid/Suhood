package com.suhood.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.suhood.core.designsystem.theme.ThemeMode
import com.suhood.core.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "suhood_prefs")

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val LANGUAGE = stringPreferencesKey("language")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_completed")
        val REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
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

    suspend fun resetDemoData() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }
}

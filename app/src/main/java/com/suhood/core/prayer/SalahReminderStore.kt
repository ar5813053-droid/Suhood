package com.suhood.core.prayer

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.salahStore by preferencesDataStore("salah_reminders")

data class SalahReminderPref(
    val name: String,
    val enabled: Boolean,
    /** Minutes before calculated prayer time (0 = at prayer time). */
    val offsetMinutes: Int
)

class SalahReminderStore(private val context: Context) {
    private fun enabledKey(name: String) = booleanPreferencesKey("enabled_$name")
    private fun offsetKey(name: String) = intPreferencesKey("offset_$name")

    val preferences: Flow<List<SalahReminderPref>> = context.salahStore.data.map { prefs ->
        listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha").map { name ->
            SalahReminderPref(
                name = name,
                enabled = prefs[enabledKey(name)] ?: true,
                offsetMinutes = prefs[offsetKey(name)] ?: 0
            )
        }
    }

    suspend fun setEnabled(name: String, enabled: Boolean) {
        context.salahStore.edit { it[enabledKey(name)] = enabled }
    }

    suspend fun setOffset(name: String, minutes: Int) {
        context.salahStore.edit { it[offsetKey(name)] = minutes.coerceIn(-60, 120) }
    }
}

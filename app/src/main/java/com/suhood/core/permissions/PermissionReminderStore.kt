package com.suhood.core.permissions

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.permissionReminderDs by preferencesDataStore("permission_reminders_v1")

data class PermissionReminderSettings(
    val enabled: Boolean = true,
    /** 1..4 local reminders per day while permissions remain missing. */
    val frequencyPerDay: Int = 3,
    val snoozedUntilEpochMs: Long = 0L,
    val bannerDismissedUntilEpochMs: Long = 0L
)

class PermissionReminderStore(private val context: Context) {
    private val KEY_ENABLED = booleanPreferencesKey("enabled")
    private val KEY_FREQ = intPreferencesKey("freq")
    private val KEY_SNOOZE = longPreferencesKey("snooze")
    private val KEY_BANNER = longPreferencesKey("banner_dismiss")

    val settings: Flow<PermissionReminderSettings> = context.permissionReminderDs.data.map { p ->
        PermissionReminderSettings(
            enabled = p[KEY_ENABLED] ?: true,
            frequencyPerDay = (p[KEY_FREQ] ?: 3).coerceIn(1, 4),
            snoozedUntilEpochMs = p[KEY_SNOOZE] ?: 0L,
            bannerDismissedUntilEpochMs = p[KEY_BANNER] ?: 0L
        )
    }

    suspend fun current(): PermissionReminderSettings = settings.first()

    suspend fun update(block: (PermissionReminderSettings) -> PermissionReminderSettings) {
        val next = block(current())
        context.permissionReminderDs.edit { p ->
            p[KEY_ENABLED] = next.enabled
            p[KEY_FREQ] = next.frequencyPerDay.coerceIn(1, 4)
            p[KEY_SNOOZE] = next.snoozedUntilEpochMs
            p[KEY_BANNER] = next.bannerDismissedUntilEpochMs
        }
    }

    suspend fun snoozeReminders(hours: Int = 6) {
        update { it.copy(snoozedUntilEpochMs = System.currentTimeMillis() + hours * 3_600_000L) }
    }

    suspend fun dismissBannerForToday() {
        // Hide until next local midnight-ish (12h)
        update { it.copy(bannerDismissedUntilEpochMs = System.currentTimeMillis() + 12 * 3_600_000L) }
    }
}

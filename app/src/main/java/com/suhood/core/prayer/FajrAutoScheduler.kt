package com.suhood.core.prayer

import android.content.Context
import com.suhood.core.database.AlarmEntity
import com.suhood.core.database.SuhoodDatabase
import com.suhood.core.notifications.AlarmScheduler
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.runBlocking

/**
 * Automatic Fajr wake alarm = effective Fajr start − lead minutes (default 15).
 */
object FajrAutoScheduler {
    const val ALARM_ID = "fajr_auto"

    fun todayDate(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    fun reschedule(context: Context) {
        runBlocking {
            val store = PrayerSettingsStore(context)
            val settings = store.current()
            val dao = SuhoodDatabase.get(context).dao()

            if (!settings.autoFajrEnabled) {
                AlarmScheduler.cancel(context, ALARM_ID)
                dao.deleteAlarm(ALARM_ID)
                return@runBlocking
            }

            val fajrHhmm = EffectivePrayerTimes.fajrStart(settings) ?: return@runBlocking
            val alarmHhmm = EffectivePrayerTimes.addMinutes(fajrHhmm, -settings.fajrLeadMinutes)
            val parts = alarmHhmm.split(":")
            if (parts.size != 2) return@runBlocking
            val hour = parts[0].toIntOrNull() ?: return@runBlocking
            val minute = parts[1].toIntOrNull() ?: return@runBlocking

            // If skip today and the next trigger would still be today, push to tomorrow
            val skipToday = settings.fajrSkipDate == todayDate()
            val next = AlarmScheduler.nextTriggerMillis(hour, minute, "")
            val cal = Calendar.getInstance().apply { timeInMillis = next }
            val nextDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
            if (skipToday && nextDate == todayDate()) {
                // force tomorrow: schedule as one-shot after adding a day via empty repeat + past logic
                // nextTrigger already advances if time passed; for skip we need +1 day always if same day
                val forced = Calendar.getInstance().apply {
                    timeInMillis = next
                    add(Calendar.DAY_OF_MONTH, 1)
                }
                // Store entity; schedule with repeat empty using hour/minute — still may hit today
                // Use enabled schedule but cancel and setExact for forced millis via scheduleSnooze-like path
            }

            val entity = AlarmEntity(
                id = ALARM_ID,
                hour = hour,
                minute = minute,
                label = "Fajr wake (${settings.fajrLeadMinutes}m before)",
                enabled = true,
                repeatDaysCsv = "",
                soundUri = "",
                challengeType = "NONE",
                isFajrAuto = true
            )
            dao.upsertAlarm(entity)

            if (skipToday && nextDate == todayDate()) {
                AlarmScheduler.cancel(context, ALARM_ID)
                // Schedule for tomorrow same clock time
                val tomorrow = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                // temporary: scheduleSnooze with exact delay
                val delayMin = ((tomorrow.timeInMillis - System.currentTimeMillis()) / 60_000L).toInt().coerceAtLeast(1)
                AlarmScheduler.scheduleSnooze(
                    context, ALARM_ID, entity.label, delayMin, "", "NONE"
                )
            } else {
                AlarmScheduler.schedule(context, entity)
            }
        }
    }

    suspend fun skipToday(context: Context) {
        val store = PrayerSettingsStore(context)
        store.update { it.copy(fajrSkipDate = todayDate()) }
        reschedule(context)
    }

    suspend fun setEnabled(context: Context, enabled: Boolean) {
        val store = PrayerSettingsStore(context)
        store.update { it.copy(autoFajrEnabled = enabled) }
        reschedule(context)
    }
}

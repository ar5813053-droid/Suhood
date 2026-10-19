package com.suhood.core.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.suhood.core.database.AlarmEntity
import com.suhood.feature.alarms.AlarmRingActivity
import java.util.Calendar

object AlarmScheduler {

    fun schedule(context: Context, alarm: AlarmEntity) {
        if (!alarm.enabled) {
            cancel(context, alarm.id)
            return
        }
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pendingIntent(context, alarm.id, alarm.label, alarm.soundId)
        val triggerAt = nextTriggerMillis(alarm.hour, alarm.minute, alarm.repeatDaysCsv)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            }
        } catch (_: SecurityException) {
            am.set(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    fun cancel(context: Context, alarmId: String) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pendingIntent(context, alarmId, ""))
    }

    fun scheduleSnooze(
        context: Context,
        alarmId: String,
        label: String,
        minutes: Int,
        soundUri: String = ""
    ) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pendingIntent(context, alarmId, label, soundUri)
        val triggerAt = System.currentTimeMillis() + minutes.coerceAtLeast(1) * 60_000L
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            }
        } catch (_: SecurityException) {
            am.set(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    /**
     * Next wall-clock trigger for [hour]/[minute].
     * [repeatDaysCsv] is optional CSV of Calendar.DAY_OF_WEEK values (1=Sunday … 7=Saturday).
     * Empty string = one-shot next occurrence of that clock time.
     */
    fun nextTriggerMillis(hour: Int, minute: Int, repeatDaysCsv: String = ""): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val days = repeatDaysCsv.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 1..7 }
            .toSet()

        if (days.isEmpty()) {
            if (!target.after(now)) {
                target.add(Calendar.DAY_OF_MONTH, 1)
            }
            return target.timeInMillis
        }

        if (!target.after(now)) {
            target.add(Calendar.DAY_OF_MONTH, 1)
        }
        repeat(8) {
            if (target.get(Calendar.DAY_OF_WEEK) in days) {
                return target.timeInMillis
            }
            target.add(Calendar.DAY_OF_MONTH, 1)
        }
        return target.timeInMillis
    }

    private fun pendingIntent(
        context: Context,
        alarmId: String,
        label: String,
        soundUri: String = ""
    ): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_LABEL, label)
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmRingActivity.EXTRA_SOUND_URI, soundUri)
        }
        return PendingIntent.getBroadcast(
            context,
            alarmId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

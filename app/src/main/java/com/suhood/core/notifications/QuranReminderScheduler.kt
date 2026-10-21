package com.suhood.core.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.runBlocking
import java.util.Calendar

object QuranReminderScheduler {
    private const val REQ = 71010

    fun schedule(context: Context, hour: Int, minute: Int, enabled: Boolean) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pending(context)
        am.cancel(pi)
        if (!enabled) return
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(Calendar.getInstance())) add(Calendar.DAY_OF_MONTH, 1)
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
            }
        } catch (_: SecurityException) {
            am.set(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
        }
    }

    fun cancel(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pending(context))
    }

    fun rescheduleFromPrefs(context: Context) {
        // Lightweight read without full DI — mirrors PreferencesRepository keys
        runCatching {
            val prefs = context.getSharedPreferences("suhood_quran_reminder_cache", Context.MODE_PRIVATE)
            // Prefer cache written by app when settings change
            val enabled = prefs.getBoolean("enabled", false)
            val hour = prefs.getInt("hour", 9)
            val minute = prefs.getInt("minute", 0)
            schedule(context, hour, minute, enabled)
        }
    }

    fun cachePrefs(context: Context, hour: Int, minute: Int, enabled: Boolean) {
        context.getSharedPreferences("suhood_quran_reminder_cache", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("enabled", enabled)
            .putInt("hour", hour)
            .putInt("minute", minute)
            .apply()
        schedule(context, hour, minute, enabled)
    }

    private fun pending(context: Context): PendingIntent {
        val intent = Intent(context, QuranReminderReceiver::class.java).setAction(QuranReminderReceiver.ACTION)
        return PendingIntent.getBroadcast(
            context, REQ, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

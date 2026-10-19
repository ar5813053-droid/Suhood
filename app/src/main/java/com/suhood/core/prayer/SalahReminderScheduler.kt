package com.suhood.core.prayer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.suhood.app.MainActivity
import com.suhood.app.SuhoodApplication
import java.util.Calendar

class SalahReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val name = intent.getStringExtra(EXTRA_PRAYER) ?: "Salah"
        val open = PendingIntent.getActivity(
            context, name.hashCode(),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(context, SuhoodApplication.CHANNEL_PRAYER)
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setContentTitle("$name reminder")
            .setContentText("Time for $name (calculated schedule)")
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        runCatching {
            NotificationManagerCompat.from(context).notify(name.hashCode(), n)
        }
    }

    companion object {
        const val EXTRA_PRAYER = "prayer"
    }
}

object SalahReminderScheduler {
    fun scheduleFromTimes(
        context: Context,
        times: List<Pair<String, String>>, // name to HH:mm
        prefs: List<SalahReminderPref>
    ) {
        val enabled = prefs.associateBy { it.name }
        times.forEach { (name, hhmm) ->
            val pref = enabled[name]
            if (pref == null || !pref.enabled) {
                cancel(context, name)
                return@forEach
            }
            val parts = hhmm.split(":")
            if (parts.size != 2) return@forEach
            val hour = parts[0].toIntOrNull() ?: return@forEach
            val minute = parts[1].toIntOrNull() ?: return@forEach
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                add(Calendar.MINUTE, -pref.offsetMinutes)
                if (before(Calendar.getInstance())) add(Calendar.DAY_OF_MONTH, 1)
            }
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pi = pending(context, name)
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
    }


    fun rescheduleAll(context: Context) {
        val defaults = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha").map {
            SalahReminderPref(it, enabled = true, offsetMinutes = 0)
        }
        val calc = PrayerTimesCalculator.calculate(21.4225, 39.8262)
        scheduleFromTimes(context, calc.times.map { it.name to it.time }, defaults)
    }

    fun cancel(context: Context, name: String) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pending(context, name))
    }

    private fun pending(context: Context, name: String): PendingIntent {
        val i = Intent(context, SalahReminderReceiver::class.java).putExtra(
            SalahReminderReceiver.EXTRA_PRAYER, name
        )
        return PendingIntent.getBroadcast(
            context, name.hashCode(), i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

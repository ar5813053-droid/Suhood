package com.suhood.core.prayer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.suhood.app.MainActivity
import com.suhood.app.SuhoodApplication
import com.suhood.feature.focus.FocusSessionController
import com.suhood.feature.focus.SalahLockActivity
import com.suhood.feature.focus.SalahLockStore
import java.util.Calendar
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class SalahReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val name = intent.getStringExtra(EXTRA_PRAYER) ?: "Salah"
        val app = context.applicationContext

        // Notification (separate from blocking)
        val open = PendingIntent.getActivity(
            context, name.hashCode(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("open", "salah")
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(context, SuhoodApplication.CHANNEL_PRAYER)
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setContentTitle("$name time")
            .setContentText("It's Salah time. Selected apps are limited for 10 minutes.")
            .setContentIntent(open)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .build()
        runCatching {
            NotificationManagerCompat.from(context).notify(name.hashCode() and 0x7FFF, n)
        }

        // Activate 10-minute Salah lock for user's selected distracting apps
        runCatching {
            val packages = FocusSessionController.get(app).getBlockedApps()
            if (packages.isNotEmpty()) {
                SalahLockStore.get(app).activate(
                    prayerName = name,
                    packages = packages,
                    nowWallMs = System.currentTimeMillis(),
                    elapsedRealtimeMs = SystemClock.elapsedRealtime()
                )
                // Surface the lock screen immediately if possible
                context.startActivity(
                    Intent(context, SalahLockActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        putExtra(SalahLockActivity.EXTRA_PRAYER, name)
                    }
                )
            }
        }

        // Reschedule tomorrow's occurrence (does not extend current lock)
        runCatching { SalahReminderScheduler.rescheduleAll(app) }
    }

    companion object {
        const val EXTRA_PRAYER = "prayer"
    }
}

object SalahReminderScheduler {
    fun scheduleFromTimes(
        context: Context,
        times: List<Pair<String, String>>,
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
                set(Calendar.MILLISECOND, 0)
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
        runBlocking {
            val settings = PrayerSettingsStore(context).current()
            val reminderPrefs = SalahReminderStore(context).preferences.first()
            val times = EffectivePrayerTimes.compute(settings).map { it.name to it.startHhmm }
            scheduleFromTimes(context, times, reminderPrefs)
        }
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

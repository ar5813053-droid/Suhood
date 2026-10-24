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
import com.suhood.app.SuhoodApplication
import com.suhood.feature.focus.FocusSessionController
import com.suhood.feature.focus.SalahLockActivity
import com.suhood.feature.focus.SalahLockMonitorService
import com.suhood.feature.focus.SalahLockStore
import java.util.Calendar
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class SalahReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val name = intent.getStringExtra(EXTRA_PRAYER) ?: "Salah"
        val app = context.applicationContext

        // 1) Activate persistent 10-minute lock for selected apps
        val packages = FocusSessionController.get(app).getBlockedApps()
            .filter { it != app.packageName && it.isNotBlank() }
            .toSet()
        if (packages.isNotEmpty()) {
            val lockStore = SalahLockStore.get(app)
            lockStore.activate(
                prayerName = name,
                packages = packages,
                nowWallMs = System.currentTimeMillis(),
                elapsedRealtimeMs = SystemClock.elapsedRealtime()
            )
            val snap = lockStore.read()
            // Watchdog alarms survive swipe-from-recents / process death
            com.suhood.feature.focus.SalahLockWatchdog.schedule(app, snap.expiryWallMs)
            SalahLockMonitorService.start(app)
            runCatching {
                com.suhood.feature.focus.FocusAccessibilityService.onSalahLockActivated(app, name)
            }
        }

        // 2) Full-screen intent notification → SalahLockActivity (works when phone is locked)
        //    Uses CHANNEL_SALAH_LOCK with custom Adhan sound + vibration.
        val lockIntent = Intent(app, SalahLockActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(SalahLockActivity.EXTRA_PRAYER, name)
        }
        val fullScreen = PendingIntent.getActivity(
            app,
            name.hashCode() xor 0x51A1,
            lockIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val content = if (packages.isEmpty()) {
            "It's time for $name. Select apps in Focus → Apps to enable locking."
        } else {
            "It's time for $name. Selected apps are locked for 10 minutes."
        }
        val notification = NotificationCompat.Builder(app, SuhoodApplication.CHANNEL_SALAH_LOCK)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle("$name — It's Salah Time")
            .setContentText(content)
            .setContentIntent(fullScreen)
            .setFullScreenIntent(fullScreen, true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setDefaults(0) // sound/vibration come from channel
            .build()
        runCatching {
            NotificationManagerCompat.from(app).notify(NOTIF_BASE + name.hashCode() and 0x0FFF, notification)
        }

        // Best-effort activity start (may be blocked in background on Android 10+)
        if (packages.isNotEmpty()) {
            runCatching { app.startActivity(lockIntent) }
        }

        // 3) Reschedule next day (does not extend current lock)
        runCatching { SalahReminderScheduler.rescheduleAll(app) }
    }

    companion object {
        const val EXTRA_PRAYER = "prayer"
        private const val NOTIF_BASE = 72000
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

package com.suhood.core.permissions

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.suhood.app.SuhoodApplication
import kotlinx.coroutines.runBlocking
import java.util.Calendar

/**
 * Local, inexact permission-setup reminders (1–4 per day).
 * Re-checks permissions before notifying; cancels when complete or disabled.
 */
object PermissionReminderScheduler {
    private const val REQ_BASE = 7100
    private const val ACTION = "com.suhood.PERMISSION_REMINDER"

    fun reschedule(context: Context) {
        val app = context.applicationContext
        cancelAll(app)
        val store = PermissionReminderStore(app)
        val settings = runBlocking { store.current() }
        if (!settings.enabled) return
        if (settings.snoozedUntilEpochMs > System.currentTimeMillis()) return
        val snap = PermissionStatusManager.snapshot(app)
        if (snap.allApplicableComplete) return

        val am = app.getSystemService(AlarmManager::class.java) ?: return
        val hours = defaultSlots(settings.frequencyPerDay)
        val now = Calendar.getInstance()
        hours.forEachIndexed { index, hour ->
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, 15)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
            }
            val pi = pending(app, index)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
            } else {
                @Suppress("DEPRECATION")
                am.set(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
            }
        }
    }

    fun cancelAll(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        for (i in 0 until 4) {
            am.cancel(pending(context.applicationContext, i))
        }
    }

    private fun defaultSlots(freq: Int): List<Int> = when (freq.coerceIn(1, 4)) {
        1 -> listOf(11)
        2 -> listOf(10, 18)
        3 -> listOf(9, 14, 20)
        else -> listOf(9, 12, 16, 20)
    }

    private fun pending(context: Context, index: Int): PendingIntent {
        val intent = Intent(context, PermissionReminderReceiver::class.java).setAction(ACTION)
            .putExtra("idx", index)
        return PendingIntent.getBroadcast(
            context,
            REQ_BASE + index,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

class PermissionReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val app = context.applicationContext
        val store = PermissionReminderStore(app)
        val settings = runBlocking { store.current() }
        if (!settings.enabled) return
        if (settings.snoozedUntilEpochMs > System.currentTimeMillis()) return
        val snap = PermissionStatusManager.snapshot(app)
        if (snap.allApplicableComplete) {
            PermissionReminderScheduler.cancelAll(app)
            return
        }
        if (!NotificationManagerCompat.from(app).areNotificationsEnabled()) {
            // Cannot show; still try to keep schedule for later
            PermissionReminderScheduler.reschedule(app)
            return
        }
        val count = snap.missingCount
        val title = "Finish setting up Suhood"
        val body = when {
            count == 1 -> "Suhood Focus needs one more permission when you're ready."
            count > 1 -> "$count permissions remaining — enable them when you're ready for full Focus protection."
            else -> "A few Suhood features still need permissions."
        }
        val nm = NotificationManagerCompat.from(app)
        val notif = NotificationCompat.Builder(app, SuhoodApplication.CHANNEL_FOCUS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        runCatching { nm.notify(7201, notif) }
        // Schedule next day slots
        PermissionReminderScheduler.reschedule(app)
    }
}

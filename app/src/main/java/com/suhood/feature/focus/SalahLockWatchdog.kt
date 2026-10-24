package com.suhood.feature.focus

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.suhood.app.SuhoodApplication

/**
 * Survives process death and Recents swipe.
 * Frequent exact alarms re-check Usage Access and fire a full-screen lock intent
 * so Instagram cannot stay open even if Accessibility was briefly killed.
 */
object SalahLockWatchdog {
    /** ~20s — tight enough that swipe-bypass lasts only seconds */
    private const val INTERVAL_MS = 20_000L
    private const val MAX_TICKS = 32 // ~10–11 min
    private const val REQ_BASE = 73000
    private const val NOTIF_ID = 73111

    fun schedule(context: Context, expiryWallMs: Long) {
        val app = context.applicationContext
        val am = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        cancelAll(app)
        val now = System.currentTimeMillis()
        var t = now + 3_000L
        var i = 0
        while (t < expiryWallMs + 3_000L && i < MAX_TICKS) {
            val pi = pending(app, i)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pi)
                } else {
                    am.setExact(AlarmManager.RTC_WAKEUP, t, pi)
                }
            } catch (_: SecurityException) {
                am.set(AlarmManager.RTC_WAKEUP, t, pi)
            }
            t += INTERVAL_MS
            i++
        }
    }

    fun cancelAll(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for (i in 0 until MAX_TICKS) {
            am.cancel(pending(context, i))
        }
        runCatching {
            NotificationManagerCompat.from(context).cancel(NOTIF_ID)
        }
    }

    private fun pending(context: Context, index: Int): PendingIntent {
        val intent = Intent(context, SalahLockWatchdogReceiver::class.java)
            .setAction(ACTION_TICK)
            .putExtra(EXTRA_INDEX, index)
        return PendingIntent.getBroadcast(
            context,
            REQ_BASE + index,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    const val ACTION_TICK = "com.suhood.action.SALAH_LOCK_WATCHDOG"
    const val EXTRA_INDEX = "idx"
}

class SalahLockWatchdogReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val app = context.applicationContext
        val store = SalahLockStore.get(app)
        val now = System.currentTimeMillis()
        val snap = store.reconcileNow(now)
        if (!SalahLockLogic.isActive(snap, now)) {
            SalahLockWatchdog.cancelAll(app)
            SalahLockMonitorService.stop(app)
            return
        }

        // Keep FGS alive (stopWithTask=false)
        SalahLockMonitorService.start(app)

        // Accessibility overlay path (if service process still alive)
        FocusAccessibilityService.onSalahLockActivated(
            app,
            snap.prayerName.ifBlank { "Salah" }
        )

        // Usage Access: is a blocked app in front?
        val blockedFg = AppBlockHelper.isBlockedAppForeground(app, snap.blockedPackages)

        // Always re-arm a full-screen / high-priority lock notification while lock is active.
        // When user is inside Instagram after killing Suhood, this is what brings the lock back.
        val lockIntent = Intent(app, SalahLockActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            putExtra(SalahLockActivity.EXTRA_PRAYER, snap.prayerName)
            if (blockedFg != null) putExtra(SalahLockActivity.EXTRA_PACKAGE, blockedFg)
        }
        val fullScreen = PendingIntent.getActivity(
            app,
            NOTIF_REQ,
            lockIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val remaining = SalahLockLogic.formatMmSs(
            SalahLockLogic.remainingMs(snap, now)
        )
        val text = if (blockedFg != null) {
            "Blocked app open — lock $remaining left"
        } else {
            "Salah lock active — $remaining left"
        }
        val notification = NotificationCompat.Builder(app, SuhoodApplication.CHANNEL_SALAH_LOCK)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle("It's Salah Time")
            .setContentText(text)
            .setContentIntent(fullScreen)
            .setFullScreenIntent(fullScreen, blockedFg != null) // full-screen when bypass attempt
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setOnlyAlertOnce(true) // don't re-blast Adhan every 20s
            .setSilent(blockedFg == null)
            .build()
        runCatching {
            NotificationManagerCompat.from(app).notify(NOTIF_ID, notification)
        }

        // Direct activity start (works when system allows background activity after alarm)
        if (blockedFg != null) {
            runCatching { app.startActivity(lockIntent) }
        }
    }

    companion object {
        private const val NOTIF_ID = 73111
        private const val NOTIF_REQ = 73112
    }
}

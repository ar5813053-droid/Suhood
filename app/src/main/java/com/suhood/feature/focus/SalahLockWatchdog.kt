package com.suhood.feature.focus

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.suhood.app.SuhoodApplication

/**
 * Survives process death / Recents swipe.
 * Every 5s: if a blocked app is in the foreground, pull user out and show lock UI.
 * Does NOT interrupt non-blocked apps or spam the Salah screen while idle.
 */
object SalahLockWatchdog {
    private const val INTERVAL_MS = 5_000L
    private const val MAX_TICKS = 130 // ~10–11 min at 5s
    private const val REQ_BASE = 73000
    private const val NOTIF_ID = 73111

    fun schedule(context: Context, expiryWallMs: Long) {
        val app = context.applicationContext
        val am = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        cancelAll(app)
        val now = System.currentTimeMillis()
        var t = now + 2_000L
        var i = 0
        while (t < expiryWallMs + 2_000L && i < MAX_TICKS) {
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
        runCatching { NotificationManagerCompat.from(context).cancel(NOTIF_ID) }
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

        SalahLockMonitorService.start(app)

        // Only act when a *blocked* app is in the foreground
        val blockedFg = AppBlockHelper.isBlockedAppForeground(app, snap.blockedPackages)

        if (blockedFg != null) {
            FocusAccessibilityService.onSalahLockActivated(
                app,
                snap.prayerName.ifBlank { "Salah" }
            )
            val lockIntent = Intent(app, SalahLockActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                putExtra(SalahLockActivity.EXTRA_PRAYER, snap.prayerName)
                putExtra(SalahLockActivity.EXTRA_PACKAGE, blockedFg)
            }
            val fullScreen = PendingIntent.getActivity(
                app, NOTIF_REQ, lockIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val remaining = SalahLockLogic.formatMmSs(SalahLockLogic.remainingMs(snap, now))
            val notification = NotificationCompat.Builder(app, SuhoodApplication.CHANNEL_SALAH_LOCK)
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setContentTitle("It's Salah Time")
                .setContentText("Blocked app open — $remaining left")
                .setContentIntent(fullScreen)
                .setFullScreenIntent(fullScreen, true)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .build()
            runCatching { NotificationManagerCompat.from(app).notify(NOTIF_ID, notification) }
            runCatching { app.startActivity(lockIntent) }
        } else {
            // Quiet ongoing status only — do NOT open Salah screen or HOME
            val remaining = SalahLockLogic.formatMmSs(SalahLockLogic.remainingMs(snap, now))
            val open = PendingIntent.getActivity(
                app, NOTIF_REQ,
                Intent(app, SalahLockActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra(SalahLockActivity.EXTRA_PRAYER, snap.prayerName)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val quiet = NotificationCompat.Builder(app, SuhoodApplication.CHANNEL_FOCUS)
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setContentTitle("Salah lock active")
                .setContentText("$remaining remaining")
                .setContentIntent(open)
                .setOngoing(true)
                .setSilent(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
            runCatching { NotificationManagerCompat.from(app).notify(NOTIF_ID, quiet) }
        }
    }

    companion object {
        private const val NOTIF_ID = 73111
        private const val NOTIF_REQ = 73112
    }
}

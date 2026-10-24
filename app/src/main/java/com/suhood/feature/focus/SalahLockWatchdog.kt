package com.suhood.feature.focus

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock

/**
 * Survives process death / swipe-from-recents:
 * schedules exact alarms across the 10-minute Salah lock window so enforcement
 * resumes even if Accessibility briefly dies with the task.
 */
object SalahLockWatchdog {
    private const val INTERVAL_MS = 45_000L
    private const val MAX_TICKS = 15 // ~11 minutes coverage
    private const val REQ_BASE = 73000

    fun schedule(context: Context, expiryWallMs: Long) {
        val app = context.applicationContext
        val am = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        cancelAll(app)
        val now = System.currentTimeMillis()
        var t = now + 5_000L
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
        // Restart monitor FGS (survives better with stopWithTask=false)
        SalahLockMonitorService.start(app)
        // Ask Accessibility to overlay + HOME if a blocked app is up
        FocusAccessibilityService.onSalahLockActivated(
            app,
            snap.prayerName.ifBlank { "Salah" }
        )
        // Usage-stats path: if blocked package is foreground, bring lock activity
        val blockedFg = AppBlockHelper.isBlockedAppForeground(app, snap.blockedPackages)
        if (blockedFg != null) {
            runCatching {
                app.startActivity(
                    Intent(app, SalahLockActivity::class.java).apply {
                        addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                        )
                        putExtra(SalahLockActivity.EXTRA_PACKAGE, blockedFg)
                        putExtra(SalahLockActivity.EXTRA_PRAYER, snap.prayerName)
                    }
                )
            }
        }
    }
}

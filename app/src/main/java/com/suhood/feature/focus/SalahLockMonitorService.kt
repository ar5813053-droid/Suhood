package com.suhood.feature.focus

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.suhood.app.SuhoodApplication

/**
 * While a Salah lock is active, periodically checks the foreground package via Usage Access
 * and opens [SalahLockActivity] if a selected app is in front.
 * Complements AccessibilityService (which can be delayed or restricted on some OEMs).
 */
class SalahLockMonitorService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var running = false

    private val tick = object : Runnable {
        override fun run() {
            if (!running) return
            val store = SalahLockStore.get(applicationContext)
            val now = System.currentTimeMillis()
            val snap = store.reconcileNow(now)
            if (!SalahLockLogic.isActive(snap, now)) {
                stopSelf()
                return
            }
            // Usage-stats fallback when Accessibility is slow/missing
            val blockedFg = AppBlockHelper.isBlockedAppForeground(
                applicationContext,
                snap.blockedPackages
            )
            if (blockedFg != null) {
                // Prefer Accessibility HOME + lock so Instagram is actually left
                FocusAccessibilityService.onSalahLockActivated(
                    applicationContext,
                    snap.prayerName.ifBlank { "Salah" }
                )
                launchLock(blockedFg, snap.prayerName)
            }
            handler.postDelayed(this, POLL_MS)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val store = SalahLockStore.get(applicationContext)
        val snap = store.reconcileNow()
        if (!SalahLockLogic.isActive(snap, System.currentTimeMillis())) {
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(NOTIF_ID, buildOngoing(snap.prayerName.ifBlank { "Salah" }))
        if (!running) {
            running = true
            handler.post(tick)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        running = false
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun launchLock(pkg: String, prayer: String) {
        startActivity(
            Intent(this, SalahLockActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                )
                putExtra(SalahLockActivity.EXTRA_PACKAGE, pkg)
                putExtra(SalahLockActivity.EXTRA_PRAYER, prayer)
            }
        )
    }

    private fun buildOngoing(prayer: String): Notification {
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, SalahLockActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, SuhoodApplication.CHANNEL_SALAH_LOCK)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle("Salah lock active")
            .setContentText("$prayer — selected apps limited for 10 minutes")
            .setContentIntent(open)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val NOTIF_ID = 71001
        private const val POLL_MS = 1500L

        fun start(context: Context) {
            val i = Intent(context, SalahLockMonitorService::class.java)
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(i)
                } else {
                    context.startService(i)
                }
            } catch (_: Exception) {
                // OEM may block FGS from background; Accessibility remains primary path
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, SalahLockMonitorService::class.java))
        }
    }
}

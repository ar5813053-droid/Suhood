package com.suhood.feature.focus

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.suhood.app.SuhoodApplication

/**
 * Foreground service while a Salah lock is active.
 *
 * - Persists nothing itself; [SalahLockStore] wall-clock expiry is source of truth.
 * - Shows an ongoing notification with a live MM:SS countdown derived from expiry.
 * - Polls Usage Access as a fallback when Accessibility is slow (OEM).
 * - On restart, recovers remaining time from saved expiry (does not reset to 10:00).
 */
class SalahLockMonitorService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var running = false
    private var lastNotifText: String = ""
    private val unlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val now = System.currentTimeMillis()
            val snap = SalahLockStore.get(applicationContext).reconcileNow(now)
            if (!SalahLockLogic.isActive(snap, now)) return
            val blockedFg = AppBlockHelper.isBlockedAppForeground(
                applicationContext, snap.blockedPackages
            )
            if (blockedFg != null) {
                SalahLockEnforcer.enforce(this@SalahLockMonitorService, blockedFg, snap.prayerName)
            } else {
                // Force re-eval of Accessibility path
                FocusAccessibilityService.onSalahLockActivated(applicationContext, snap.prayerName)
            }
        }
    }

    private val tick = object : Runnable {
        override fun run() {
            if (!running) return
            val store = SalahLockStore.get(applicationContext)
            val now = System.currentTimeMillis()
            val snap = store.reconcileNow(now)
            if (!SalahLockLogic.isActive(snap, now)) {
                NotificationManagerCompat.from(applicationContext).cancel(NOTIF_ID)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return
            }
            // Refresh countdown notification from wall-clock expiry (no drift)
            updateCountdownNotification(snap, now)

            val blockedFg = AppBlockHelper.isBlockedAppForeground(
                applicationContext,
                snap.blockedPackages
            )
            if (blockedFg != null) {
                // Launch lock screen + HOME even if Accessibility is slow/off
                SalahLockEnforcer.enforce(
                    applicationContext,
                    blockedFg,
                    snap.prayerName.ifBlank { "Salah" }
                )
            }
            handler.postDelayed(this, POLL_MS)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val store = SalahLockStore.get(applicationContext)
        val now = System.currentTimeMillis()
        val snap = store.reconcileNow(now)
        if (!SalahLockLogic.isActive(snap, now)) {
            stopSelf()
            return START_NOT_STICKY
        }
        val notif = buildCountdownNotification(snap, now)
        startForeground(NOTIF_ID, notif)
        runCatching {
            val f = IntentFilter().apply {
                addAction(Intent.ACTION_USER_PRESENT)
                addAction(Intent.ACTION_SCREEN_ON)
            }
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                registerReceiver(unlockReceiver, f, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                registerReceiver(unlockReceiver, f)
            }
        }
        lastNotifText = notif.extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        if (!running) {
            running = true
            handler.post(tick)
        } else {
            // Already running — still try an immediate FG check
            handler.post(tick)
        }
        // Immediate kick if user is already inside a blocked app
        val blockedFg = AppBlockHelper.isBlockedAppForeground(applicationContext, snap.blockedPackages)
        if (blockedFg != null) {
            SalahLockEnforcer.enforce(
                this, // service context — allowed to start activity while FGS
                blockedFg,
                snap.prayerName.ifBlank { "Salah" }
            )
        }
        return START_STICKY
    }

    override fun onDestroy() {
        running = false
        handler.removeCallbacksAndMessages(null)
        runCatching { unregisterReceiver(unlockReceiver) }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun updateCountdownNotification(snap: SalahLockSnapshot, now: Long) {
        val remaining = SalahLockLogic.remainingMs(snap, now)
        val mmss = SalahLockLogic.formatMmSs(remaining)
        val text = "Return to Allah • $mmss remaining"
        // Avoid hammering NotificationManager when text unchanged
        if (text == lastNotifText) return
        lastNotifText = text
        val notif = buildCountdownNotification(snap, now)
        runCatching {
            NotificationManagerCompat.from(this).notify(NOTIF_ID, notif)
        }
    }

    private fun buildCountdownNotification(snap: SalahLockSnapshot, now: Long): Notification {
        val remaining = SalahLockLogic.remainingMs(snap, now)
        val mmss = SalahLockLogic.formatMmSs(remaining)
        val prayer = snap.prayerName.ifBlank { "Salah" }
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, SalahLockActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(SalahLockActivity.EXTRA_PRAYER, prayer)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = NotificationCompat.Builder(this, SuhoodApplication.CHANNEL_SALAH_LOCK)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle("Salah Time")
            .setContentText("Return to Allah • $mmss remaining")
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            // Native countdown chronometer when supported (API 24+)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setWhen(snap.expiryWallMs)
            .setShowWhen(true)
        return builder.build()
    }

    companion object {
        private const val NOTIF_ID = 71001
        /** Poll blocked apps ~1s; notification text updates only when MM:SS changes. */
        private const val POLL_MS = 500L

        fun start(context: Context) {
            val i = Intent(context, SalahLockMonitorService::class.java)
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(i)
                } else {
                    context.startService(i)
                }
            } catch (_: Exception) {
                // OEM may block FGS from background; Accessibility + watchdog remain
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, SalahLockMonitorService::class.java))
        }
    }
}

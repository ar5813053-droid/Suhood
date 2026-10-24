package com.suhood.feature.focus

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.suhood.app.SuhoodApplication
import java.lang.ref.WeakReference

/**
 * Detects blocked apps during Salah lock / Focus.
 * Shows [SalahLockActivity] on top and pulls the user out of the blocked app.
 */
class FocusAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var lastActionMs = 0L
    private var lastPkg = ""

    private val pollWhileLocked = object : Runnable {
        override fun run() {
            val store = SalahLockStore.get(applicationContext)
            val now = System.currentTimeMillis()
            val snap = store.reconcileNow(now)
            if (!SalahLockLogic.isActive(snap, now)) return
            val fg = rootInActiveWindow?.packageName?.toString()
            if (fg != null &&
                SalahLockLogic.shouldBlockPackage(snap, now, fg, packageName)
            ) {
                showLockOverBlockedApp(fg, snap.prayerName)
            }
            handler.postDelayed(this, 800L)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instanceRef = WeakReference(this)
        val snap = SalahLockStore.get(applicationContext).reconcileNow()
        if (SalahLockLogic.isActive(snap, System.currentTimeMillis())) {
            handler.removeCallbacks(pollWhileLocked)
            handler.post(pollWhileLocked)
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(pollWhileLocked)
        if (instanceRef?.get() === this) instanceRef = null
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val type = event.eventType
        if (type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            type != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            type != AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) return

        val pkg = event.packageName?.toString()
            ?: rootInActiveWindow?.packageName?.toString()
            ?: return
        if (pkg == packageName) return

        val now = System.currentTimeMillis()
        val salahSnap = SalahLockStore.get(applicationContext).reconcileNow(now)

        if (SalahLockLogic.isActive(salahSnap, now)) {
            handler.removeCallbacks(pollWhileLocked)
            handler.post(pollWhileLocked)
            if (SalahLockLogic.shouldBlockPackage(salahSnap, now, pkg, packageName)) {
                showLockOverBlockedApp(pkg, salahSnap.prayerName)
            }
            return
        }

        val session = FocusSessionController.get(applicationContext).session.value
        if (session.state != com.suhood.core.model.FocusSessionState.RUNNING) return
        val blocked = session.selectedAppIds.toSet()
        if (blocked.isEmpty() || pkg !in blocked) return
        if (pkg == lastPkg && now - lastActionMs < 600) return
        lastPkg = pkg
        lastActionMs = now
        performGlobalAction(GLOBAL_ACTION_HOME)
        startActivity(
            Intent(this, FocusBlockActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
                putExtra(FocusBlockActivity.EXTRA_PACKAGE, pkg)
            }
        )
    }

    override fun onInterrupt() {}

    /**
     * Show lock screen first (so user sees the premium UI), then leave the blocked app.
     * Retries activity start — OEM launchers often win the race after HOME-only.
     */
    private fun showLockOverBlockedApp(pkg: String, prayer: String) {
        val now = System.currentTimeMillis()
        if (pkg == lastPkg && now - lastActionMs < 900) return
        lastPkg = pkg
        lastActionMs = now

        // 1) Launch lock UI immediately (on top of Instagram if possible)
        launchSalahLock(prayer, pkg)
        // 2) Full-screen notification as OS-level backup
        postFullScreenLockNotification(prayer)
        // 3) HOME so blocked app loses focus, then bring lock to front again
        handler.postDelayed({ performGlobalAction(GLOBAL_ACTION_HOME) }, 80L)
        handler.postDelayed({ launchSalahLock(prayer, pkg) }, 350L)
        handler.postDelayed({ launchSalahLock(prayer, pkg) }, 700L)
    }

    private fun launchSalahLock(prayer: String, pkg: String) {
        runCatching {
            val intent = Intent(this, SalahLockActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                        Intent.FLAG_ACTIVITY_NO_USER_ACTION
                )
                putExtra(SalahLockActivity.EXTRA_PACKAGE, pkg)
                putExtra(SalahLockActivity.EXTRA_PRAYER, prayer)
            }
            startActivity(intent)
        }
    }

    private fun postFullScreenLockNotification(prayer: String) {
        runCatching {
            val full = PendingIntent.getActivity(
                this,
                71111,
                Intent(this, SalahLockActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra(SalahLockActivity.EXTRA_PRAYER, prayer)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val n = NotificationCompat.Builder(this, SuhoodApplication.CHANNEL_SALAH_LOCK)
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setContentTitle("It's Salah Time")
                .setContentText("Selected apps are locked — tap to view timer")
                .setContentIntent(full)
                .setFullScreenIntent(full, true)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(this).notify(71111, n)
        }
    }

    companion object {
        @Volatile
        private var instanceRef: WeakReference<FocusAccessibilityService>? = null

        fun isEnabled(context: android.content.Context): Boolean {
            val expected =
                "${context.packageName}/${FocusAccessibilityService::class.java.canonicalName}"
            val enabled = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
        }

        fun openSettings(context: android.content.Context) {
            context.startActivity(
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }

        fun onSalahLockActivated(context: android.content.Context, prayer: String) {
            val svc = instanceRef?.get()
            if (svc != null) {
                svc.handler.removeCallbacks(svc.pollWhileLocked)
                svc.handler.post(svc.pollWhileLocked)
                val fg = svc.rootInActiveWindow?.packageName?.toString() ?: ""
                svc.showLockOverBlockedApp(fg.ifBlank { "app" }, prayer)
            } else {
                context.startActivity(
                    Intent(context, SalahLockActivity::class.java).apply {
                        addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP
                        )
                        putExtra(SalahLockActivity.EXTRA_PRAYER, prayer)
                    }
                )
            }
        }
    }
}

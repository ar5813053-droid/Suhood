package com.suhood.feature.focus

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import java.lang.ref.WeakReference

/**
 * Detects when a Salah-locked (or Focus-restricted) app is in the foreground.
 * Performs GLOBAL_ACTION_HOME so the user is pulled out of the blocked app,
 * then shows [SalahLockActivity].
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
            if (!SalahLockLogic.isActive(snap, now)) {
                return // stop polling until next lock
            }
            val fg = rootInActiveWindow?.packageName?.toString()
            if (fg != null &&
                SalahLockLogic.shouldBlockPackage(snap, now, fg, packageName)
            ) {
                pullOutAndShowLock(fg, snap.prayerName)
            }
            handler.postDelayed(this, 700L)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instanceRef = WeakReference(this)
        // If a lock is already active when service connects, start polling
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
        val salahStore = SalahLockStore.get(applicationContext)
        val salahSnap = salahStore.reconcileNow(now)

        if (SalahLockLogic.isActive(salahSnap, now)) {
            // Keep polling while locked
            handler.removeCallbacks(pollWhileLocked)
            handler.post(pollWhileLocked)
            if (SalahLockLogic.shouldBlockPackage(salahSnap, now, pkg, packageName)) {
                pullOutAndShowLock(pkg, salahSnap.prayerName)
            }
            return
        }

        // Focus session path
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

    private fun pullOutAndShowLock(pkg: String, prayer: String) {
        val now = System.currentTimeMillis()
        // Allow repeated enforcement but avoid tight loop spam
        if (pkg == lastPkg && now - lastActionMs < 450) return
        lastPkg = pkg
        lastActionMs = now
        // Leave the blocked app first — critical for Instagram already in foreground
        performGlobalAction(GLOBAL_ACTION_HOME)
        handler.postDelayed({
            startActivity(
                Intent(this, SalahLockActivity::class.java).apply {
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
            )
        }, 120L)
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

        /**
         * Called when Salah lock activates (from BroadcastReceiver).
         * Immediately leaves the current app and shows the lock screen.
         */
        fun onSalahLockActivated(context: android.content.Context, prayer: String) {
            val svc = instanceRef?.get()
            if (svc != null) {
                svc.handler.removeCallbacks(svc.pollWhileLocked)
                svc.handler.post(svc.pollWhileLocked)
                val fg = svc.rootInActiveWindow?.packageName?.toString()
                val snap = SalahLockStore.get(context).read()
                val now = System.currentTimeMillis()
                if (fg != null && SalahLockLogic.shouldBlockPackage(snap, now, fg, context.packageName)) {
                    svc.pullOutAndShowLock(fg, prayer)
                } else {
                    // Still show lock + go home so user leaves whatever app they are in
                    svc.performGlobalAction(GLOBAL_ACTION_HOME)
                    svc.handler.postDelayed({
                        svc.startActivity(
                            Intent(svc, SalahLockActivity::class.java).apply {
                                addFlags(
                                    Intent.FLAG_ACTIVITY_NEW_TASK or
                                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                                )
                                putExtra(SalahLockActivity.EXTRA_PRAYER, prayer)
                            }
                        )
                    }, 120L)
                }
            } else {
                // Service not bound — best effort activity only
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

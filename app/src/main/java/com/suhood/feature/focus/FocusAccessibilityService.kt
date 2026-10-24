package com.suhood.feature.focus

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import java.lang.ref.WeakReference

/**
 * Detects blocked apps during Salah lock / Focus.
 * Shows lock via TYPE_ACCESSIBILITY_OVERLAY (reliable over Instagram).
 * Does not spam notifications on every open.
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
                SalahLockOverlay.hide(this@FocusAccessibilityService)
                return
            }
            val fg = rootInActiveWindow?.packageName?.toString()
            if (fg != null &&
                SalahLockLogic.shouldBlockPackage(snap, now, fg, packageName)
            ) {
                enforceLock(fg, snap.prayerName)
            }
            handler.postDelayed(this, 700L)
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
        SalahLockOverlay.hide(this)
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
                enforceLock(pkg, salahSnap.prayerName)
            }
            return
        } else {
            SalahLockOverlay.hide(this)
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

    /** Show overlay + mild HOME — no notification spam. */
    private fun enforceLock(pkg: String, prayer: String) {
        val now = System.currentTimeMillis()
        if (pkg == lastPkg && now - lastActionMs < 500 && SalahLockOverlay.isShowing()) return
        lastPkg = pkg
        lastActionMs = now
        SalahLockOverlay.show(this)
        // Pull blocked app down under the overlay
        handler.postDelayed({ performGlobalAction(GLOBAL_ACTION_HOME) }, 60L)
        // Also try activity (some devices prefer it for recents)
        handler.postDelayed({
            runCatching {
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
        }, 200L)
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
                val fg = svc.rootInActiveWindow?.packageName?.toString() ?: "app"
                svc.enforceLock(fg, prayer)
            }
        }
    }
}

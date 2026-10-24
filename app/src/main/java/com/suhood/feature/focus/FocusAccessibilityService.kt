package com.suhood.feature.focus

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.SystemClock
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent

/**
 * Foreground-app detection for Focus sessions and Salah locks.
 * Shows [SalahLockActivity] or [FocusBlockActivity]. Does not force-stop apps.
 */
class FocusAccessibilityService : AccessibilityService() {

    private var lastLaunchMs = 0L
    private var lastPkg = ""

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return

        val now = System.currentTimeMillis()
        // Throttle relaunch loops
        if (pkg == lastPkg && now - lastLaunchMs < 800) return

        // 1) Salah lock (10 min fixed window — independent of Focus cancel)
        val salahStore = SalahLockStore.get(applicationContext)
        val salahSnap = salahStore.reconcileNow(now)
        if (SalahLockLogic.shouldBlockPackage(salahSnap, now, pkg, packageName)) {
            lastPkg = pkg
            lastLaunchMs = now
            startActivity(
                Intent(this, SalahLockActivity::class.java).apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP or
                            Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    )
                    putExtra(SalahLockActivity.EXTRA_PACKAGE, pkg)
                    putExtra(SalahLockActivity.EXTRA_PRAYER, salahSnap.prayerName)
                }
            )
            return
        }

        // 2) Focus session block
        val session = FocusSessionController.get(applicationContext).session.value
        if (session.state != com.suhood.core.model.FocusSessionState.RUNNING) return
        val blocked = session.selectedAppIds.toSet()
        if (blocked.isEmpty() || pkg !in blocked) return
        lastPkg = pkg
        lastLaunchMs = now
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

    companion object {
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
    }
}

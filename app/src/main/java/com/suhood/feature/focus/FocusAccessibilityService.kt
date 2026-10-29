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

    private val pollWhileFocusGate = object : Runnable {
        override fun run() {
            val challenge = FocusChallengeStore.get(applicationContext)
            if (!challenge.isFeatureEnabled()) return
            if (challenge.hasActivePass()) {
                FocusChallengeOverlay.hide(this@FocusAccessibilityService)
                return
            }
            // Don't fight Salah lock
            val now = System.currentTimeMillis()
            if (SalahLockLogic.isActive(SalahLockStore.get(applicationContext).reconcileNow(now), now)) {
                return
            }
            val fg = rootInActiveWindow?.packageName?.toString() ?: return
            if (fg == packageName) {
                handler.postDelayed(this, 800L)
                return
            }
            val blocked = FocusSessionController.get(applicationContext).getBlockedApps()
            val isBlocked = blocked.isNotEmpty() && (
                fg in blocked || blocked.any {
                    fg.startsWith("$it.") ||
                        (it.startsWith("com.instagram") && fg.startsWith("com.instagram"))
                }
            )
            if (isBlocked && !FocusChallengeOverlay.isShowing()) {
                FocusChallengeOverlay.show(this@FocusAccessibilityService, fg, challenge.challengesRemaining())
            }
            handler.postDelayed(this, 800L)
        }
    }

    private val pollWhileLocked = object : Runnable {
        override fun run() {
            val store = SalahLockStore.get(applicationContext)
            val now = System.currentTimeMillis()
            val snap = store.reconcileNow(now)
            if (!SalahLockLogic.isActive(snap, now)) {
                SalahLockOverlay.hide(this@FocusAccessibilityService)
                // Keep heartbeat so we catch lock activation after process restart
                handler.postDelayed(this, 1_200L)
                return
            }
            SalahLockMonitorService.start(applicationContext)
            val fg = rootInActiveWindow?.packageName?.toString()
            if (fg != null &&
                SalahLockLogic.shouldBlockPackage(snap, now, fg, packageName)
            ) {
                enforceLock(fg, snap.prayerName)
            }
            handler.postDelayed(this, 400L)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instanceRef = WeakReference(this)
        // Always arm lock poller — lock may activate while process was dead / not in Recents
        handler.removeCallbacks(pollWhileLocked)
        handler.post(pollWhileLocked)
        handler.removeCallbacks(pollWhileFocusGate)
        handler.post(pollWhileFocusGate)
        val now = System.currentTimeMillis()
        val snap = SalahLockStore.get(applicationContext).reconcileNow(now)
        if (SalahLockLogic.isActive(snap, now)) {
            SalahLockMonitorService.start(applicationContext)
            val fg = rootInActiveWindow?.packageName?.toString()
            if (fg != null && SalahLockLogic.shouldBlockPackage(snap, now, fg, packageName)) {
                enforceLock(fg, snap.prayerName)
            }
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(pollWhileLocked)
        handler.removeCallbacks(pollWhileFocusGate)
        SalahLockOverlay.hide(this)
        FocusChallengeOverlay.hide(this)
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

        // Focus / distracting-app gate (any time, not only active Focus session)
        val blocked = FocusSessionController.get(applicationContext).getBlockedApps()
        if (blocked.isEmpty()) return
        val isBlocked = pkg in blocked || blocked.any {
            pkg.startsWith("$it.") || (it.startsWith("com.instagram") && pkg.startsWith("com.instagram"))
        }
        if (!isBlocked) return
        if (pkg == lastPkg && now - lastActionMs < 700) return
        lastPkg = pkg
        lastActionMs = now

        val challenge = FocusChallengeStore.get(applicationContext)
        if (!challenge.isFeatureEnabled()) return
        // Short pass after completing a mini-game
        if (challenge.hasActivePass()) return

        handler.removeCallbacks(pollWhileFocusGate)
        handler.post(pollWhileFocusGate)

        // Prayer-Pause style: overlay on top of the blocked app (reliable)
        if (FocusChallengeOverlay.isShowing()) return
        val remaining = challenge.challengesRemaining()
        FocusChallengeOverlay.show(this, pkg, remaining)
        // Also try activity as backup
        if (challenge.canOfferChallenge()) {
            runCatching {
                startActivity(
                    Intent(this, FocusChallengeActivity::class.java).apply {
                        addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                        )
                        putExtra(FocusChallengeActivity.EXTRA_PACKAGE, pkg)
                    }
                )
            }
        } else {
            runCatching {
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
        }
    }

    override fun onInterrupt() {}

    /** Show overlay + mild HOME — no notification spam. */
    private fun enforceLock(pkg: String, prayer: String) {
        val now = System.currentTimeMillis()
        if (pkg == lastPkg && now - lastActionMs < 450 && SalahLockOverlay.isShowing()) return
        lastPkg = pkg
        lastActionMs = now
        FocusChallengeOverlay.hide(this)
        // 1) Full-screen accessibility overlay ON TOP of the blocked app (no notification tap)
        SalahLockOverlay.show(this)
        // 2) Send user to Home so they leave the blocked app (overlay may dismiss on some OEMs)
        handler.postDelayed({
            runCatching { performGlobalAction(GLOBAL_ACTION_HOME) }
        }, 120L)
        // 3) Backup: full-screen SalahLockActivity with remaining countdown
        handler.postDelayed({
            runCatching {
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
            }
        }, 280L)
        // Keep polling while lock is active
        handler.removeCallbacks(pollWhileLocked)
        handler.postDelayed(pollWhileLocked, 400L)
    }

    companion object {
        @Volatile
        private var instanceRef: WeakReference<FocusAccessibilityService>? = null

        fun currentForegroundPackage(): String? {
            return try {
                instanceRef?.get()?.rootInActiveWindow?.packageName?.toString()
            } catch (_: Exception) {
                null
            }
        }

        fun isEnabled(context: android.content.Context): Boolean {
            val enabled = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            val needle = FocusAccessibilityService::class.java.name
            return enabled.split(':').any { part ->
                part.contains(needle, ignoreCase = true) ||
                    part.endsWith("/${FocusAccessibilityService::class.java.simpleName}", ignoreCase = true)
            }
        }

        fun openSettings(context: android.content.Context) {
            context.startActivity(
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }

        fun onSalahLockActivated(context: android.content.Context, prayer: String) {
            val svc = instanceRef?.get()
            val now = System.currentTimeMillis()
            val snap = SalahLockStore.get(context).reconcileNow(now)
            if (!SalahLockLogic.isActive(snap, now)) return
            // Always start monitor FGS for live countdown notification
            SalahLockMonitorService.start(context.applicationContext)
            if (svc == null) return
            svc.handler.removeCallbacks(svc.pollWhileLocked)
            svc.handler.post(svc.pollWhileLocked)
            val fg = svc.rootInActiveWindow?.packageName?.toString()
            if (fg != null && SalahLockLogic.shouldBlockPackage(snap, now, fg, context.packageName)) {
                svc.enforceLock(fg, prayer.ifBlank { snap.prayerName })
            }
        }
    }
}

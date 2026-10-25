package com.suhood.feature.focus

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper

/**
 * Single entry point to force the user out of a blocked app during an active Salah lock.
 *
 * Works even when Accessibility is off (launches [SalahLockActivity]).
 * When Accessibility is on, also shows overlay + GLOBAL_ACTION_HOME.
 */
object SalahLockEnforcer {

    private val main = Handler(Looper.getMainLooper())
    @Volatile private var lastEnforceMs = 0L
    @Volatile private var lastPkg = ""

    fun matchesBlocked(pkg: String, blocked: Set<String>, selfPkg: String): Boolean {
        if (pkg.isBlank() || pkg == selfPkg) return false
        if (pkg in blocked) return true
        return blocked.any { b ->
            b.isNotBlank() && (
                pkg == b ||
                    pkg.startsWith("$b.") ||
                    b.startsWith("$pkg.") ||
                    (b.contains("instagram") && pkg.contains("instagram")) ||
                    (b.contains("tiktok") && (pkg.contains("tiktok") || pkg.contains("musically"))) ||
                    (b.contains("youtube") && pkg.contains("youtube")) ||
                    (b.contains("facebook") && pkg.contains("facebook")) ||
                    (b.contains("snapchat") && pkg.contains("snapchat"))
                )
        }
    }

    /**
     * Call when a blocked package is in the foreground during an active lock.
     * Rate-limited so we do not spam activities.
     */
    fun enforce(context: Context, blockedPkg: String, prayerName: String) {
        val app = context.applicationContext
        val now = System.currentTimeMillis()
        val snap = SalahLockStore.get(app).reconcileNow(now)
        if (!SalahLockLogic.isActive(snap, now)) return
        if (!matchesBlocked(blockedPkg, snap.blockedPackages, app.packageName)) return

        // Avoid thrashing the same package more than once per 1.2s
        if (blockedPkg == lastPkg && now - lastEnforceMs < 1_200L) return
        lastPkg = blockedPkg
        lastEnforceMs = now

        // Path A — always: full-screen lock activity (works without Accessibility)
        main.post {
            runCatching {
                app.startActivity(
                    Intent(app, SalahLockActivity::class.java).apply {
                        addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                                Intent.FLAG_ACTIVITY_NO_USER_ACTION
                        )
                        putExtra(SalahLockActivity.EXTRA_PACKAGE, blockedPkg)
                        putExtra(SalahLockActivity.EXTRA_PRAYER, prayerName.ifBlank { snap.prayerName })
                    }
                )
            }
        }

        // Path B — Accessibility: overlay + leave blocked app to Home
        main.post {
            runCatching {
                FocusAccessibilityService.onSalahLockActivated(
                    app,
                    prayerName.ifBlank { snap.prayerName }
                )
            }
        }
    }

    /** On Salah start: if already inside a blocked app, kick out immediately. */
    fun onLockActivated(context: Context, prayerName: String) {
        val app = context.applicationContext
        val now = System.currentTimeMillis()
        val snap = SalahLockStore.get(app).reconcileNow(now)
        if (!SalahLockLogic.isActive(snap, now)) return
        SalahLockMonitorService.start(app)
        val fg = AppBlockHelper.foregroundPackage(app)
        if (fg != null && matchesBlocked(fg, snap.blockedPackages, app.packageName)) {
            enforce(app, fg, prayerName.ifBlank { snap.prayerName })
        } else {
            // Still arm Accessibility poll for when user opens a blocked app next
            runCatching {
                FocusAccessibilityService.onSalahLockActivated(app, prayerName)
            }
        }
    }
}

package com.suhood.feature.focus

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper

/**
 * After the user unlocks the phone (or the screen turns on), immediately re-check
 * whether a Salah lock is active and whether a blocked app is in the foreground.
 *
 * Common bypass: user locks phone while in Instagram → unlocks → Instagram resumes
 * without a WINDOW_STATE_CHANGED event that our Accessibility path catches in time.
 */
class SalahUnlockReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_USER_PRESENT &&
            action != Intent.ACTION_SCREEN_ON
        ) return

        val app = context.applicationContext
        val now = System.currentTimeMillis()
        val snap = SalahLockStore.get(app).reconcileNow(now)
        if (!SalahLockLogic.isActive(snap, now)) return

        // Ensure FGS + watchdog are alive after unlock
        SalahLockMonitorService.start(app)
        SalahLockWatchdog.schedule(app, snap.expiryWallMs)

        // Burst re-check: unlock → launcher/app resume can lag UsageStats by 1–3s
        val main = Handler(Looper.getMainLooper())
        fun kick(attempt: Int) {
            val s = SalahLockStore.get(app).reconcileNow()
            if (!SalahLockLogic.isActive(s, System.currentTimeMillis())) return
            val fg = AppBlockHelper.isBlockedAppForeground(app, s.blockedPackages)
                ?: FocusAccessibilityService.currentForegroundPackage()?.takeIf {
                    SalahLockEnforcer.matchesBlocked(it, s.blockedPackages, app.packageName)
                }
            if (fg != null) {
                SalahLockEnforcer.enforce(app, fg, s.prayerName.ifBlank { "Salah" })
            } else if (attempt == 0) {
                // Still show lock activity as a hard gate if we cannot yet resolve FG
                // but only when Accessibility reports a blocked root shortly after.
                FocusAccessibilityService.onSalahLockActivated(app, s.prayerName)
            }
            if (attempt < 6) {
                main.postDelayed({ kick(attempt + 1) }, 500L)
            }
        }
        main.post { kick(0) }
    }
}

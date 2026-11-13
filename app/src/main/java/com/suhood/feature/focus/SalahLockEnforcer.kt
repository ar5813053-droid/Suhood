package com.suhood.feature.focus

import android.app.ActivityOptions
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.suhood.app.SuhoodApplication

/**
 * Force-exit blocked apps during an active Salah lock and show the lock screen.
 *
 * Paths (in order):
 * 1. Launch [SalahLockActivity] (best from foreground service)
 * 2. Accessibility overlay + GLOBAL_ACTION_HOME
 * 3. Full-screen intent notification (when activity start is blocked)
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
                    (b.contains("snapchat") && pkg.contains("snapchat")) ||
                    (b.contains("whatsapp") && pkg.contains("whatsapp"))
                )
        }
    }

    fun launchLockActivity(context: Context, blockedPkg: String, prayerName: String) {
        val app = context.applicationContext
        val intent = Intent(app, SalahLockActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                    Intent.FLAG_ACTIVITY_NO_USER_ACTION
            )
            putExtra(SalahLockActivity.EXTRA_PACKAGE, blockedPkg)
            putExtra(SalahLockActivity.EXTRA_PRAYER, prayerName)
        }
        runCatching {
            if (Build.VERSION.SDK_INT >= 34) {
                val opts = ActivityOptions.makeBasic()
                @Suppress("MissingPermission")
                opts.setPendingIntentBackgroundActivityStartMode(
                    ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                )
                app.startActivity(intent, opts.toBundle())
            } else {
                app.startActivity(intent)
            }
        }.onFailure {
            // Fallback: full-screen intent notification
            postFullScreenLock(app, blockedPkg, prayerName)
        }
    }

    private fun postFullScreenLock(app: Context, blockedPkg: String, prayerName: String) {
        val intent = Intent(app, SalahLockActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(SalahLockActivity.EXTRA_PACKAGE, blockedPkg)
            putExtra(SalahLockActivity.EXTRA_PRAYER, prayerName)
        }
        val pi = PendingIntent.getActivity(
            app,
            0x51A10 + (blockedPkg.hashCode() and 0xFF),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(app, SuhoodApplication.CHANNEL_SALAH_LOCK)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle("${prayerName.ifBlank { "Salah" }} — App locked")
            .setContentText("Return to Allah. Tap if lock screen did not open.")
            .setContentIntent(pi)
            .setFullScreenIntent(pi, true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .build()
        runCatching {
            NotificationManagerCompat.from(app).notify(0x51A100 + (blockedPkg.hashCode() and 0xFF), n)
        }
    }

    /**
     * Call when a blocked package is in the foreground during an active lock.
     */
    fun enforce(context: Context, blockedPkg: String, prayerName: String) {
        val app = context.applicationContext
        val now = System.currentTimeMillis()
        val snap = SalahLockStore.get(app).reconcileNow(now)
        if (!SalahLockLogic.isActive(snap, now)) return
        if (!matchesBlocked(blockedPkg, snap.blockedPackages, app.packageName)) return

        // Rate-limit same package slightly; always allow after 700ms
        if (blockedPkg == lastPkg && now - lastEnforceMs < 700L) return
        lastPkg = blockedPkg
        lastEnforceMs = now

        val prayer = prayerName.ifBlank { snap.prayerName }

        // Path A — lock activity (works best when called from FGS)
        main.post { launchLockActivity(app, blockedPkg, prayer) }

        // Path B — Accessibility overlay (same TYPE_ACCESSIBILITY_OVERLAY stack as 3-scroll)
        main.post {
            runCatching {
                FocusAccessibilityService.onSalahLockActivated(app, prayer)
            }
        }

        // Path C — Display-over-apps overlay when Accessibility is unavailable
        // Detection still comes from Usage Access via AppBlockHelper / monitor service.
        main.post {
            if (!FocusAccessibilityService.isEnabled(app) && AppBlockHelper.canDrawOverlays(app)) {
                SalahLockOverlay.showFromContext(app)
            }
        }
    }

    /** On Salah start: arm monitor and kick out if already inside a blocked app. */
    fun onLockActivated(context: Context, prayerName: String) {
        val app = context.applicationContext
        val now = System.currentTimeMillis()
        val snap = SalahLockStore.get(app).reconcileNow(now)
        if (!SalahLockLogic.isActive(snap, now)) return

        SalahLockMonitorService.start(app)

        // Prefer Accessibility package (instant) then UsageStats
        val fgA11y = FocusAccessibilityService.currentForegroundPackage()
        val fgUsage = AppBlockHelper.foregroundPackage(app)
        val fg = when {
            fgA11y != null && matchesBlocked(fgA11y, snap.blockedPackages, app.packageName) -> fgA11y
            fgUsage != null && matchesBlocked(fgUsage, snap.blockedPackages, app.packageName) -> fgUsage
            fgA11y != null -> fgA11y
            else -> fgUsage
        }

        if (fg != null && matchesBlocked(fg, snap.blockedPackages, app.packageName)) {
            enforce(app, fg, prayerName.ifBlank { snap.prayerName })
        } else {
            // Still arm Accessibility poll for the next open
            runCatching {
                FocusAccessibilityService.onSalahLockActivated(app, prayerName)
            }
        }
    }
}

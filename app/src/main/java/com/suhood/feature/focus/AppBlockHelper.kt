package com.suhood.feature.focus

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings

object AppBlockHelper {

    fun hasUsageAccess(context: Context): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) {
            false
        }
    }

    fun openUsageAccessSettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun hasAccessibility(context: Context): Boolean =
        FocusAccessibilityService.isEnabled(context)

    fun openAccessibilitySettings(context: Context) =
        FocusAccessibilityService.openSettings(context)

    fun canDrawOverlays(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    fun openOverlaySettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    android.net.Uri.parse("package:${context.packageName}")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    /**
     * True only when a mechanism that can present a block UI is available.
     * Usage-only detection is not treated as active blocking.
     */
    fun isBlockingMechanismReady(context: Context): Boolean {
        val a11y = hasAccessibility(context)
        val usage = hasUsageAccess(context)
        val overlay = canDrawOverlays(context)
        return a11y || (usage && overlay)
    }

    fun foregroundPackage(context: Context): String? {
        // 1) Accessibility is most accurate when service is connected
        FocusAccessibilityService.currentForegroundPackage()?.let { return it }

        if (!hasUsageAccess(context)) return null
        return try {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val end = System.currentTimeMillis()
            // Events first
            val events = usm.queryEvents(end - 30_000, end)
            val event = UsageEvents.Event()
            var last: String? = null
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND ||
                    event.eventType == UsageEvents.Event.ACTIVITY_RESUMED
                ) {
                    last = event.packageName
                }
            }
            if (last != null) return last
            // Fallback: most recent usage stats
            @Suppress("DEPRECATION")
            val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_BEST, end - 60_000, end)
            stats?.maxByOrNull { it.lastTimeUsed }?.packageName
        } catch (_: Exception) {
            null
        }
    }

    fun isBlockedAppForeground(context: Context, blockedPackages: Set<String>): String? {
        if (blockedPackages.isEmpty()) return null
        val fg = foregroundPackage(context) ?: return null
        if (SalahLockEnforcer.matchesBlocked(fg, blockedPackages, context.packageName)) return fg
        return null
    }
}

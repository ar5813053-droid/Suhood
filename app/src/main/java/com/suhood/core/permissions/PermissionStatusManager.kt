package com.suhood.core.permissions

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.suhood.feature.focus.AppBlockHelper
import com.suhood.feature.focus.FocusAccessibilityService

enum class PermissionKind {
    NOTIFICATIONS,
    USAGE_ACCESS,
    ACCESSIBILITY,
    OVERLAY,
    LOCATION,
    EXACT_ALARM
}

enum class PermissionApplicability { REQUIRED_FOR_FEATURE, OPTIONAL, NOT_APPLICABLE }

data class PermissionItem(
    val kind: PermissionKind,
    val title: String,
    val reason: String,
    val feature: String,
    val granted: Boolean,
    val applicability: PermissionApplicability,
    /** True when Android/OEM may block enabling (e.g. restricted settings). */
    val mayBeRestricted: Boolean = false
)

data class PermissionSnapshot(
    val items: List<PermissionItem>,
    val missingApplicable: List<PermissionItem>,
    val blockingCapability: BlockingCapability
) {
    val missingCount: Int get() = missingApplicable.size
    val allApplicableComplete: Boolean get() = missingApplicable.isEmpty()
}

enum class BlockingMode {
    NONE,
    ACCESSIBILITY,
    USAGE_AND_OVERLAY,
    USAGE_ONLY_DETECT // can detect but not reliably block
}

data class BlockingCapability(
    val mode: BlockingMode,
    val canDetectForeground: Boolean,
    val canPresentBlockUi: Boolean,
    val isReliableBlocking: Boolean,
    val statusMessage: String
)

/**
 * Central permission + Focus blocking capability assessment.
 * Never claims blocking is active when the underlying mechanism is unavailable.
 */
object PermissionStatusManager {

    fun snapshot(context: Context): PermissionSnapshot {
        val items = listOf(
            notifItem(context),
            usageItem(context),
            a11yItem(context),
            overlayItem(context),
            locationItem(context),
            exactAlarmItem(context)
        )
        val missing = items.filter {
            it.applicability != PermissionApplicability.NOT_APPLICABLE && !it.granted
        }
        return PermissionSnapshot(
            items = items,
            missingApplicable = missing,
            blockingCapability = assessBlocking(context)
        )
    }

    fun assessBlocking(context: Context): BlockingCapability {
        val a11y = FocusAccessibilityService.isEnabled(context)
        val usage = AppBlockHelper.hasUsageAccess(context)
        val overlay = AppBlockHelper.canDrawOverlays(context)

        return when {
            a11y -> BlockingCapability(
                mode = BlockingMode.ACCESSIBILITY,
                canDetectForeground = true,
                canPresentBlockUi = true,
                isReliableBlocking = true,
                statusMessage = "Accessibility is enabled — Focus can detect and present the block screen."
            )
            usage && overlay -> BlockingCapability(
                mode = BlockingMode.USAGE_AND_OVERLAY,
                canDetectForeground = true,
                canPresentBlockUi = true,
                isReliableBlocking = false, // best-effort; OEMs may limit overlays
                statusMessage = "Using Usage Access + overlay fallback. Blocking is best-effort without Accessibility."
            )
            usage -> BlockingCapability(
                mode = BlockingMode.USAGE_ONLY_DETECT,
                canDetectForeground = true,
                canPresentBlockUi = false,
                isReliableBlocking = false,
                statusMessage = "Usage Access can detect apps, but without Accessibility or overlay Suhood cannot present a block screen."
            )
            else -> BlockingCapability(
                mode = BlockingMode.NONE,
                canDetectForeground = false,
                canPresentBlockUi = false,
                isReliableBlocking = false,
                statusMessage = "No detection permission enabled. Focus cannot block apps until Accessibility or Usage Access is granted."
            )
        }
    }

    fun openSettingsFor(context: Context, kind: PermissionKind) {
        val intent = when (kind) {
            PermissionKind.NOTIFICATIONS -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                } else {
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .setData(Uri.parse("package:${context.packageName}"))
                }
            }
            PermissionKind.USAGE_ACCESS -> Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            PermissionKind.ACCESSIBILITY -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            PermissionKind.OVERLAY -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                } else {
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .setData(Uri.parse("package:${context.packageName}"))
                }
            }
            PermissionKind.LOCATION -> Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            PermissionKind.EXACT_ALARM -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                } else {
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .setData(Uri.parse("package:${context.packageName}"))
                }
            }
        }
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun notifItem(context: Context): PermissionItem {
        val granted = NotificationManagerCompat.from(context).areNotificationsEnabled()
        return PermissionItem(
            kind = PermissionKind.NOTIFICATIONS,
            title = "Notifications",
            reason = "Prayer reminders, Focus updates, and optional setup reminders.",
            feature = "Reminders & Focus",
            granted = granted,
            applicability = PermissionApplicability.OPTIONAL
        )
    }

    private fun usageItem(context: Context): PermissionItem {
        val granted = AppBlockHelper.hasUsageAccess(context)
        return PermissionItem(
            kind = PermissionKind.USAGE_ACCESS,
            title = "Usage Access",
            reason = "Identify which app is open so Focus rules can apply when Accessibility is off.",
            feature = "Focus / Salah block",
            granted = granted,
            applicability = PermissionApplicability.OPTIONAL
        )
    }

    private fun a11yItem(context: Context): PermissionItem {
        val granted = FocusAccessibilityService.isEnabled(context)
        return PermissionItem(
            kind = PermissionKind.ACCESSIBILITY,
            title = "Accessibility Service",
            reason = "Detect selected apps during Focus / Salah lock and show your configured block screen. Some devices restrict this for sideloaded apps.",
            feature = "Focus / Salah block",
            granted = granted,
            applicability = PermissionApplicability.OPTIONAL,
            mayBeRestricted = true
        )
    }

    private fun overlayItem(context: Context): PermissionItem {
        val granted = AppBlockHelper.canDrawOverlays(context)
        return PermissionItem(
            kind = PermissionKind.OVERLAY,
            title = "Display over other apps",
            reason = "Show the Focus / Salah block screen when Accessibility is unavailable.",
            feature = "Focus fallback",
            granted = granted,
            applicability = PermissionApplicability.OPTIONAL
        )
    }

    private fun locationItem(context: Context): PermissionItem {
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return PermissionItem(
            kind = PermissionKind.LOCATION,
            title = "Location",
            reason = "Location-based prayer times. You can pick a city manually instead.",
            feature = "Prayer times",
            granted = fine || coarse,
            applicability = PermissionApplicability.OPTIONAL
        )
    }

    private fun exactAlarmItem(context: Context): PermissionItem {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return PermissionItem(
                kind = PermissionKind.EXACT_ALARM,
                title = "Exact alarms",
                reason = "Precise prayer and alarm times.",
                feature = "Alarms",
                granted = true,
                applicability = PermissionApplicability.NOT_APPLICABLE
            )
        }
        val am = context.getSystemService(AlarmManager::class.java)
        val granted = am?.canScheduleExactAlarms() == true
        return PermissionItem(
            kind = PermissionKind.EXACT_ALARM,
            title = "Exact alarms",
            reason = "Precise prayer and custom alarm times on Android 12+.",
            feature = "Alarms & Salah",
            granted = granted,
            applicability = PermissionApplicability.OPTIONAL
        )
    }
}

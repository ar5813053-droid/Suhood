package com.suhood.feature.focus

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import com.suhood.core.model.DistractingApp

data class InstalledAppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable?
)

object InstalledAppsRepository {

    /**
     * Launcher apps only. Defensive against package visibility / broken icons.
     * Never throws — returns empty list on total failure.
     */
    fun loadLauncherApps(context: Context): List<InstalledAppInfo> {
        return try {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            @Suppress("DEPRECATION")
            val resolved = try {
                if (android.os.Build.VERSION.SDK_INT >= 33) {
                    pm.queryIntentActivities(
                        intent,
                        PackageManager.ResolveInfoFlags.of(0L)
                    )
                } else {
                    pm.queryIntentActivities(intent, 0)
                }
            } catch (_: Exception) {
                emptyList()
            }
            resolved.mapNotNull { ri ->
                try {
                    val pkg = ri.activityInfo?.packageName ?: return@mapNotNull null
                    if (pkg == context.packageName) return@mapNotNull null
                    val label = try {
                        ri.loadLabel(pm)?.toString()?.takeIf { it.isNotBlank() } ?: pkg
                    } catch (_: Exception) {
                        pkg
                    }
                    val icon = try {
                        ri.loadIcon(pm)
                    } catch (_: Exception) {
                        null
                    }
                    InstalledAppInfo(pkg, label, icon)
                } catch (_: Exception) {
                    null
                }
            }
                .distinctBy { it.packageName }
                .sortedBy { it.label.lowercase() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun toDistracting(apps: List<InstalledAppInfo>, selected: Set<String>): List<DistractingApp> =
        apps.map {
            DistractingApp(
                id = it.packageName,
                name = it.label,
                packageName = it.packageName,
                isSelected = it.packageName in selected
            )
        }
}

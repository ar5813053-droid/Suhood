package com.suhood.feature.focus

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import com.suhood.core.model.DistractingApp

data class InstalledAppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable?
)

object InstalledAppsRepository {

    fun loadLauncherApps(context: Context): List<InstalledAppInfo> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        return resolved
            .mapNotNull { ri ->
                val pkg = ri.activityInfo.packageName
                if (pkg == context.packageName) return@mapNotNull null
                val label = ri.loadLabel(pm)?.toString() ?: pkg
                val icon = runCatching { ri.loadIcon(pm) }.getOrNull()
                InstalledAppInfo(pkg, label, icon)
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
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

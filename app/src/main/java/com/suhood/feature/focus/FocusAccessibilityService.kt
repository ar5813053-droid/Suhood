package com.suhood.feature.focus

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent

/**
 * AppBlock-style detection: when Focus is RUNNING and the user opens a selected
 * package, show [FocusBlockActivity]. Does not force-stop other apps.
 */
class FocusAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return
        val session = FocusSessionController.get(applicationContext).session.value
        if (session.state != com.suhood.core.model.FocusSessionState.RUNNING) return
        val blocked = session.selectedAppIds.toSet()
        if (blocked.isEmpty() || pkg !in blocked) return
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

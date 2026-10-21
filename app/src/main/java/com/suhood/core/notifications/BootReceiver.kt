package com.suhood.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.suhood.core.prayer.SalahReminderScheduler

/**
 * Reschedule local reminders after reboot / timezone changes.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == Intent.ACTION_TIME_CHANGED
        ) {
            runCatching { QuranReminderScheduler.rescheduleFromPrefs(context) }
            runCatching { SalahReminderScheduler.rescheduleAll(context) }
        }
    }
}

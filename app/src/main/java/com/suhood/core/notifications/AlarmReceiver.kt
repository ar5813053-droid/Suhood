package com.suhood.core.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.suhood.app.SuhoodApplication
import com.suhood.feature.alarms.AlarmRingActivity

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val label = intent.getStringExtra(EXTRA_LABEL) ?: "Suhood alarm"
        val alarmId = intent.getStringExtra(EXTRA_ALARM_ID) ?: "alarm"
        val soundUri = intent.getStringExtra(AlarmRingActivity.EXTRA_SOUND_URI).orEmpty()
        val challenge = intent.getStringExtra(AlarmRingActivity.EXTRA_CHALLENGE) ?: "NONE"

        val ringIntent = Intent(context, AlarmRingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_LABEL, label)
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmRingActivity.EXTRA_SOUND_URI, soundUri)
            putExtra(AlarmRingActivity.EXTRA_CHALLENGE, challenge)
        }
        val fullScreen = PendingIntent.getActivity(
            context,
            alarmId.hashCode(),
            ringIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, SuhoodApplication.CHANNEL_ALARM)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(label)
            .setContentText("Alarm — tap to open")
            .setContentIntent(fullScreen)
            .setFullScreenIntent(fullScreen, true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)
            .setOngoing(true)
            .build()

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED
            || Build.VERSION.SDK_INT < 33
        ) {
            NotificationManagerCompat.from(context).notify(alarmId.hashCode(), notification)
        }

        // Also try direct activity start (works when app is not restricted)
        runCatching { context.startActivity(ringIntent) }
    }

    companion object {
        const val EXTRA_LABEL = "label"
        const val EXTRA_ALARM_ID = "alarm_id"
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_TIMEZONE_CHANGED ||
            intent.action == Intent.ACTION_TIME_CHANGED
        ) {
            runCatching { AlarmScheduler.rescheduleAll(context) }
            runCatching { com.suhood.core.prayer.SalahReminderScheduler.rescheduleAll(context) }
            runCatching { com.suhood.core.prayer.FajrAutoScheduler.reschedule(context) }
            runCatching { com.suhood.feature.focus.SalahLockStore.get(context).reconcileNow() }
            runCatching { QuranReminderScheduler.rescheduleFromPrefs(context) }
            runCatching { com.suhood.widgets.WidgetBridge.refreshAll(context) }
            runCatching { com.suhood.widgets.WidgetBridge.scheduleNextPrayerRefresh(context) }
        }
    }
}

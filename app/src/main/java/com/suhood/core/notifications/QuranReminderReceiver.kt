package com.suhood.core.notifications

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.suhood.app.MainActivity
import com.suhood.app.SuhoodApplication

class QuranReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val open = PendingIntent.getActivity(
            context,
            REQ_OPEN,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("open", "quran_companion")
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(context, SuhoodApplication.CHANNEL_QURAN)
            .setSmallIcon(android.R.drawable.ic_menu_today)
            .setContentTitle("Quran Companion")
            .setContentText("Time for your daily Quran reading")
            .setContentIntent(open)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        runCatching {
            NotificationManagerCompat.from(context).notify(NOTIF_ID, n)
        }
        // Reschedule next day
        QuranReminderScheduler.rescheduleFromPrefs(context)
    }

    companion object {
        const val NOTIF_ID = 71001
        const val REQ_OPEN = 71002
        const val ACTION = "com.suhood.action.QURAN_DAILY_REMINDER"
    }
}

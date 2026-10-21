package com.suhood.widgets

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Refresh widgets at prayer boundaries without continuous polling. */
class WidgetRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        WidgetBridge.refreshAll(context)
        WidgetBridge.scheduleNextPrayerRefresh(context)
    }
}

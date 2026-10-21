package com.suhood.widgets

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class WidgetRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        WidgetBridge.refreshAll(context)
        WidgetBridge.scheduleNextPrayerRefresh(context)
    }
}

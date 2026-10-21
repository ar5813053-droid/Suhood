package com.suhood.widgets

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.suhood.feature.salah.SalahStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TogglePrayerAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val prayer = parameters[PrayerKey] ?: return
        withContext(Dispatchers.IO) {
            val snap = WidgetBridge.loadSalah(context)
            val current = snap.marked[prayer]
            when (current) {
                null, SalahStatus.NOT_PRAYED.key ->
                    WidgetBridge.markPrayer(context, prayer, SalahStatus.ON_TIME.key)
                SalahStatus.ON_TIME.key, SalahStatus.JAMA_AH.key ->
                    WidgetBridge.markPrayer(context, prayer, SalahStatus.LATE.key)
                SalahStatus.LATE.key -> {
                    val today = java.time.LocalDate.now().toString()
                    WidgetBridge.dao(context).deleteSalahLog("$today-$prayer")
                    WidgetBridge.refreshAll(context)
                }
                else -> WidgetBridge.markPrayer(context, prayer, SalahStatus.ON_TIME.key)
            }
        }
    }
    companion object {
        val PrayerKey = ActionParameters.Key<String>("prayer")
    }
}

class ToggleHabitAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[HabitKey] ?: return
        WidgetBridge.toggleHabit(context, id)
    }
    companion object {
        val HabitKey = ActionParameters.Key<String>("habit")
    }
}

class PlayPauseAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        runCatching { WidgetBridge.app(context).playbackController.togglePlayPause() }
        WidgetBridge.refreshAll(context)
    }
}

class SkipNextAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        runCatching { WidgetBridge.app(context).playbackController.next() }
        WidgetBridge.refreshAll(context)
    }
}

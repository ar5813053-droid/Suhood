package com.suhood.widgets

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.suhood.feature.salah.SalahStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Handles interactive widget taps (prayer mark, habit toggle, play/pause).
 * Writes through the same Room / Media3 paths as the main app.
 */
class WidgetClickReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val pending = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                when (action) {
                    ACTION_TOGGLE_PRAYER -> {
                        val prayer = intent.getStringExtra(EXTRA_PRAYER) ?: return@launch
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
                    ACTION_TOGGLE_HABIT -> {
                        val id = intent.getStringExtra(EXTRA_HABIT) ?: return@launch
                        WidgetBridge.toggleHabit(context, id)
                    }
                    ACTION_PLAY_PAUSE -> {
                        runCatching { WidgetBridge.app(context).playbackController.togglePlayPause() }
                        WidgetBridge.refreshAll(context)
                    }
                    ACTION_NEXT -> {
                        runCatching { WidgetBridge.app(context).playbackController.next() }
                        WidgetBridge.refreshAll(context)
                    }
                    ACTION_REFRESH -> WidgetBridge.refreshAll(context)
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_TOGGLE_PRAYER = "com.suhood.widgets.TOGGLE_PRAYER"
        const val ACTION_TOGGLE_HABIT = "com.suhood.widgets.TOGGLE_HABIT"
        const val ACTION_PLAY_PAUSE = "com.suhood.widgets.PLAY_PAUSE"
        const val ACTION_NEXT = "com.suhood.widgets.NEXT"
        const val ACTION_REFRESH = "com.suhood.widgets.REFRESH"
        const val EXTRA_PRAYER = "prayer"
        const val EXTRA_HABIT = "habit"
    }
}

package com.suhood.feature.focus

import android.content.Context
import android.content.SharedPreferences
import com.suhood.core.model.FocusSession
import com.suhood.core.model.FocusSessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Focus timer based on wall-clock end time so remaining time stays accurate
 * when the process is backgrounded or recreated.
 */
class FocusSessionController(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("focus_session", Context.MODE_PRIVATE)

    private val _session = MutableStateFlow(read())
    val session: StateFlow<FocusSession> = _session.asStateFlow()

    fun start(durationMinutes: Int, selectedAppIds: List<String> = emptyList()) {
        val now = System.currentTimeMillis()
        val totalSec = durationMinutes * 60
        val endAt = now + totalSec * 1000L
        prefs.edit()
            .putLong(KEY_END, endAt)
            .putInt(KEY_DURATION, durationMinutes)
            .putLong(KEY_STARTED, now)
            .putString(KEY_STATE, FocusSessionState.RUNNING.name)
            .putString(KEY_APPS, selectedAppIds.joinToString(","))
            .putLong(KEY_PAUSED_REMAINING, -1)
            .apply()
        _session.value = read()
    }

    fun pause() {
        val s = read()
        if (s.state != FocusSessionState.RUNNING) return
        prefs.edit()
            .putString(KEY_STATE, FocusSessionState.PAUSED.name)
            .putLong(KEY_PAUSED_REMAINING, s.remainingSeconds.toLong())
            .apply()
        _session.value = read()
    }

    fun resume() {
        val remaining = prefs.getLong(KEY_PAUSED_REMAINING, -1)
        if (remaining < 0) return
        val endAt = System.currentTimeMillis() + remaining * 1000L
        prefs.edit()
            .putLong(KEY_END, endAt)
            .putString(KEY_STATE, FocusSessionState.RUNNING.name)
            .putLong(KEY_PAUSED_REMAINING, -1)
            .apply()
        _session.value = read()
    }

    fun cancel() {
        prefs.edit().clear().apply()
        _session.value = FocusSession(durationMinutes = 25, remainingSeconds = 25 * 60)
    }

    fun complete() {
        prefs.edit().putString(KEY_STATE, FocusSessionState.COMPLETED.name).apply()
        _session.value = read()
    }

    fun tick(): FocusSession {
        val s = read()
        _session.value = s
        if (s.state == FocusSessionState.RUNNING && s.remainingSeconds <= 0) {
            complete()
            return read()
        }
        return s
    }

    private fun read(): FocusSession {
        val stateName = prefs.getString(KEY_STATE, FocusSessionState.IDLE.name)
            ?: FocusSessionState.IDLE.name
        val state = runCatching { FocusSessionState.valueOf(stateName) }
            .getOrDefault(FocusSessionState.IDLE)
        val duration = prefs.getInt(KEY_DURATION, 25)
        val apps = prefs.getString(KEY_APPS, "")?.split(',')?.filter { it.isNotBlank() }.orEmpty()
        val remaining = when (state) {
            FocusSessionState.RUNNING -> {
                val end = prefs.getLong(KEY_END, 0)
                ((end - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0)
            }
            FocusSessionState.PAUSED -> prefs.getLong(KEY_PAUSED_REMAINING, duration * 60L).toInt()
            FocusSessionState.COMPLETED -> 0
            else -> duration * 60
        }
        return FocusSession(
            durationMinutes = duration,
            remainingSeconds = remaining,
            state = state,
            selectedAppIds = apps,
            startedAt = prefs.getLong(KEY_STARTED, 0).takeIf { it > 0 }
        )
    }

    companion object {
        private const val KEY_END = "end_at"
        private const val KEY_DURATION = "duration"
        private const val KEY_STARTED = "started"
        private const val KEY_STATE = "state"
        private const val KEY_APPS = "apps"
        private const val KEY_PAUSED_REMAINING = "paused_remaining"

        @Volatile private var instance: FocusSessionController? = null
        fun get(context: Context): FocusSessionController =
            instance ?: synchronized(this) {
                instance ?: FocusSessionController(context.applicationContext).also { instance = it }
            }
    }
}

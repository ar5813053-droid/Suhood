package com.suhood.feature.phone

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.telecom.Call
import android.telecom.InCallService
import android.telecom.VideoProfile
import androidx.core.app.NotificationCompat
import com.suhood.app.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * System InCallService. Only bound when Suhood is the default dialer.
 * Opens [PhoneCallActivity] and posts a full-screen notification for lock-screen.
 */
class SuhoodInCallService : InCallService() {

    private val callback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            CallSession.onState(state)
            if (state == Call.STATE_DISCONNECTED || state == Call.STATE_DISCONNECTING) {
                cancelNotification()
                CallSession.clear()
            }
        }
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        call.registerCallback(callback)
        CallSession.bind(call, this)
        val details = call.details
        val number = details?.handle?.schemeSpecificPart ?: ""
        val name = details?.callerDisplayName?.toString()?.ifBlank { null } ?: number.ifBlank { "Unknown" }
        CallSession.setIdentity(number, name)
        showIncomingUi(number, name, call.state)
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        call.unregisterCallback(callback)
        cancelNotification()
        CallSession.clear()
    }

    private fun showIncomingUi(number: String, name: String, state: Int) {
        val intent = Intent(this, PhoneCallActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(PhoneCallActivity.EXTRA_NUMBER, number)
            putExtra(PhoneCallActivity.EXTRA_NAME, name)
        }
        startActivity(intent)
        if (state == Call.STATE_RINGING) {
            postFullScreenNotification(number, name)

        // Custom ringtone if set
        try {
            val prefs = PhonePrefs(this)
            val uriStr = prefs.ringtoneUri
            if (uriStr != null && state == Call.STATE_RINGING) {
                val r = android.media.RingtoneManager.getRingtone(this, android.net.Uri.parse(uriStr))
                r?.play()
            }
        } catch (_: Exception) {}

        }
    }

    private fun postFullScreenNotification(number: String, name: String) {
        val nm = getSystemService(NotificationManager::class.java) ?: return
        val channelId = "suhood_incoming_call"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                channelId,
                "Incoming calls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Full-screen incoming call UI"
                setSound(null, null)
                enableVibration(true)
            }
            nm.createNotificationChannel(ch)
        }
        val fullScreen = PendingIntent.getActivity(
            this, 1,
            Intent(this, PhoneCallActivity::class.java).apply {
                putExtra(PhoneCallActivity.EXTRA_NUMBER, number)
                putExtra(PhoneCallActivity.EXTRA_NAME, name)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notif = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(name)
            .setContentText(number.ifBlank { "Incoming call" })
            .setCategory(Notification.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setFullScreenIntent(fullScreen, true)
            .setContentIntent(fullScreen)
            .build()
        nm.notify(NOTIF_ID, notif)
    }

    private fun cancelNotification() {
        getSystemService(NotificationManager::class.java)?.cancel(NOTIF_ID)
    }

    companion object {
        private const val NOTIF_ID = 71001
    }
}

object CallSession {
    private val _state = MutableStateFlow(Call.STATE_NEW)
    val state: StateFlow<Int> = _state.asStateFlow()

    private val _number = MutableStateFlow("")
    val number: StateFlow<String> = _number.asStateFlow()

    private val _displayName = MutableStateFlow("")
    val displayName: StateFlow<String> = _displayName.asStateFlow()

    @Volatile private var call: Call? = null
    @Volatile private var service: InCallService? = null

    fun bind(c: Call, svc: InCallService) {
        call = c
        service = svc
        _state.value = c.state
    }

    fun setIdentity(number: String, name: String) {
        _number.value = number
        _displayName.value = name
    }

    fun onState(state: Int) {
        _state.value = state
    }

    fun clear() {
        call = null
        service = null
        _state.value = Call.STATE_NEW
        _number.value = ""
        _displayName.value = ""
    }

    fun answer() {
        call?.answer(VideoProfile.STATE_AUDIO_ONLY)
    }

    fun reject() {
        call?.reject(false, null)
    }

    fun disconnect() {
        call?.disconnect()
    }

    fun hold() {
        call?.hold()
    }

    fun unhold() {
        call?.unhold()
    }

    fun setMuted(muted: Boolean) {
        service?.setMuted(muted)
    }

    fun setSpeaker(on: Boolean) {
        val svc = service ?: return
        try {
            if (on) {
                svc.setAudioRoute(android.telecom.CallAudioState.ROUTE_SPEAKER)
            } else {
                svc.setAudioRoute(android.telecom.CallAudioState.ROUTE_EARPIECE)
            }
        } catch (_: Exception) {
        }
    }
}

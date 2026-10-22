package com.suhood.feature.phone

import android.content.Intent
import android.telecom.Call
import android.telecom.InCallService
import android.telecom.VideoProfile

/**
 * System InCallService UI bridge. Only active when Suhood Phone is the default dialer.
 * Surfaces the current [Call] to [CallSession] and opens [PhoneCallActivity].
 */
class SuhoodInCallService : InCallService() {

    private val callback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            CallSession.onState(state)
            if (state == Call.STATE_DISCONNECTED || state == Call.STATE_DISCONNECTING) {
                CallSession.clear()
            }
        }
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        call.registerCallback(callback)
        CallSession.bind(call, this)
        val i = Intent(this, PhoneCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        }
        startActivity(i)
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        call.unregisterCallback(callback)
        if (CallSession.currentCall === call) CallSession.clear()
    }
}

object CallSession {
    @Volatile var currentCall: Call? = null
        private set
    @Volatile var service: InCallService? = null
        private set
    @Volatile var state: Int = Call.STATE_NEW
        private set

    fun bind(call: Call, svc: InCallService) {
        currentCall = call
        service = svc
        state = call.state
    }

    fun clear() {
        currentCall = null
        service = null
        state = Call.STATE_NEW
    }

    fun onState(s: Int) { state = s }

    fun answer() {
        currentCall?.answer(VideoProfile.STATE_AUDIO_ONLY)
    }

    fun reject() {
        currentCall?.reject(false, null)
    }

    fun disconnect() {
        currentCall?.disconnect()
    }

    fun hold() {
        currentCall?.hold()
    }

    fun unhold() {
        currentCall?.unhold()
    }

    fun setMuted(muted: Boolean) {
        service?.setMuted(muted)
    }

    fun setSpeaker(on: Boolean) {
        // Audio routing via InCallService setAudioRoute when available
        runCatching {
            val svc = service ?: return
            val route = if (on) android.telecom.CallAudioState.ROUTE_SPEAKER
            else android.telecom.CallAudioState.ROUTE_EARPIECE
            svc.setAudioRoute(route)
        }
    }
}

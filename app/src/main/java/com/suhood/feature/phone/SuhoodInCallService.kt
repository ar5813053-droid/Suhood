package com.suhood.feature.phone

import android.content.Intent
import android.telecom.Call
import android.telecom.InCallService

class SuhoodInCallService : InCallService() {
    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        CallSession.bind(call, this)
        startActivity(Intent(this, PhoneCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("state", call.state)
            putExtra("number", call.details?.handle?.schemeSpecificPart.orEmpty())
        })
    }
    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        CallSession.unbind(call)
    }
}

object CallSession {
    @Volatile var active: Call? = null; private set
    @Volatile var service: InCallService? = null; private set
    fun bind(call: Call, svc: InCallService) { active = call; service = svc }
    fun unbind(call: Call) { if (active == call) { active = null; service = null } }
    fun answer() { active?.answer(0) }
    fun reject() { active?.reject(false, null) }
    fun disconnect() { active?.disconnect() }
}

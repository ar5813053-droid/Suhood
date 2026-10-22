package com.suhood.feature.phone

import android.telecom.Call
import android.telecom.CallScreeningService

class SuhoodCallScreeningService : CallScreeningService() {
    override fun onScreenCall(callDetails: Call.Details) {
        val number = callDetails.handle?.schemeSpecificPart.orEmpty()
        val blocked = PhonePrefs(this).isBlocked(number)
        respondToCall(
            callDetails,
            CallResponse.Builder()
                .setDisallowCall(blocked)
                .setRejectCall(blocked)
                .setSkipNotification(blocked)
                .setSkipCallLog(false)
                .build()
        )
    }
}

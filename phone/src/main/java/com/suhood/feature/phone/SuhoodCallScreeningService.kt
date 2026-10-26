package com.suhood.feature.phone

import android.telecom.Call
import android.telecom.CallScreeningService

/**
 * Blocks numbers on the local blocklist. During quiet hours, silences (does not reject)
 * unknown callers when quiet mode is enabled — never claims this is full spam AI.
 */
class SuhoodCallScreeningService : CallScreeningService() {
    override fun onScreenCall(callDetails: Call.Details) {
        val number = callDetails.handle?.schemeSpecificPart.orEmpty()
        val prefs = PhonePrefs(this)
        val response = when {
            prefs.isBlocked(number) ->
                CallResponse.Builder()
                    .setDisallowCall(true)
                    .setRejectCall(true)
                    .setSkipNotification(true)
                    .setSkipCallLog(false)
                    .build()
            prefs.isQuietNow() && number.isNotBlank() &&
                callDetails.callDirection == Call.Details.DIRECTION_INCOMING ->
                CallResponse.Builder()
                    .setSilenceCall(true)
                    .build()
            else -> CallResponse.Builder().build()
        }
        respondToCall(callDetails, response)
    }
}

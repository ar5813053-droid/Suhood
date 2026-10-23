package com.suhood.feature.phone

import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.telecom.Call
import android.telecom.InCallService
import android.telecom.VideoProfile
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.suhood.app.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * System InCallService — only active when Suhood is the default dialer.
 *
 * RINGING meta-data is true, so this service owns ringtone playback.
 * Shows full-screen UI on lock/idle, heads-up notification + optional
 * floating card when the user is in another app.
 */
class SuhoodInCallService : InCallService() {

    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null

    private val callback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            CallSession.onState(state)
            when (state) {
                Call.STATE_ACTIVE, Call.STATE_DIALING, Call.STATE_CONNECTING -> {
                    stopRinging()
                    // Refresh ongoing notification without full-screen
                    postOngoingNotification(
                        CallSession.number.value,
                        CallSession.displayName.value
                    )
                }
                Call.STATE_DISCONNECTED, Call.STATE_DISCONNECTING -> {
                    stopRinging()
                    cancelNotification()
                    CallSession.clear()
                }
                Call.STATE_RINGING -> {
                    // already handled in onCallAdded
                }
            }
        }
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        call.registerCallback(callback)
        CallSession.bind(call, this)
        val details = call.details
        val number = details?.handle?.schemeSpecificPart ?: ""
        // Prefer device contact name over network CNAM
        val contact = resolveContactName(number)
        val cnam = details?.callerDisplayName?.toString()?.takeIf { it.isNotBlank() && !it.all { ch -> ch.isDigit() || ch in "+- ()" } }
        val name = contact ?: cnam ?: number.ifBlank { "Unknown" }
        CallSession.setIdentity(number, name)

        when (call.state) {
            Call.STATE_RINGING -> handleIncoming(number, name)
            Call.STATE_DIALING, Call.STATE_CONNECTING, Call.STATE_ACTIVE -> {
                // Stay as floating overlay when already in another app; full when locked handled elsewhere
                openCallActivity(number, name, forceFullScreen = false)
                postOngoingNotification(number, name)
            }
            else -> openCallActivity(number, name, forceFullScreen = false)
        }
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        call.unregisterCallback(callback)
        stopRinging()
        cancelNotification()
        CallSession.clear()
    }

    override fun onDestroy() {
        stopRinging()
        super.onDestroy()
    }

    private fun handleIncoming(number: String, name: String) {
        val lockedOrIdle = isDeviceLockedOrIdle()
        startRinging()
        postIncomingNotification(number, name, fullScreen = lockedOrIdle)
        if (lockedOrIdle) {
            openCallActivity(number, name, forceFullScreen = true)
        } else {
            // User is in another app — heads-up notification is primary;
            // also try a lightweight floating activity if overlay allowed.
            openCallActivity(number, name, forceFullScreen = false)
        }
    }

    private fun isDeviceLockedOrIdle(): Boolean {
        val km = getSystemService(KeyguardManager::class.java)
        if (km?.isKeyguardLocked == true) return true
        val pm = getSystemService(PowerManager::class.java)
        if (pm?.isInteractive == false) return true
        return false
    }

    private fun openCallActivity(number: String, name: String, forceFullScreen: Boolean) {
        val intent = Intent(this, PhoneCallActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_NO_USER_ACTION
            )
            putExtra(PhoneCallActivity.EXTRA_NUMBER, number)
            putExtra(PhoneCallActivity.EXTRA_NAME, name)
            putExtra(PhoneCallActivity.EXTRA_HEADS_UP, !forceFullScreen)
        }
        try {
            startActivity(intent)
        } catch (_: Exception) {
            // Background start may be blocked; full-screen notification is the fallback
        }
    }

    private fun startRinging() {
        stopRinging()
        val prefs = PhonePrefs(this)
        if (prefs.isQuietNow()) return

        try {
            val uri = prefs.ringtoneUri?.let { android.net.Uri.parse(it) }
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            val r = RingtoneManager.getRingtone(this, uri) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                r.isLooping = true
            }
            r.audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            r.play()
            ringtone = r
        } catch (_: Exception) {
        }

        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                getSystemService(VibratorManager::class.java)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(VIBRATOR_SERVICE) as? Vibrator
            }
            val pattern = longArrayOf(0, 800, 400, 800, 400)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (_: Exception) {
        }
    }

    private fun stopRinging() {
        try {
            ringtone?.stop()
        } catch (_: Exception) {
        }
        ringtone = null
        try {
            vibrator?.cancel()
        } catch (_: Exception) {
        }
        vibrator = null
    }

    private fun ensureChannel(fullScreen: Boolean): String {
        val nm = getSystemService(NotificationManager::class.java) ?: return CHANNEL_INCOMING
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = CHANNEL_INCOMING
            val existing = nm.getNotificationChannel(channelId)
            if (existing == null) {
                val ch = NotificationChannel(
                    channelId,
                    "Incoming calls",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Incoming call alerts"
                    // Ringtone is played by InCallService; avoid double sound
                    setSound(null, null)
                    enableVibration(false)
                    setBypassDnd(true)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
                nm.createNotificationChannel(ch)
            }
            if (nm.getNotificationChannel(CHANNEL_ONGOING) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ONGOING,
                        "Ongoing calls",
                        NotificationManager.IMPORTANCE_DEFAULT
                    ).apply {
                        setSound(null, null)
                        enableVibration(false)
                    }
                )
            }
        }
        return CHANNEL_INCOMING
    }

    private fun postIncomingNotification(number: String, name: String, fullScreen: Boolean) {
        val nm = getSystemService(NotificationManager::class.java) ?: return
        val channelId = ensureChannel(fullScreen)

        val answerPi = PendingIntent.getBroadcast(
            this, 11,
            Intent(this, IncomingCallActionReceiver::class.java).setAction(IncomingCallActionReceiver.ACTION_ANSWER),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val declinePi = PendingIntent.getBroadcast(
            this, 12,
            Intent(this, IncomingCallActionReceiver::class.java).setAction(IncomingCallActionReceiver.ACTION_DECLINE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val openPi = PendingIntent.getActivity(
            this, 13,
            Intent(this, PhoneCallActivity::class.java).apply {
                putExtra(PhoneCallActivity.EXTRA_NUMBER, number)
                putExtra(PhoneCallActivity.EXTRA_NAME, name)
                putExtra(PhoneCallActivity.EXTRA_HEADS_UP, !fullScreen)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(name)
            .setContentText(number.ifBlank { "Incoming call" })
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openPi)
            .addAction(0, "Decline", declinePi)
            .addAction(0, "Answer", answerPi)
            .setStyle(
                NotificationCompat.CallStyle.forIncomingCall(
                    person(name),
                    declinePi,
                    answerPi
                )
            )

        if (fullScreen) {
            builder.setFullScreenIntent(openPi, true)
        }

        try {
            nm.notify(NOTIF_ID, builder.build())
        } catch (_: Exception) {
            // CallStyle may fail on older devices — fallback without style
            val fallback = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(name)
                .setContentText(number.ifBlank { "Incoming call" })
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setOngoing(true)
                .setContentIntent(openPi)
                .addAction(0, "Decline", declinePi)
                .addAction(0, "Answer", answerPi)
                .apply { if (fullScreen) setFullScreenIntent(openPi, true) }
                .build()
            nm.notify(NOTIF_ID, fallback)
        }
    }

    private fun postOngoingNotification(number: String, name: String) {
        val nm = getSystemService(NotificationManager::class.java) ?: return
        ensureChannel(false)
        val hangup = PendingIntent.getBroadcast(
            this, 14,
            Intent(this, IncomingCallActionReceiver::class.java).setAction(IncomingCallActionReceiver.ACTION_HANGUP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val openPi = PendingIntent.getActivity(
            this, 15,
            Intent(this, PhoneCallActivity::class.java).apply {
                putExtra(PhoneCallActivity.EXTRA_NUMBER, number)
                putExtra(PhoneCallActivity.EXTRA_NAME, name)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notif = NotificationCompat.Builder(this, CHANNEL_ONGOING)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(name.ifBlank { "Call in progress" })
            .setContentText(number.ifBlank { "Ongoing call" })
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setOngoing(true)
            .setContentIntent(openPi)
            .addAction(0, "End", hangup)
            .build()
        nm.notify(NOTIF_ID, notif)
    }

    private fun person(name: String): androidx.core.app.Person {
        return androidx.core.app.Person.Builder().setName(name).setImportant(true).build()
    }

    private fun cancelNotification() {
        getSystemService(NotificationManager::class.java)?.cancel(NOTIF_ID)
    }

    private fun resolveContactName(number: String): String? {
        if (number.isBlank()) return null
        return try {
            val uri = android.net.Uri.withAppendedPath(
                android.provider.ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                android.net.Uri.encode(number)
            )
            contentResolver.query(
                uri,
                arrayOf(android.provider.ContactsContract.PhoneLookup.DISPLAY_NAME),
                null, null, null
            )?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private const val NOTIF_ID = 71001
        private const val CHANNEL_INCOMING = "suhood_incoming_call_v2"
        private const val CHANNEL_ONGOING = "suhood_ongoing_call_v2"

        fun stopRingingStatic(context: Context) {
            // Best-effort: vibration cancel
            try {
                val v = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.getSystemService(VibratorManager::class.java)?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(VIBRATOR_SERVICE) as? Vibrator
                }
                v?.cancel()
            } catch (_: Exception) {
            }
        }
    }
}

/**
 * Handles Answer / Decline / Hangup from the notification action buttons.
 */
class IncomingCallActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            ACTION_ANSWER -> {
                CallSession.answer()
                SuhoodInCallService.stopRingingStatic(context)
                context.startActivity(
                    Intent(context, PhoneCallActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        putExtra(PhoneCallActivity.EXTRA_HEADS_UP, true)
                    }
                )
            }
            ACTION_DECLINE -> {
                CallSession.reject()
                SuhoodInCallService.stopRingingStatic(context)
            }
            ACTION_HANGUP -> {
                CallSession.disconnect()
                SuhoodInCallService.stopRingingStatic(context)
            }
        }
    }

    companion object {
        const val ACTION_ANSWER = "com.suhood.phone.ACTION_ANSWER"
        const val ACTION_DECLINE = "com.suhood.phone.ACTION_DECLINE"
        const val ACTION_HANGUP = "com.suhood.phone.ACTION_HANGUP"
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
        try {
            call?.answer(VideoProfile.STATE_AUDIO_ONLY)
        } catch (_: Exception) {
        }
    }

    fun reject() {
        val c = call ?: return
        try {
            @Suppress("DEPRECATION")
            c.reject(false, null)
        } catch (_: Exception) {
            try {
                c.disconnect()
            } catch (_: Exception) {
            }
        }
    }

    fun disconnect() {
        try {
            call?.disconnect()
        } catch (_: Exception) {
        }
    }

    fun hold() {
        try {
            call?.hold()
        } catch (_: Exception) {
        }
    }

    fun unhold() {
        try {
            call?.unhold()
        } catch (_: Exception) {
        }
    }

    fun setMuted(muted: Boolean) {
        try {
            service?.setMuted(muted)
        } catch (_: Exception) {
        }
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

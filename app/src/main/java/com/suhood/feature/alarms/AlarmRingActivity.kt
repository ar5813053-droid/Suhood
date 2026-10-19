package com.suhood.feature.alarms

import android.app.KeyguardManager
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier.Modifier
import androidx.compose.ui.unit.dp
import com.suhood.core.designsystem.components.GlassButton
import com.suhood.core.designsystem.theme.SuhoodTheme
import com.suhood.core.designsystem.theme.SuhoodTypography
import com.suhood.core.designsystem.theme.ThemeMode
import com.suhood.core.designsystem.theme.suhoodColors
import com.suhood.core.notifications.AlarmReceiver
import com.suhood.core.notifications.AlarmScheduler

/**
 * Full-screen alarm UI when Android allows full-screen intents.
 * Uses a dedicated MediaPlayer so Nasheedify's ExoPlayer is not corrupted.
 */
class AlarmRingActivity : ComponentActivity() {

    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val km = getSystemService(KeyguardManager::class.java)
            km?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        val label = intent.getStringExtra(AlarmReceiver.EXTRA_LABEL) ?: "Alarm"
        val alarmId = intent.getStringExtra(AlarmReceiver.EXTRA_ALARM_ID) ?: "alarm"
        val soundUri = intent.getStringExtra(EXTRA_SOUND_URI)

        startSound(soundUri)
        startVibrate()

        setContent {
            SuhoodTheme(themeMode = ThemeMode.DARK) {
                val colors = suhoodColors
                Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(label, style = SuhoodTypography.headlineLarge, color = colors.textPrimary)
                    Spacer(Modifier.height(8.dp))
                    Text("Suhood alarm", style = SuhoodTypography.bodyMedium, color = colors.textSecondary)
                    Spacer(Modifier.height(32.dp))
                    GlassButton(
                        text = "Dismiss",
                        onClick = { stopAll(); finish() },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    GlassButton(
                        text = "Snooze 5 min",
                        onClick = {
                            stopAll()
                            AlarmScheduler.scheduleSnooze(this@AlarmRingActivity, alarmId, label, 5)
                            finish()
                        },
                        primary = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    private fun startSound(uriString: String?) {
        runCatching {
            val uri = uriString?.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(this@AlarmRingActivity, uri)
                isLooping = true
                prepare()
                start()
            }
        }
    }

    private fun startVibrate() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        vibrator?.let { v ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 500), 0))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(longArrayOf(0, 500, 500), 0)
            }
        }
    }

    private fun stopAll() {
        runCatching { player?.stop(); player?.release() }
        player = null
        vibrator?.cancel()
    }

    override fun onDestroy() {
        stopAll()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_SOUND_URI = "sound_uri"
    }
}

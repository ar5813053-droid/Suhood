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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import com.suhood.core.designsystem.components.GlassButton
import com.suhood.core.designsystem.components.GlassSearchField
import com.suhood.core.designsystem.theme.SuhoodTheme
import com.suhood.core.designsystem.theme.SuhoodTypography
import com.suhood.core.designsystem.theme.ThemeMode
import com.suhood.core.designsystem.theme.suhoodColors
import com.suhood.core.notifications.AlarmReceiver
import com.suhood.core.notifications.AlarmScheduler
import kotlin.random.Random

/**
 * Full-screen alarm ring UI with optional math challenge before dismiss.
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
        val challenge = intent.getStringExtra(EXTRA_CHALLENGE) ?: "NONE"

        startSound(soundUri)
        startVibrate()

        setContent {
            SuhoodTheme(themeMode = ThemeMode.DARK) {
                val colors = suhoodColors
                var solved by remember { mutableStateOf(challenge == "NONE") }
                val a = remember { Random.nextInt(2, 12) }
                val b = remember { Random.nextInt(2, 12) }
                val answer = a + b
                var input by remember { mutableStateOf("") }
                var error by remember { mutableStateOf<String?>(null) }
                var tapsNeeded by remember { mutableIntStateOf(5) }
                var taps by remember { mutableIntStateOf(0) }

                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(label, style = SuhoodTypography.headlineMedium, color = colors.textPrimary)
                    Spacer(Modifier.height(8.dp))
                    Text("Suhood alarm", style = SuhoodTypography.bodyMedium, color = colors.textMuted)
                    Spacer(Modifier.height(24.dp))

                    when {
                        challenge == "MATH" && !solved -> {
                            Text("Solve to dismiss", style = SuhoodTypography.titleMedium, color = colors.textSecondary)
                            Spacer(Modifier.height(8.dp))
                            Text("$a + $b = ?", style = SuhoodTypography.headlineLarge, color = colors.textPrimary)
                            Spacer(Modifier.height(12.dp))
                            GlassSearchField(query = input, onQueryChange = { input = it }, placeholder = "Answer")
                            error?.let {
                                Spacer(Modifier.height(8.dp))
                                Text(it, color = colors.textMuted)
                            }
                            Spacer(Modifier.height(12.dp))
                            GlassButton(
                                text = "Check",
                                onClick = {
                                    if (input.trim().toIntOrNull() == answer) {
                                        solved = true
                                        error = null
                                    } else error = "Incorrect — try again"
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        challenge == "TAP" && !solved -> {
                            Text("Tap $tapsNeeded times to dismiss", style = SuhoodTypography.titleMedium, color = colors.textSecondary)
                            Spacer(Modifier.height(12.dp))
                            GlassButton(
                                text = "Tap (${taps}/$tapsNeeded)",
                                onClick = {
                                    taps++
                                    if (taps >= tapsNeeded) solved = true
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        challenge == "MEMORY" && !solved -> {
                            // Lightweight: same as math for reliability offline
                            Text("Solve to dismiss", style = SuhoodTypography.titleMedium, color = colors.textSecondary)
                            Spacer(Modifier.height(8.dp))
                            Text("$a + $b = ?", style = SuhoodTypography.headlineLarge, color = colors.textPrimary)
                            Spacer(Modifier.height(12.dp))
                            GlassSearchField(query = input, onQueryChange = { input = it }, placeholder = "Answer")
                            Spacer(Modifier.height(12.dp))
                            GlassButton(
                                text = "Check",
                                onClick = {
                                    if (input.trim().toIntOrNull() == answer) solved = true
                                    else error = "Incorrect"
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            error?.let { Text(it, color = colors.textMuted) }
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                    if (solved || challenge == "NONE") {
                        GlassButton(
                            text = "Dismiss",
                            onClick = {
                                stopAll()
                                NotificationManagerCompat.from(this@AlarmRingActivity)
                                    .cancel(alarmId.hashCode())
                                finish()
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    GlassButton(
                        text = "Snooze 5 min",
                        onClick = {
                            stopAll()
                            NotificationManagerCompat.from(this@AlarmRingActivity)
                                .cancel(alarmId.hashCode())
                            AlarmScheduler.scheduleSnooze(
                                this@AlarmRingActivity, alarmId, label, 5, soundUri.orEmpty(), challenge
                            )
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
            val uri = uriString?.takeIf { it.isNotBlank() && it.contains("://") }?.let { Uri.parse(it) }
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
        }.onFailure {
            // fallback default
            runCatching {
                val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                player = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build()
                    )
                    setDataSource(this@AlarmRingActivity, uri)
                    isLooping = true
                    prepare()
                    start()
                }
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
        const val EXTRA_CHALLENGE = "challenge"
    }
}

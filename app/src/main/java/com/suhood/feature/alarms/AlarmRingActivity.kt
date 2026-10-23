package com.suhood.feature.alarms

import android.app.KeyguardManager
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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import com.suhood.core.designsystem.theme.SuhoodTheme
import com.suhood.core.notifications.AlarmReceiver
import com.suhood.core.notifications.AlarmScheduler
import com.suhood.core.designsystem.theme.ThemeMode
import com.suhood.core.designsystem.theme.suhoodColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Random
import kotlin.math.roundToInt

class AlarmRingActivity : ComponentActivity() {
    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            getSystemService(KeyguardManager::class.java)?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val label = intent.getStringExtra(AlarmReceiver.EXTRA_LABEL) ?: "Alarm"
        val alarmId = intent.getStringExtra(AlarmReceiver.EXTRA_ALARM_ID) ?: "alarm"
        val soundUri = intent.getStringExtra(EXTRA_SOUND_URI)
        val challenge = intent.getStringExtra(EXTRA_CHALLENGE) ?: "NONE"

        startSound(soundUri)
        startVibrate()

        setContent {
            SuhoodTheme(themeMode = ThemeMode.DARK) {
                AlarmRingScreen(
                    label = label,
                    challenge = challenge,
                    onSnooze = {
                        stopAll()
                        NotificationManagerCompat.from(this).cancel(alarmId.hashCode())
                        AlarmScheduler.scheduleSnooze(
                            this, alarmId, label, 5, soundUri.orEmpty(), challenge
                        )
                        finish()
                    },
                    onStop = {
                        stopAll()
                        NotificationManagerCompat.from(this).cancel(alarmId.hashCode())
                        finish()
                    }
                )
            }
        }
    }

    private fun stopAll() {
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
        runCatching { vibrator?.cancel() }
    }

    private fun startSound(soundUri: String?) {
        runCatching {
            player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                val uri = soundUri?.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                setDataSource(this@AlarmRingActivity, uri)
                prepare()
                start()
            }
        }.onFailure {
            runCatching {
                player = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    isLooping = true
                    setDataSource(
                        this@AlarmRingActivity,
                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    )
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
            getSystemService(VIBRATOR_SERVICE) as? Vibrator
        }
        val pattern = longArrayOf(0, 600, 400, 600, 400, 600)
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        }
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

@Composable
private fun AlarmRingScreen(
    label: String,
    challenge: String,
    onSnooze: () -> Unit,
    onStop: () -> Unit
) {
    var phase by remember { mutableStateOf("ringing") }
    var clock by remember { mutableStateOf(currentTime()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            clock = currentTime()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        if (phase == "challenge") {
            DismissChallenge(challenge = challenge, onSolved = onStop)
        } else {
            Column(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Filled.Alarm, null, tint = Color(0xFF8E8E93), modifier = Modifier.size(28.dp))
                Spacer(Modifier.height(8.dp))
                Text("Alarm", color = Color(0xFF8E8E93), fontSize = 20.sp, fontWeight = FontWeight.Medium)
                if (label.isNotBlank() && label != "Alarm") {
                    Spacer(Modifier.height(6.dp))
                    Text(label, color = Color(0xFFAEAEB2), fontSize = 15.sp)
                }
            }

            Text(
                clock,
                color = Color.White,
                fontSize = 96.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = (-2).sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center)
            )

            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 28.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(10.dp, RoundedCornerShape(28.dp))
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color(0xFFFF9F0A))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onSnooze
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Snooze", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(Modifier.height(22.dp))

                LiquidSlideToStop(
                    onCompleted = {
                        if (challenge != "NONE") phase = "challenge"
                        else onStop()
                    }
                )
            }
        }
    }
}

@Composable
private fun LiquidSlideToStop(onCompleted: () -> Unit) {
    val density = LocalDensity.current
    var trackW by remember { mutableIntStateOf(1) }
    val slide = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val knob = 56.dp
    val knobPx = with(density) { knob.toPx() }
    val maxSlide = (trackW - knobPx - with(density) { 8.dp.toPx() } * 2).coerceAtLeast(1f)
    val threshold = maxSlide * 0.82f

    Box(
        Modifier
            .fillMaxWidth()
            .height(64.dp)
            .onSizeChanged { trackW = it.width }
            .shadow(12.dp, RoundedCornerShape(32.dp))
            .clip(RoundedCornerShape(32.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF3A3A3C), Color(0xFF1C1C1E))))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(32.dp))
    ) {
        Text(
            "slide to stop",
            color = Color.White.copy(alpha = 0.45f),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.align(Alignment.Center)
        )
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .padding(start = 4.dp)
                .offset { IntOffset(slide.value.roundToInt(), 0) }
                .size(knob)
                .graphicsLayer {
                    shadowElevation = 10f
                    shape = CircleShape
                    clip = true
                }
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color(0xFFF5F5F7), Color(0xFFD1D1D6))))
                .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                .pointerInput(maxSlide) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                if (slide.value >= threshold) {
                                    slide.animateTo(maxSlide, spring(stiffness = Spring.StiffnessMedium))
                                    onCompleted()
                                } else {
                                    slide.animateTo(
                                        0f,
                                        spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow)
                                    )
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch {
                                slide.animateTo(
                                    0f,
                                    spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow)
                                )
                            }
                        },
                        onHorizontalDrag = { _, dx ->
                            scope.launch { slide.snapTo((slide.value + dx).coerceIn(0f, maxSlide)) }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .size(18.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF3A3A3C))
            )
        }
    }
}

@Composable
private fun DismissChallenge(challenge: String, onSolved: () -> Unit) {
    val a = remember { Random().nextInt(10) + 2 }
    val b = remember { Random().nextInt(10) + 2 }
    var input by remember { mutableStateOf("") }
    var taps by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Dismiss challenge", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(20.dp))
        when (challenge) {
            "MATH" -> {
                Text("$a + $b = ?", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Light)
                Spacer(Modifier.height(16.dp))
                TextField(
                    value = input,
                    onValueChange = { input = it.filter { ch -> ch.isDigit() }.take(3) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF2C2C2E),
                        unfocusedContainerColor = Color(0xFF2C2C2E)
                    )
                )
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = {
                    if (input.toIntOrNull() == a + b) onSolved() else error = "Try again"
                }) { Text("Confirm", color = Color(0xFFFF9F0A)) }
            }
            "TAPS" -> {
                Text("Tap 5 times", color = Color.White, fontSize = 20.sp)
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = {
                    taps++
                    if (taps >= 5) onSolved()
                }) { Text("Tap ($taps/5)", color = Color(0xFFFF9F0A), fontSize = 18.sp) }
            }
            else -> TextButton(onClick = onSolved) { Text("Stop alarm", color = Color(0xFFFF9F0A)) }
        }
        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = Color(0xFFFF453A))
        }
    }
}

private fun currentTime(): String =
    SimpleDateFormat("h:mm", Locale.getDefault()).format(Date())

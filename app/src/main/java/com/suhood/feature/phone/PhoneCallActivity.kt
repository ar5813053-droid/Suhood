package com.suhood.feature.phone

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.telecom.Call
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.suhood.core.designsystem.theme.SuhoodTheme
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

class PhoneCallActivity : ComponentActivity() {
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
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val preview = intent.getBooleanExtra(EXTRA_PREVIEW, false)
        val previewNumber = intent.getStringExtra(EXTRA_NUMBER) ?: "Unknown"
        val previewName = intent.getStringExtra(EXTRA_NAME) ?: "Incoming call"
        val forceTheme = intent.getStringExtra(EXTRA_THEME)

        // Vibrate on incoming even in silent mode (app-driven vibration)
        val state = CallSession.state.value
        if (preview || state == Call.STATE_RINGING) {
            startIncomingVibrate()
        }

        setContent {
            SuhoodTheme {
                CallUiScreen(
                    preview = preview,
                    previewNumber = previewNumber,
                    previewName = previewName,
                    forceThemeName = forceTheme,
                    onFinish = {
                        stopIncomingVibrate()
                        finish()
                    },
                    onAnswered = { stopIncomingVibrate() }
                )
            }
        }
    }

    private fun startIncomingVibrate() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as? Vibrator
        }
        val pattern = longArrayOf(0, 500, 400, 500, 400, 500)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (_: Exception) {
        }
    }

    private fun stopIncomingVibrate() {
        try {
            vibrator?.cancel()
        } catch (_: Exception) {
        }
    }

    override fun onDestroy() {
        stopIncomingVibrate()
        super.onDestroy()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_POWER || keyCode == KeyEvent.KEYCODE_ENDCALL) {
            if (CallSession.state.value == Call.STATE_RINGING || intent.getBooleanExtra(EXTRA_PREVIEW, false)) {
                if (CallSession.state.value == Call.STATE_RINGING) CallSession.reject()
                stopIncomingVibrate()
                finish()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    companion object {
        const val EXTRA_PREVIEW = "preview"
        const val EXTRA_NUMBER = "number"
        const val EXTRA_NAME = "name"
        const val EXTRA_THEME = "theme"

        fun previewIntent(
            context: Context,
            theme: CallThemeId? = null,
            name: String = "Incoming (Preview)",
            number: String = "+91 98765 43210"
        ) = android.content.Intent(context, PhoneCallActivity::class.java).apply {
            putExtra(EXTRA_PREVIEW, true)
            putExtra(EXTRA_NAME, name)
            putExtra(EXTRA_NUMBER, number)
            if (theme != null) putExtra(EXTRA_THEME, theme.name)
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}

@Composable
fun CallUiScreen(
    preview: Boolean,
    previewNumber: String,
    previewName: String,
    forceThemeName: String?,
    onFinish: () -> Unit,
    onAnswered: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember { PhonePrefs(context) }
    val state by CallSession.state.collectAsState()
    val number by CallSession.number.collectAsState()
    val name by CallSession.displayName.collectAsState()

    val displayNumber = if (preview) previewNumber else number.ifBlank { previewNumber }
    val displayName = if (preview) previewName else name.ifBlank { displayNumber.ifBlank { "Unknown" } }
    val emoji = prefs.contactEmoji(displayNumber)

    val themeId = remember(forceThemeName, displayNumber, state) {
        forceThemeName?.let { runCatching { CallThemeId.valueOf(it) }.getOrNull() }
            ?: prefs.contactTheme(displayNumber)
            ?: if (state == Call.STATE_RINGING || preview) prefs.incomingTheme else prefs.ongoingTheme
    }
    val theme = CallThemes.colors(themeId)
    val assetWp = prefs.wallpaperAsset

    var muted by remember { mutableStateOf(false) }
    var onHold by remember { mutableStateOf(false) }
    var speaker by remember { mutableStateOf(false) }
    var showKeypad by remember { mutableStateOf(false) }
    var slidePx by remember { mutableFloatStateOf(0f) }
    var trackWidthPx by remember { mutableFloatStateOf(1f) }
    var elapsed by remember { mutableIntStateOf(0) }

    val ringing = preview || state == Call.STATE_RINGING
    val density = LocalDensity.current
    val knobSizePx = with(density) { 56.dp.toPx() }
    val maxSlide = (trackWidthPx - knobSizePx - with(density) { 12.dp.toPx() }).coerceAtLeast(1f)
    // Must reach ~95% of the track before release counts as answer
    val answerThreshold = maxSlide * 0.95f

    // Liquid nudge animation when not dragging
    val infinite = rememberInfiniteTransition(label = "slideNudge")
    val nudge by infinite.animateFloat(
        initialValue = 0f,
        targetValue = with(density) { 14.dp.toPx() },
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "nudge"
    )
    var dragging by remember { mutableStateOf(false) }
    val displaySlide = if (dragging) slidePx else (slidePx + nudge).coerceIn(0f, maxSlide)

    LaunchedEffect(state, preview) {
        if (!preview && state == Call.STATE_ACTIVE) {
            elapsed = 0
            while (true) {
                delay(1000)
                elapsed++
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF1C1C1E))) {
        if (assetWp != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data("file:///android_asset/phone_wallpapers/$assetWp")
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            theme.wallpaperRes?.let { res ->
                Image(
                    painter = painterResource(res),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } ?: Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color(0xFF3A3A3C), Color(0xFF1C1C1E), Color(0xFF000000)))
                )
            )
        }
        // Soft top + strong bottom black gradient (iPhone-like)
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color(0x44000000),
                    0.45f to Color(0x22000000),
                    0.75f to Color(0x99000000),
                    1f to Color(0xEE000000)
                )
            )
        )

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp)
                .padding(top = 48.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (preview) {
                Text("PREVIEW", color = Color(0xFFFFD60A), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
            }
            Text(
                when {
                    ringing -> ""
                    state == Call.STATE_DIALING || state == Call.STATE_CONNECTING -> "Calling…"
                    state == Call.STATE_HOLDING -> "On hold"
                    else -> String.format("%02d:%02d", elapsed / 60, elapsed % 60)
                },
                color = Color(0x99FFFFFF),
                fontSize = 15.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                displayName,
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2
            )
            if (!ringing) {
                Spacer(Modifier.height(6.dp))
                Text(
                    PhoneNumberUtils.display(
                        PhoneNumberUtils.normalize(displayNumber).ifBlank { displayNumber },
                        prefs.numeralStyle
                    ),
                    color = Color(0x99FFFFFF),
                    fontSize = 16.sp
                )
            }
            Spacer(Modifier.height(28.dp))
            Box(
                Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .background(Color(0x33FFFFFF)),
                contentAlignment = Alignment.Center
            ) {
                if (!emoji.isNullOrBlank()) {
                    Text(emoji, fontSize = 72.sp)
                } else {
                    Text(
                        displayName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        color = Color.White,
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Light
                    )
                }
            }
            Spacer(Modifier.weight(1f))

            if (ringing) {
                // Liquid glass slide bar — no instructional text
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .onSizeChanged { trackWidthPx = it.width.toFloat() }
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color(0x28FFFFFF))
                        .border(1.dp, Color(0x44FFFFFF), RoundedCornerShape(32.dp))
                ) {
                    // subtle glass highlight
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                            .align(Alignment.TopCenter)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0x33FFFFFF), Color.Transparent)
                                )
                            )
                    )
                    Text(
                        "slide to answer",
                        color = Color(0x66FFFFFF),
                        modifier = Modifier.align(Alignment.Center),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Box(
                        Modifier
                            .offset { IntOffset(displaySlide.roundToInt(), 0) }
                            .padding(6.dp)
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF34C759), Color(0xFF28A745))
                                )
                            )
                            .border(1.dp, Color(0x55FFFFFF), CircleShape)
                            .align(Alignment.CenterStart)
                            .pointerInput(maxSlide, answerThreshold) {
                                detectHorizontalDragGestures(
                                    onDragStart = { dragging = true },
                                    onDragEnd = {
                                        dragging = false
                                        if (slidePx >= answerThreshold) {
                                            if (!preview) CallSession.answer()
                                            onAnswered()
                                            slidePx = 0f
                                        } else {
                                            slidePx = 0f
                                        }
                                    },
                                    onDragCancel = {
                                        dragging = false
                                        slidePx = 0f
                                    },
                                    onHorizontalDrag = { _, dx ->
                                        slidePx = (slidePx + dx).coerceIn(0f, maxSlide)
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Call, null, tint = Color.White, modifier = Modifier.size(26.dp))
                    }
                }
                Spacer(Modifier.height(18.dp))
                // Decline with short SMS (opens Messages; rejects call)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf(
                        "Can't talk now",
                        "Call you later",
                        "I'm in Salah"
                    ).forEach { msg ->
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0x33FFFFFF))
                                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(18.dp))
                                .clickable {
                                    if (!preview) {
                                        CallSession.reject()
                                        PhoneRepository(context).openSms(displayNumber, msg)
                                    }
                                    onFinish()
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(msg, color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Message",
                    color = Color(0xFF0A84FF),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable {
                        if (!preview) {
                            CallSession.reject()
                            PhoneRepository(context).openSms(displayNumber, null)
                        }
                        onFinish()
                    }
                )
            } else {
                if (showKeypad) {
                    val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#")
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        keys.chunked(3).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                                row.forEach { k ->
                                    Box(
                                        Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .background(Color(0x33FFFFFF))
                                            .clickable {
                                                DialPadTone.play(k.first())
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(k, color = Color.White, fontSize = 24.sp)
                                    }
                                }
                            }
                        }
                    }
                    Text(
                        "Hide keypad",
                        color = Color(0xFF0A84FF),
                        modifier = Modifier.padding(12.dp).clickable { showKeypad = false }
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    CallAction(Icons.Filled.VolumeUp, "Speaker", speaker) {
                        speaker = !speaker
                        if (!preview) CallSession.setSpeaker(speaker)
                    }
                    CallAction(Icons.Filled.Videocam, "Video", false) { }
                    CallAction(if (muted) Icons.Filled.MicOff else Icons.Filled.Mic, "Mute", muted) {
                        muted = !muted
                        if (!preview) CallSession.setMuted(muted)
                    }
                }
                Spacer(Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    CallAction(Icons.Filled.PersonAdd, "Add", false) {
                        if (!preview) {
                            context.startActivity(
                                android.content.Intent(context, PhoneLauncherActivity::class.java)
                                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            if (!preview) CallSession.disconnect()
                            onFinish()
                        },
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF3B30))
                    ) {
                        Icon(Icons.Filled.CallEnd, null, tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                    CallAction(Icons.Filled.Dialpad, "Keypad", showKeypad) {
                        showKeypad = !showKeypad
                    }
                }
                Spacer(Modifier.height(12.dp))
                CallAction(if (onHold) Icons.Filled.PlayArrow else Icons.Filled.Pause, if (onHold) "Resume" else "Hold", onHold) {
                    onHold = !onHold
                    if (!preview) {
                        if (onHold) CallSession.hold() else CallSession.unhold()
                    }
                }
            }
        }
    }
}

@Composable
private fun CallAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(if (active) Color.White else Color(0x33FFFFFF))
        ) {
            Icon(icon, null, tint = if (active) Color.Black else Color.White, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(label, color = Color(0xCCFFFFFF), fontSize = 12.sp)
    }
}

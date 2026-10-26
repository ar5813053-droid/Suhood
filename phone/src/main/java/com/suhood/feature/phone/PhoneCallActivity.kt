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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.telecom.Call
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.suhood.core.designsystem.theme.SuhoodTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
        val headsUp = intent.getBooleanExtra(EXTRA_HEADS_UP, false)

        // Transparent window so floating call UI sits over other apps
        if (headsUp && !preview) {
            window.setBackgroundDrawableResource(android.R.color.transparent)
            @Suppress("DEPRECATION")
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            @Suppress("DEPRECATION")
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
        }

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
                    headsUp = headsUp,
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
        const val EXTRA_HEADS_UP = "heads_up"

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
    headsUp: Boolean = false,
    onFinish: () -> Unit,
    onAnswered: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember { PhonePrefs(context) }
    val state by CallSession.state.collectAsState()
    val number by CallSession.number.collectAsState()
    val name by CallSession.displayName.collectAsState()

    val displayNumber = if (preview) previewNumber else number.ifBlank { previewNumber }
    val resolvedContact = remember(displayNumber) {
        if (preview) null else resolveLocalContactName(context, displayNumber)
    }
    val displayName = when {
        preview -> previewName
        !resolvedContact.isNullOrBlank() -> resolvedContact
        name.isNotBlank() && name != displayNumber -> name
        else -> displayNumber.ifBlank { "Unknown" }
    }
    val emoji = prefs.contactEmoji(displayNumber)
    // Overlay vs full-screen (user can expand)
    var expanded by remember { mutableStateOf(preview || !headsUp) }

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
    var trackWidthPx by remember { mutableFloatStateOf(1f) }
    var elapsed by remember { mutableIntStateOf(0) }
    val slideAnim = remember { Animatable(0f) }
    var dragging by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val ringing = preview || state == Call.STATE_RINGING
    val density = LocalDensity.current
    val knobSizeDp = 64.dp
    val knobSizePx = with(density) { knobSizeDp.toPx() }
    val trackPadPx = with(density) { 6.dp.toPx() }
    val maxSlide = (trackWidthPx - knobSizePx - trackPadPx * 2).coerceAtLeast(1f)
    // Bidirectional: ~78% of half-track
    val answerThreshold = (maxSlide / 2f) * 0.78f

    // Soft idle breathe (very subtle, slow) — only when not dragging and near center
    val infinite = rememberInfiniteTransition(label = "glassBreathe")
    val breathe by infinite.animateFloat(
        initialValue = 0f,
        targetValue = with(density) { 6.dp.toPx() },
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )
    val displaySlide = if (dragging) {
        slideAnim.value
    } else if (kotlin.math.abs(slideAnim.value) < 3f) {
        slideAnim.value + breathe * 0.45f
    } else {
        slideAnim.value
    }

    LaunchedEffect(state, preview) {
        if (!preview && state == Call.STATE_ACTIVE) {
            elapsed = 0
            while (true) {
                delay(1000)
                elapsed++
            }
        }
    }

    // —— FLOATING OVERLAY MODE (incoming + in-call) ——
    if (!expanded && !preview) {
        Box(Modifier.fillMaxSize()) {
            // Soft dim only behind bars
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.18f))
                    .clickable {
                        // Tap outside expands to full call UI
                        expanded = true
                    }
            )
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(initialOffsetY = { -it / 3 }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it / 3 }) + fadeOut(),
                modifier = Modifier
                    .align(if (ringing) Alignment.TopCenter else Alignment.BottomCenter)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                if (ringing) {
                    IncomingFloatingBar(
                        name = displayName,
                        number = displayNumber,
                        emoji = emoji,
                        numeralStyle = prefs.numeralStyle,
                        onDecline = {
                            CallSession.reject()
                            onFinish()
                        },
                        onAnswer = {
                            CallSession.answer()
                            onAnswered()
                            // Stay as floating in-call controls
                        },
                        onExpand = { expanded = true }
                    )
                } else {
                    OngoingFloatingBar(
                        name = displayName,
                        number = displayNumber,
                        emoji = emoji,
                        elapsed = elapsed,
                        muted = muted,
                        onMute = {
                            muted = !muted
                            CallSession.setMuted(muted)
                        },
                        onEnd = {
                            CallSession.disconnect()
                            onFinish()
                        },
                        onExpand = { expanded = true },
                        onSpeaker = {
                            speaker = !speaker
                            CallSession.setSpeaker(speaker)
                        },
                        speaker = speaker
                    )
                }
            }
        }
        return
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
            if (!ringing) {
                Text(
                    when {
                        state == Call.STATE_DIALING || state == Call.STATE_CONNECTING -> "Calling…"
                        state == Call.STATE_HOLDING -> "On hold"
                        else -> String.format("%02d:%02d", elapsed / 60, elapsed % 60)
                    },
                    color = Color(0xFF8E8E93),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
            }
            Text(
                displayName,
                color = Color.White,
                fontSize = 34.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 40.sp,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
            )
            if (!ringing) {
                Spacer(Modifier.height(6.dp))
                Text(
                    PhoneNumberUtils.display(
                        PhoneNumberUtils.normalize(displayNumber).ifBlank { displayNumber },
                        prefs.numeralStyle
                    ),
                    color = Color(0xFF8E8E93),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
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
                // Full incoming — same as preview
                    // iOS-scale liquid-glass slide bar (left = decline, right = answer)
                    Spacer(Modifier.height(12.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 28.dp)
                            .height(78.dp)
                            .onSizeChanged { trackWidthPx = it.width.toFloat() }
                            .shadow(
                                elevation = 24.dp,
                                shape = RoundedCornerShape(39.dp),
                                ambientColor = Color.Black.copy(alpha = 0.40f),
                                spotColor = Color.Black.copy(alpha = 0.30f)
                            )
                            .clip(RoundedCornerShape(39.dp))
                            // Frosted green liquid glass (Apple-style)
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0x66A8E6B0),
                                        Color(0x5590C99A),
                                        Color(0x4480B888)
                                    )
                                )
                            )
                            .border(
                                width = 1.25.dp,
                                brush = Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.55f),
                                        Color.White.copy(alpha = 0.12f),
                                        Color.White.copy(alpha = 0.30f)
                                    )
                                ),
                                shape = RoundedCornerShape(39.dp)
                            )
                            .drawBehind {
                                // Top specular sheen
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        0f to Color.White.copy(alpha = 0.32f),
                                        0.35f to Color.Transparent
                                    ),
                                    size = size.copy(height = size.height * 0.5f)
                                )
                            }
                    ) {
                        // Soft decline / answer zones
                        Box(
                            Modifier
                                .align(Alignment.CenterStart)
                                .fillMaxHeight()
                                .fillMaxWidth(0.42f)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color(0x44FF3B30).copy(
                                                alpha = (0.12f + (-slideAnim.value / (maxSlide / 2f)).coerceIn(0f, 1f) * 0.35f)
                                            ),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                        Box(
                            Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight()
                                .fillMaxWidth(0.42f)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color.Transparent,
                                            Color(0x4434C759).copy(
                                                alpha = (0.12f + (slideAnim.value / (maxSlide / 2f)).coerceIn(0f, 1f) * 0.35f)
                                            )
                                        )
                                    )
                                )
                        )
                        // Hint icons (subtle, like iOS secondary affordances)
                        Icon(
                            Icons.Filled.CallEnd,
                            contentDescription = null,
                            tint = Color.White.copy(
                                alpha = 0.28f + (-slideAnim.value / (maxSlide / 2f)).coerceIn(0f, 1f) * 0.45f
                            ),
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = 22.dp)
                                .size(22.dp)
                        )
                        Icon(
                            Icons.Filled.Call,
                            contentDescription = null,
                            tint = Color.White.copy(
                                alpha = 0.28f + (slideAnim.value / (maxSlide / 2f)).coerceIn(0f, 1f) * 0.45f
                            ),
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 22.dp)
                                .size(22.dp)
                        )
                        // Large circular glass knob — centered, spring-driven
                        val centerX = (trackWidthPx - knobSizePx) / 2f
                        Box(
                            Modifier
                                .offset { IntOffset((centerX + displaySlide).roundToInt(), 0) }
                                .padding(7.dp)
                                .size(knobSizeDp)
                                .align(Alignment.CenterStart)
                                .shadow(
                                    elevation = 14.dp,
                                    shape = CircleShape,
                                    ambientColor = Color.Black.copy(alpha = 0.35f),
                                    spotColor = Color.Black.copy(alpha = 0.28f)
                                )
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = when {
                                            slideAnim.value > answerThreshold * 0.35f -> listOf(
                                                Color(0xFFE8FFE9),
                                                Color(0xFF34C759)
                                            )
                                            slideAnim.value < -answerThreshold * 0.35f -> listOf(
                                                Color(0xFFFFE8E6),
                                                Color(0xFFFF3B30)
                                            )
                                            else -> listOf(
                                                Color.White,
                                                Color(0xFFF2F2F7)
                                            )
                                        }
                                    )
                                )
                                .border(
                                    width = 1.2.dp,
                                    brush = Brush.verticalGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.95f),
                                            Color.White.copy(alpha = 0.35f)
                                        )
                                    ),
                                    shape = CircleShape
                                )
                                .pointerInput(maxSlide, answerThreshold) {
                                    detectHorizontalDragGestures(
                                        onDragStart = { dragging = true },
                                        onDragEnd = {
                                            dragging = false
                                            val v = slideAnim.value
                                            when {
                                                v >= answerThreshold -> {
                                                    if (!preview) CallSession.answer()
                                                    onAnswered()
                                                    scope.launch {
                                                        slideAnim.animateTo(
                                                            0f,
                                                            spring(
                                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                                stiffness = Spring.StiffnessMediumLow
                                                            )
                                                        )
                                                    }
                                                }
                                                v <= -answerThreshold -> {
                                                    if (!preview) CallSession.reject()
                                                    onFinish()
                                                    scope.launch { slideAnim.snapTo(0f) }
                                                }
                                                else -> {
                                                    scope.launch {
                                                        slideAnim.animateTo(
                                                            0f,
                                                            spring(
                                                                dampingRatio = 0.78f,
                                                                stiffness = Spring.StiffnessMediumLow
                                                            )
                                                        )
                                                    }
                                                }
                                            }
                                        },
                                        onDragCancel = {
                                            dragging = false
                                            scope.launch {
                                                slideAnim.animateTo(
                                                    0f,
                                                    spring(
                                                        dampingRatio = 0.78f,
                                                        stiffness = Spring.StiffnessMediumLow
                                                    )
                                                )
                                            }
                                        },
                                        onHorizontalDrag = { _, dx ->
                                            val half = maxSlide / 2f
                                            val next = (slideAnim.value + dx).coerceIn(-half, half)
                                            scope.launch { slideAnim.snapTo(next) }
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            // Specular highlight on glass knob
                            Box(
                                Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 8.dp)
                                    .width(26.dp)
                                    .height(11.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.White.copy(alpha = 0.65f))
                            )
                            Icon(
                                imageVector = when {
                                    slideAnim.value < -answerThreshold * 0.3f -> Icons.Filled.CallEnd
                                    else -> Icons.Filled.Call
                                },
                                contentDescription = null,
                                tint = when {
                                    slideAnim.value > answerThreshold * 0.35f -> Color.White
                                    slideAnim.value < -answerThreshold * 0.35f -> Color.White
                                    else -> Color(0xFF34C759)
                                },
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    // Message / quick-reply chips (Message-style affordance under slider)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        listOf("Can't talk now", "Call you later", "I'm in Salah").forEach { msg ->
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color.White.copy(alpha = 0.14f))
                                    .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(20.dp))
                                    .clickable {
                                        if (!preview) {
                                            CallSession.reject()
                                            PhoneRepository(context).openSms(displayNumber, msg)
                                        }
                                        onFinish()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(msg, color = Color.White.copy(alpha = 0.92f), fontSize = 12.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
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


@Composable
private fun IncomingFloatingBar(
    name: String,
    number: String,
    emoji: String?,
    numeralStyle: NumeralStyle,
    onDecline: () -> Unit,
    onAnswer: () -> Unit,
    onExpand: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(40.dp))
            .clip(RoundedCornerShape(40.dp))
            .background(Color(0xF01C1C1E))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(40.dp))
            .clickable { onExpand() }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(46.dp).clip(CircleShape).background(Color(0x33FFFFFF)),
            contentAlignment = Alignment.Center
        ) {
            if (!emoji.isNullOrBlank()) Text(emoji, fontSize = 22.sp)
            else Text(
                name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f).clickable { onExpand() }) {
            Text(
                name,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 19.sp
            )
            Text(
                PhoneNumberUtils.display(
                    PhoneNumberUtils.normalize(number).ifBlank { number },
                    numeralStyle
                ),
                color = Color(0x99FFFFFF),
                fontSize = 13.sp,
                maxLines = 1
            )
        }
        IconButton(
            onClick = onDecline,
            modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFFFF3B30))
        ) {
            Icon(Icons.Filled.CallEnd, null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(10.dp))
        IconButton(
            onClick = onAnswer,
            modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFF34C759))
        ) {
            Icon(Icons.Filled.Call, null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun OngoingFloatingBar(
    name: String,
    number: String,
    emoji: String?,
    elapsed: Int,
    muted: Boolean,
    speaker: Boolean,
    onMute: () -> Unit,
    onEnd: () -> Unit,
    onExpand: () -> Unit,
    onSpeaker: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .shadow(18.dp, RoundedCornerShape(36.dp))
            .clip(RoundedCornerShape(36.dp))
            .background(Color(0xF0111111))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(36.dp))
            .clickable { onExpand() }
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(Color(0x33FFFFFF)),
                contentAlignment = Alignment.Center
            ) {
                if (!emoji.isNullOrBlank()) Text(emoji, fontSize = 16.sp)
                else Text(
                    name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    String.format("%02d:%02d", elapsed / 60, elapsed % 60),
                    color = Color(0xFF8E8E93),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OverlayAction(Icons.Filled.Videocam, onClick = { })
            OverlayAction(if (muted) Icons.Filled.MicOff else Icons.Filled.Mic, active = muted, onClick = onMute)
            IconButton(
                onClick = onEnd,
                modifier = Modifier.size(56.dp).clip(CircleShape).background(Color(0xFFFF3B30))
            ) {
                Icon(Icons.Filled.CallEnd, null, tint = Color.White, modifier = Modifier.size(26.dp))
            }
            OverlayAction(Icons.Filled.PersonAdd, onClick = { })
            OverlayAction(Icons.Filled.MoreHoriz, onClick = onExpand)
        }
    }
}

@Composable
private fun OverlayAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean = false,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(44.dp).clip(CircleShape).background(if (active) Color.White else Color(0x33FFFFFF))
    ) {
        Icon(icon, null, tint = if (active) Color.Black else Color.White, modifier = Modifier.size(20.dp))
    }
}

private fun resolveLocalContactName(context: android.content.Context, number: String): String? {
    if (number.isBlank()) return null
    return try {
        val uri = android.net.Uri.withAppendedPath(
            android.provider.ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            android.net.Uri.encode(number)
        )
        context.contentResolver.query(
            uri,
            arrayOf(android.provider.ContactsContract.PhoneLookup.DISPLAY_NAME),
            null, null, null
        )?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
    } catch (_: Exception) {
        null
    }
}


package com.suhood.feature.phone

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.telecom.Call
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.suhood.core.designsystem.theme.SuhoodTheme
import kotlinx.coroutines.delay

class PhoneCallActivity : ComponentActivity() {
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

        setContent {
            SuhoodTheme {
                CallUiScreen(
                    preview = preview,
                    previewNumber = previewNumber,
                    previewName = previewName,
                    forceThemeName = forceTheme,
                    onFinish = { finish() }
                )
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Power / endcall declines ringing call (system often consumes power; Endcall works when default dialer)
        if (keyCode == KeyEvent.KEYCODE_POWER || keyCode == KeyEvent.KEYCODE_ENDCALL) {
            if (CallSession.state.value == Call.STATE_RINGING) {
                CallSession.reject()
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
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { PhonePrefs(context) }
    val state by CallSession.state.collectAsState()
    val number by CallSession.number.collectAsState()
    val name by CallSession.displayName.collectAsState()

    val displayNumber = if (preview) previewNumber else number.ifBlank { previewNumber }
    val displayName = if (preview) previewName else name.ifBlank { displayNumber.ifBlank { "Unknown" } }

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
    var slideOffset by remember { mutableFloatStateOf(0f) }
    var elapsed by remember { mutableIntStateOf(0) }

    val ringing = preview || state == Call.STATE_RINGING
    val active = !ringing

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
                    Brush.verticalGradient(listOf(Color(0xFF2C2C2E), Color(0xFF1C1C1E), Color(0xFF000000)))
                )
            )
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(Color(0x66000000), Color(0x33000000), Color(0xCC000000))
                )
            )
        )

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (preview) {
                Text("PREVIEW", color = Color(0xFFFFD60A), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            Text(
                when {
                    ringing -> "Incoming call"
                    state == Call.STATE_DIALING || state == Call.STATE_CONNECTING -> "Calling…"
                    state == Call.STATE_HOLDING -> "On hold"
                    else -> String.format("%02d:%02d", elapsed / 60, elapsed % 60)
                },
                color = Color(0x99FFFFFF),
                fontSize = 15.sp
            )
            Spacer(Modifier.height(12.dp))
            Text(
                displayName,
                color = Color.White,
                fontSize = 34.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2
            )
            Spacer(Modifier.height(6.dp))
            Text(
                PhoneNumberUtils.display(
                    PhoneNumberUtils.normalize(displayNumber).ifBlank { displayNumber },
                    prefs.numeralStyle
                ),
                color = Color(0x99FFFFFF),
                fontSize = 16.sp
            )
            Spacer(Modifier.height(36.dp))
            Box(
                Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color(0x33FFFFFF)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    displayName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    color = Color.White,
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Light
                )
            }
            Spacer(Modifier.weight(1f))

            if (ringing) {
                // Slide to answer (iPhone-inspired)
                Text("Slide to answer  ·  Power key declines", color = Color(0x99FFFFFF), fontSize = 13.sp)
                Spacer(Modifier.height(16.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color(0x33FFFFFF))
                ) {
                    Text(
                        "slide to answer",
                        color = Color(0x88FFFFFF),
                        modifier = Modifier.align(Alignment.Center),
                        fontSize = 16.sp
                    )
                    Box(
                        Modifier
                            .offset(x = (slideOffset / 3f).dp)
                            .padding(6.dp)
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF34C759))
                            .align(Alignment.CenterStart)
                            .pointerInput(Unit) {
                                detectHorizontalDragGestures(
                                    onDragEnd = {
                                        if (slideOffset > 180f) {
                                            if (!preview) CallSession.answer()
                                            slideOffset = 0f
                                        } else {
                                            slideOffset = 0f
                                        }
                                    },
                                    onHorizontalDrag = { _, dx ->
                                        slideOffset = (slideOffset + dx).coerceIn(0f, 280f)
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Call, null, tint = Color.White)
                    }
                }
                Spacer(Modifier.height(20.dp))
                IconButton(
                    onClick = {
                        if (!preview) CallSession.reject()
                        onFinish()
                    },
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF3B30))
                ) {
                    Icon(Icons.Filled.CallEnd, null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
            } else {
                if (showKeypad) {
                    // simple in-call DTMF
                    val keys = listOf("1","2","3","4","5","6","7","8","9","*","0","#")
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        keys.chunked(3).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                row.forEach { k ->
                                    Box(
                                        Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .background(Color(0x33FFFFFF))
                                            .clickable {
                                                DialPadTone.play(k.first())
                                                CallSession.state // keep session
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(k, color = Color.White, fontSize = 24.sp)
                                    }
                                }
                            }
                        }
                    }
                    TextButtonLike("Hide keypad") { showKeypad = false }
                }
                // iPhone-style grid
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    CallAction(Icons.Filled.VolumeUp, "Speaker", speaker) {
                        speaker = !speaker
                        if (!preview) CallSession.setSpeaker(speaker)
                    }
                    CallAction(Icons.Filled.Videocam, "Video", false) {
                        // Video not supported for pure audio Telecom path — no fake success
                    }
                    CallAction(if (muted) Icons.Filled.MicOff else Icons.Filled.Mic, "Mute", muted) {
                        muted = !muted
                        if (!preview) CallSession.setMuted(muted)
                    }
                }
                Spacer(Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    CallAction(Icons.Filled.PersonAdd, "Add", false) {
                        // Conference requires default dialer + system support; open dialer for second call
                        if (!preview) {
                            context.startActivity(
                                android.content.Intent(context, PhoneLauncherActivity::class.java)
                                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    }
                    Box(
                        Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF3B30))
                            .pointerInput(Unit) {
                                // handled below
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(onClick = {
                            if (!preview) CallSession.disconnect()
                            onFinish()
                        }, modifier = Modifier.fillMaxSize()) {
                            Icon(Icons.Filled.CallEnd, null, tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                    }
                    CallAction(Icons.Filled.Dialpad, "Keypad", showKeypad) {
                        showKeypad = !showKeypad
                        if (showKeypad) {
                            // play tone feedback available when keys pressed
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
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
private fun TextButtonLike(text: String, onClick: () -> Unit) {
    Text(
        text,
        color = Color(0xFF0A84FF),
        modifier = Modifier
            .padding(8.dp)
            .clickable(onClick = onClick),
        fontSize = 15.sp
    )
}

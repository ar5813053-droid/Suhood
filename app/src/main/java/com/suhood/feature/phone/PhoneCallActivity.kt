package com.suhood.feature.phone

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.telecom.Call
import com.suhood.core.designsystem.theme.SuhoodTheme

/**
 * Full-screen incoming / ongoing call UI. Loaded by [SuhoodInCallService] when Suhood is the
 * default dialer, or via [EXTRA_PREVIEW] for Appearance Studio testing (no real call).
 */
class PhoneCallActivity : ComponentActivity() {
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

    companion object {
        const val EXTRA_PREVIEW = "preview"
        const val EXTRA_NUMBER = "number"
        const val EXTRA_NAME = "name"
        const val EXTRA_THEME = "theme"

        fun previewIntent(
            context: Context,
            theme: CallThemeId? = null,
            name: String = "Ahmad (Preview)",
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
    val displayName = if (preview) previewName else name.ifBlank { "Unknown" }

    val themeId = remember(forceThemeName, displayNumber, state) {
        forceThemeName?.let { runCatching { CallThemeId.valueOf(it) }.getOrNull() }
            ?: prefs.contactTheme(displayNumber)
            ?: if (state == Call.STATE_RINGING || preview) prefs.incomingTheme else prefs.ongoingTheme
    }
    val theme = CallThemes.colors(themeId)

    var muted by remember { mutableStateOf(false) }
    var onHold by remember { mutableStateOf(false) }
    var speaker by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(theme.background)) {
        theme.wallpaperRes?.let { res ->
            Image(
                painter = painterResource(res),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        // Dark gradient for readability
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0x99000000),
                            Color(0x44000000),
                            Color(0xCC000000)
                        )
                    )
                )
        )

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (preview) {
                Text(
                    "PREVIEW ONLY — not a real call",
                    color = Color(0xFFFFD54F),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
            }
            Text(
                when {
                    preview -> "Incoming call"
                    state == Call.STATE_RINGING -> "Incoming call"
                    state == Call.STATE_HOLDING -> "On hold"
                    state == Call.STATE_ACTIVE -> "On call"
                    state == Call.STATE_DIALING || state == Call.STATE_CONNECTING -> "Calling…"
                    else -> "Call"
                },
                color = theme.secondary,
                fontSize = 14.sp
            )
            Spacer(Modifier.height(28.dp))
            Box(
                Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(theme.surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Person, null, tint = theme.text, modifier = Modifier.size(48.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text(
                displayName,
                color = theme.text,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                PhoneNumberUtils.display(
                    PhoneNumberUtils.normalize(displayNumber).ifBlank { displayNumber },
                    prefs.numeralStyle
                ),
                color = theme.secondary,
                fontSize = 18.sp
            )
            Spacer(Modifier.weight(1f))

            val ringing = preview || state == Call.STATE_RINGING
            if (ringing) {
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 32.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (!preview) CallSession.reject()
                            onFinish()
                        },
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFCE1126))
                    ) {
                        Icon(Icons.Filled.CallEnd, null, tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                    IconButton(
                        onClick = {
                            if (!preview) CallSession.answer()
                        },
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2ECC71))
                    ) {
                        Icon(Icons.Filled.Call, null, tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                }
            } else {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(theme.surface)
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        muted = !muted
                        if (!preview) CallSession.setMuted(muted)
                    }) {
                        Icon(if (muted) Icons.Filled.MicOff else Icons.Filled.Mic, null, tint = theme.text)
                    }
                    IconButton(onClick = {
                        onHold = !onHold
                        if (!preview) {
                            if (onHold) CallSession.hold() else CallSession.unhold()
                        }
                    }) {
                        Icon(if (onHold) Icons.Filled.PlayArrow else Icons.Filled.Pause, null, tint = theme.text)
                    }
                    IconButton(
                        onClick = {
                            if (!preview) CallSession.disconnect()
                            onFinish()
                        },
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFCE1126))
                    ) {
                        Icon(Icons.Filled.CallEnd, null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                    IconButton(onClick = {
                        speaker = !speaker
                        if (!preview) CallSession.setSpeaker(speaker)
                    }) {
                        Icon(Icons.Filled.VolumeUp, null, tint = theme.text)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

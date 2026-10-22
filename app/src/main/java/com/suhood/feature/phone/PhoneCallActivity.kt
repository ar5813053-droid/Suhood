package com.suhood.feature.phone

import android.os.Bundle
import android.telecom.Call
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suhood.core.designsystem.theme.SuhoodTheme
import com.suhood.core.designsystem.theme.ThemeMode
import kotlinx.coroutines.delay

class PhoneCallActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val number = intent.getStringExtra("number").orEmpty()
        val state = intent.getIntExtra("state", Call.STATE_RINGING)
        setContent {
            SuhoodTheme(themeMode = ThemeMode.DARK) {
                CallUi(number = number, initialState = state, onFinish = { finish() })
            }
        }
    }
}

@Composable
private fun CallUi(number: String, initialState: Int, onFinish: () -> Unit) {
    val prefs = PhonePrefs(LocalContext.current)
    val theme = CallThemes.colors(prefs.incomingTheme)
    var muted by remember { mutableStateOf(false) }
    var state by remember { mutableIntStateOf(initialState) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            val s = CallSession.active?.state
            if (s != null) state = s
            if (s == null || s == Call.STATE_DISCONNECTED) {
                onFinish()
                break
            }
        }
    }

    val title = when (state) {
        Call.STATE_RINGING -> "Incoming call"
        Call.STATE_DIALING, Call.STATE_CONNECTING -> "Calling…"
        Call.STATE_ACTIVE -> "On call"
        Call.STATE_HOLDING -> "On hold"
        else -> "Call"
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(theme.background, theme.surface, theme.background)))
            .statusBarsPadding()
            .padding(24.dp)
    ) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(48.dp))
                Text(title, color = theme.secondary, fontSize = 14.sp)
                Spacer(Modifier.height(12.dp))
                Text(
                    PhoneNumberUtils.display(PhoneNumberUtils.normalize(number).ifBlank { "Unknown" }, prefs.numeralStyle),
                    color = theme.text, fontSize = 32.sp, fontWeight = FontWeight.SemiBold
                )
            }
            Row(Modifier.fillMaxWidth().padding(bottom = 48.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                if (state == Call.STATE_RINGING) {
                    IconButton(onClick = { CallSession.reject(); onFinish() }, modifier = Modifier.size(72.dp).clip(CircleShape).background(Color(0xFFCE1126))) {
                        Icon(Icons.Filled.CallEnd, null, tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                    IconButton(onClick = { CallSession.answer() }, modifier = Modifier.size(72.dp).clip(CircleShape).background(theme.accent)) {
                        Icon(Icons.Filled.Call, null, tint = Color(0xFF08090B), modifier = Modifier.size(32.dp))
                    }
                } else {
                    IconButton(onClick = { muted = !muted }) {
                        Icon(if (muted) Icons.Filled.MicOff else Icons.Filled.Mic, null, tint = theme.text)
                    }
                    IconButton(onClick = { CallSession.disconnect(); onFinish() }, modifier = Modifier.size(72.dp).clip(CircleShape).background(Color(0xFFCE1126))) {
                        Icon(Icons.Filled.CallEnd, null, tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                    IconButton(onClick = { }) {
                        Icon(Icons.Filled.VolumeUp, null, tint = theme.text)
                    }
                }
            }
        }
    }
}

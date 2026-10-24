package com.suhood.feature.focus

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suhood.app.R
import kotlinx.coroutines.delay

/**
 * Full-screen Salah restriction. Does not clear the lock.
 * Only action: leave to the launcher while the 10-minute window continues.
 */
class SalahLockActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Reconcile on entry — expired locks must not stick
        SalahLockStore.get(this).reconcileNow()

        setContent {
            val store = remember { SalahLockStore.get(this@SalahLockActivity) }
            var remainingMs by remember {
                mutableLongStateOf(
                    SalahLockLogic.remainingMs(store.read(), System.currentTimeMillis())
                )
            }

            LaunchedEffect(Unit) {
                while (true) {
                    val snap = store.reconcileNow()
                    remainingMs = SalahLockLogic.remainingMs(snap, System.currentTimeMillis())
                    if (!SalahLockLogic.isActive(snap, System.currentTimeMillis())) {
                        // Restriction ended — leave without unlocking early
                        goHomeAndFinish()
                        break
                    }
                    delay(1000)
                }
            }

            BackHandler {
                // Back must not return to the blocked app
                goHomeAndFinish()
            }

            SalahLockScreen(
                remainingLabel = SalahLockLogic.formatMmSs(remainingMs),
                onExit = { goHomeAndFinish() }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        SalahLockStore.get(this).reconcileNow()
    }

    private fun goHomeAndFinish() {
        runCatching {
            startActivity(
                Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
        }
        finish()
    }

    companion object {
        const val EXTRA_PACKAGE = "package"
        const val EXTRA_PRAYER = "prayer"
    }
}

@Composable
private fun SalahLockScreen(
    remainingLabel: String,
    onExit: () -> Unit
) {
    val mint = Color(0xFF9FE8C8)
    val silver = Color(0xFFE8E8EC)
    val glass = Color(0x331A2228)
    val glassBorder = Color(0x44FFFFFF)

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        // Mosque wallpaper (bundled) with dark gradient overlay
        runCatching {
            Image(
                painter = painterResource(R.drawable.bg_mosque_dusk),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.55f
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xF0000000),
                            Color(0x99000000),
                            Color(0xCC000000),
                            Color(0xF2000000)
                        )
                    )
                )
        )

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))

            // Logo
            runCatching {
                Image(
                    painter = painterResource(R.drawable.ic_suhood_logo),
                    contentDescription = "Suhood",
                    modifier = Modifier.size(64.dp),
                    contentScale = ContentScale.Fit
                )
            }.onFailure {
                Text(
                    "◆",
                    color = silver,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Light
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Suhood",
                color = silver,
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp
            )

            Spacer(Modifier.height(8.dp))
            Text("◆", color = Color(0x66FFFFFF), fontSize = 12.sp)

            Spacer(Modifier.height(28.dp))
            Text(
                text = "It's Salah Time",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            // Tint "Salah" feel via secondary line is enough; full annotated optional
            Spacer(Modifier.height(10.dp))
            Text(
                "Pause distractions and\nreturn to Allah.",
                color = Color(0xFFB8BEC6),
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(Modifier.height(36.dp))

            // Glass timer card
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(glass)
                    .border(1.dp, glassBorder, RoundedCornerShape(22.dp))
                    .padding(horizontal = 20.dp, vertical = 22.dp)
            ) {
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0x22FFFFFF))
                            .border(1.dp, glassBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Mosque,
                            contentDescription = null,
                            tint = silver,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "APP LOCKED FOR",
                        color = Color(0xFF9AA3AE),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        remainingLabel,
                        color = mint,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "You can open the app again after\n10 minutes.",
                        color = Color(0xFF9AA3AE),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // Single action — does NOT clear lock
            androidx.compose.material3.Button(
                onClick = onExit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(28.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1A2E2A),
                    contentColor = mint
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, mint.copy(alpha = 0.45f))
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = null,
                    tint = mint,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.size(10.dp))
                Text(
                    "I Understand — Exit App",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = mint
                )
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

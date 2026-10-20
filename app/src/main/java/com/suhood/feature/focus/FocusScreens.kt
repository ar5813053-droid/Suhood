package com.suhood.feature.focus

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.suhood.core.designsystem.components.*
import com.suhood.core.designsystem.theme.SuhoodSpacing
import com.suhood.core.designsystem.theme.SuhoodTypography
import com.suhood.core.designsystem.theme.suhoodColors
import com.suhood.core.navigation.Routes

object FocusScreens {

    @Composable
    fun Dashboard(onNavigate: (String) -> Unit, bottomContentPadding: Dp) {
        val colors = suhoodColors
        val context = LocalContext.current
        val session = remember {
            FocusSessionController.get(context).tick()
        }
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SuhoodSpacing.screenHorizontal)
                .padding(bottom = bottomContentPadding + 24.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            Text("تركيز", style = SuhoodTypography.arabicTitle, color = colors.textPrimary)
            Text("Focus", style = SuhoodTypography.headlineMedium, color = colors.textPrimary)
            Text(
                "Protect time before Salah — timed sessions with optional app limits.",
                style = SuhoodTypography.bodyMedium,
                color = colors.textSecondary
            )
            Spacer(Modifier.height(8.dp))
            DemoStatusLabel("Usage Access · Accessibility optional")

            Spacer(Modifier.height(20.dp))
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Session", style = SuhoodTypography.labelMedium, color = colors.textMuted)
                Spacer(Modifier.height(6.dp))
                Text(
                    session.state.name.replace('_', ' '),
                    style = SuhoodTypography.headlineSmall,
                    color = colors.textPrimary
                )
                if (session.remainingSeconds > 0) {
                    Text(
                        formatTime(session.remainingSeconds),
                        style = SuhoodTypography.titleLarge,
                        color = colors.silverBright
                    )
                }
                Spacer(Modifier.height(14.dp))
                GlassButton(
                    text = "Start focus session",
                    onClick = { onNavigate(Routes.FOCUS_DURATION) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassCard(
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Routes.FOCUS_SELECT_APPS) }
                ) {
                    Text("Apps", style = SuhoodTypography.titleSmall, color = colors.textPrimary)
                    Text("Limit list", style = SuhoodTypography.labelSmall, color = colors.textMuted)
                }
                GlassCard(
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Routes.FOCUS_HISTORY) }
                ) {
                    Text("History", style = SuhoodTypography.titleSmall, color = colors.textPrimary)
                    Text("Past sessions", style = SuhoodTypography.labelSmall, color = colors.textMuted)
                }
            }

            Spacer(Modifier.height(14.dp))
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("How it works", style = SuhoodTypography.titleSmall, color = colors.textPrimary)
                Spacer(Modifier.height(8.dp))
                Text(
                    "1. Choose duration\n2. Optionally select apps to limit\n3. Enable Usage Access / Accessibility when prompted\n4. Stay with the session until complete",
                    style = SuhoodTypography.bodySmall,
                    color = colors.textSecondary
                )
            }
        }
    }

    @Composable
    fun SelectApps(onBack: () -> Unit) {
        val colors = suhoodColors
        val context = LocalContext.current
        val controller = remember { FocusSessionController.get(context) }
        val installed = remember {
            runCatching {
                InstalledAppsRepository.loadLauncherApps(context)
            }.getOrDefault(emptyList())
        }
        // Persist selection so returning to this screen keeps choices
        var selected by remember { mutableStateOf(controller.getBlockedApps()) }

        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            ScreenHeader(title = "Distracting apps", onBack = onBack)
            Text(
                "Enable apps to limit during focus. Your choices are saved.",
                style = SuhoodTypography.bodySmall,
                color = colors.textMuted,
                modifier = Modifier.padding(horizontal = SuhoodSpacing.screenHorizontal)
            )
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SuhoodSpacing.screenHorizontal, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${selected.size} selected",
                    style = SuhoodTypography.labelMedium,
                    color = colors.silverBright
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Clear all",
                        style = SuhoodTypography.labelMedium,
                        color = colors.textMuted,
                        modifier = Modifier.clickable {
                            selected = emptySet()
                            controller.setBlockedApps(emptySet())
                        }
                    )
                    Text(
                        "Select all",
                        style = SuhoodTypography.labelMedium,
                        color = colors.silverBright,
                        modifier = Modifier.clickable {
                            selected = installed.map { it.packageName }.toSet()
                            controller.setBlockedApps(selected)
                        }
                    )
                }
            }
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = SuhoodSpacing.screenHorizontal)
                    .padding(bottom = 24.dp)
            ) {
                if (installed.isEmpty()) {
                    Text("No launcher apps found", color = colors.textMuted)
                }
                installed.forEach { app ->
                    val on = app.packageName in selected
                    GlassCard(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        selected = on,
                        onClick = {
                            selected = controller.toggleBlockedApp(app.packageName)
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val bmp = remember(app.packageName) {
                                app.icon?.let { drawableToBitmap(it) }
                            }
                            if (bmp != null) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp)
                                )
                            } else {
                                Box(Modifier.size(40.dp))
                            }
                            Column(Modifier.weight(1f)) {
                                Text(
                                    app.label,
                                    style = SuhoodTypography.titleSmall,
                                    color = colors.textPrimary
                                )
                                Text(
                                    if (on) "Limited during focus" else "Allowed",
                                    style = SuhoodTypography.labelSmall,
                                    color = if (on) colors.silverBright else colors.textMuted
                                )
                            }
                            Switch(
                                checked = on,
                                onCheckedChange = {
                                    selected = controller.toggleBlockedApp(app.packageName)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedTrackColor = Color(0xFF3B82F6),
                                    checkedThumbColor = Color.White
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) return drawable.bitmap
        val w = drawable.intrinsicWidth.coerceAtLeast(1)
        val h = drawable.intrinsicHeight.coerceAtLeast(1)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bmp
    }

    @Composable
    fun DurationSelect(onBack: () -> Unit, onConfirm: (Int) -> Unit) {
        val colors = suhoodColors
        val options = listOf(5, 10, 15, 20, 30, 45, 60)
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SuhoodSpacing.screenHorizontal)
        ) {
            ScreenHeader(title = "Duration", onBack = onBack)
            options.forEach { m ->
                GlassCard(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    onClick = { onConfirm(m) }
                ) {
                    Text("$m minutes", style = SuhoodTypography.titleMedium, color = colors.textPrimary)
                }
            }
        }
    }

    @Composable
    fun ActiveSession(durationMinutes: Int, onComplete: () -> Unit, onCancel: () -> Unit) {
        val context = LocalContext.current
        val controller = remember { FocusSessionController.get(context) }
        var remaining by remember { mutableIntStateOf(durationMinutes * 60) }
        val colors = suhoodColors

        LaunchedEffect(durationMinutes) {
            controller.start(durationMinutes)
            while (true) {
                kotlinx.coroutines.delay(1000)
                val s = controller.tick()
                remaining = s.remainingSeconds
                if (s.remainingSeconds <= 0) {
                    controller.complete()
                    onComplete()
                    break
                }
            }
        }

        Column(
            Modifier.fillMaxSize().statusBarsPadding().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Focus active", style = SuhoodTypography.headlineMedium, color = colors.textPrimary)
            Spacer(Modifier.height(12.dp))
            Text(formatTime(remaining), style = SuhoodTypography.headlineLarge, color = colors.silverBright)
            Spacer(Modifier.height(24.dp))
            GlassButton(text = "End session", onClick = {
                controller.cancel()
                onCancel()
            }, primary = false)
        }
    }

    @Composable
    fun SessionComplete(onDone: () -> Unit, durationMinutes: Int = 15) {
        val colors = suhoodColors
        Column(
            Modifier.fillMaxSize().statusBarsPadding().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Session complete", style = SuhoodTypography.headlineMedium, color = colors.textPrimary)
            Text("$durationMinutes minutes", color = colors.textSecondary)
            Spacer(Modifier.height(16.dp))
            GlassButton(text = "Done", onClick = onDone)
        }
    }

    @Composable
    fun History(onBack: () -> Unit) {
        val colors = suhoodColors
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = SuhoodSpacing.screenHorizontal)) {
            ScreenHeader(title = "History", onBack = onBack)
            Text(
                "Completed sessions are stored on this device when the session finishes.",
                style = SuhoodTypography.bodySmall,
                color = colors.textMuted
            )
        }
    }

    @Composable
    fun TimeForSalah(onDismiss: () -> Unit) {
        // Kept for nav compatibility; not linked from dashboard
        val colors = suhoodColors
        Column(
            Modifier.fillMaxSize().statusBarsPadding().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Return to focus", style = SuhoodTypography.headlineMedium, color = colors.textPrimary)
            Spacer(Modifier.height(16.dp))
            GlassButton(text = "Dismiss", onClick = onDismiss)
        }
    }

    private fun formatTime(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return "%02d:%02d".format(m, s)
    }
}

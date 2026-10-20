package com.suhood.feature.focus

import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
        val controller = remember { FocusSessionController.get(context) }
        var session by remember { mutableStateOf(controller.tick()) }
        LaunchedEffect(Unit) {
            while (true) {
                delay(1000)
                session = controller.tick()
            }
        }
        val totalSec = (session.durationMinutes.coerceAtLeast(1)) * 60
        val remaining = session.remainingSeconds.coerceAtLeast(0)
        val progress = if (session.state.name == "IDLE" || totalSec <= 0) 0f
        else (1f - remaining.toFloat() / totalSec).coerceIn(0f, 1f)

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SuhoodSpacing.screenHorizontal)
                .padding(bottom = bottomContentPadding + 24.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Suhood · Focus",
                style = SuhoodTypography.labelMedium,
                color = colors.textMuted
            )
            Text(
                "تركيز",
                style = SuhoodTypography.arabicTitle,
                color = colors.accentLavender
            )
            Text(
                "Focus",
                style = SuhoodTypography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = colors.textPrimary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Protect your time before Salah with a timed session and optional app limits.",
                style = SuhoodTypography.bodyMedium,
                color = colors.textSecondary
            )
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(colors.surfaceElevated)
                    .border(1.dp, colors.borderSubtle, RoundedCornerShape(999.dp))
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(
                    "Usage Access · Accessibility optional",
                    style = SuhoodTypography.labelSmall,
                    color = colors.textMuted
                )
            }

            Spacer(Modifier.height(22.dp))
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Session", style = SuhoodTypography.labelSmall, color = colors.textMuted)
                    Spacer(Modifier.height(12.dp))
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
                        ComposeCanvas(Modifier.size(200.dp)) {
                            val stroke = 6.dp.toPx()
                            val r = size.minDimension / 2 - stroke
                            drawCircle(
                                color = colors.borderSubtle.copy(alpha = 0.4f),
                                radius = r,
                                style = Stroke(width = stroke)
                            )
                            if (progress > 0f) {
                                drawArc(
                                    color = colors.accentLavender,
                                    startAngle = -90f,
                                    sweepAngle = 360f * progress,
                                    useCenter = false,
                                    topLeft = Offset(stroke, stroke),
                                    size = Size(size.width - stroke * 2, size.height - stroke * 2),
                                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                session.state.name.replace('_', ' '),
                                style = SuhoodTypography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp
                                ),
                                color = colors.accentLavender
                            )
                            Text(
                                if (remaining > 0) formatTime(remaining) else formatTime(totalSec),
                                style = SuhoodTypography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                                color = colors.textPrimary
                            )
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    GlassButton(
                        text = if (session.state.name == "RUNNING") "Session running…" else "Start focus session",
                        onClick = {
                            when (session.state.name) {
                                "RUNNING" -> onNavigate(Routes.FOCUS_ACTIVE)
                                "PAUSED" -> onNavigate(Routes.FOCUS_ACTIVE)
                                else -> onNavigate(Routes.FOCUS_DURATION)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GlassCard(
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Routes.FOCUS_SELECT_APPS) },
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Box(
                        Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.accentLavender.copy(alpha = 0.15f))
                            .border(1.dp, colors.accentLavender.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Apps, null, tint = colors.accentLavender, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("Apps", style = SuhoodTypography.titleSmall.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary)
                    Text("Limit list", style = SuhoodTypography.labelSmall, color = colors.textMuted)
                }
                GlassCard(
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Routes.FOCUS_HISTORY) },
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Box(
                        Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.silver.copy(alpha = 0.15f))
                            .border(1.dp, colors.silver.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.History, null, tint = colors.silverBright, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("History", style = SuhoodTypography.titleSmall.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary)
                    Text("Past sessions", style = SuhoodTypography.labelSmall, color = colors.textMuted)
                }
            }

            Spacer(Modifier.height(14.dp))
            GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Info, null, tint = colors.accentLavender, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "How it works",
                        style = SuhoodTypography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )
                }
                Spacer(Modifier.height(12.dp))
                listOf(
                    "Choose a duration for your session",
                    "Optionally select apps to limit",
                    "Enable Usage Access / Accessibility when prompted",
                    "Stay with the session until it completes"
                ).forEachIndexed { i, line ->
                    Row(
                        Modifier.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            Modifier
                                .size(22.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(colors.accentLavender.copy(alpha = 0.12f))
                                .border(1.dp, colors.accentLavender.copy(alpha = 0.3f), RoundedCornerShape(7.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${i + 1}",
                                style = SuhoodTypography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = colors.accentLavender
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(line, style = SuhoodTypography.bodySmall, color = colors.textSecondary)
                    }
                }
            }
        }
    }

    @Composable
    fun SelectApps(onBack: () -> Unit) {
        val colors = suhoodColors
        val context = LocalContext.current
        val controller = remember { FocusSessionController.get(context) }
        val installed = remember {
            runCatching { InstalledAppsRepository.loadLauncherApps(context) }
                .getOrDefault(emptyList())
        }
        var selected by remember { mutableStateOf(controller.getBlockedApps()) }
        var query by remember { mutableStateOf("") }
        val filtered = remember(query, installed) {
            if (query.isBlank()) installed
            else installed.filter {
                it.label.contains(query, ignoreCase = true) ||
                    it.packageName.contains(query, ignoreCase = true)
            }
        }

        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            ScreenHeader(title = "Distracting apps", onBack = onBack)
            Text(
                "Enable apps to limit during focus. Your choices are saved.",
                style = SuhoodTypography.bodySmall,
                color = colors.textMuted,
                modifier = Modifier.padding(horizontal = SuhoodSpacing.screenHorizontal)
            )
            Spacer(Modifier.height(8.dp))
            GlassSearchField(
                query = query,
                onQueryChange = { query = it },
                placeholder = "Search apps…",
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
                    color = colors.accentLavender
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
                        color = colors.accentLavender,
                        modifier = Modifier.clickable {
                            val all = installed.map { it.packageName }.toSet()
                            selected = all
                            controller.setBlockedApps(all)
                        }
                    )
                }
            }
            if (installed.isEmpty()) {
                Text(
                    "No launcher apps could be listed. Android may restrict package visibility on this device.",
                    color = colors.textMuted,
                    modifier = Modifier.padding(24.dp)
                )
            } else {
                LazyColumn(
                    Modifier
                        .weight(1f)
                        .padding(horizontal = SuhoodSpacing.screenHorizontal)
                        .padding(bottom = 24.dp)
                ) {
                    items(filtered, key = { it.packageName }) { app ->
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
                                    Box(
                                        Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(colors.surfaceSelected),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            app.label.take(1).uppercase(),
                                            style = SuhoodTypography.labelMedium,
                                            color = colors.textSecondary
                                        )
                                    }
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
                                        color = if (on) colors.accentLavender else colors.textMuted
                                    )
                                }
                                Switch(
                                    checked = on,
                                    onCheckedChange = {
                                        selected = controller.toggleBlockedApp(app.packageName)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap? {
        return try {
            if (drawable is BitmapDrawable && drawable.bitmap != null && !drawable.bitmap.isRecycled) {
                return drawable.bitmap
            }
            val w = drawable.intrinsicWidth.coerceIn(1, 192)
            val h = drawable.intrinsicHeight.coerceIn(1, 192)
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bmp
        } catch (_: Exception) {
            null
        }
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

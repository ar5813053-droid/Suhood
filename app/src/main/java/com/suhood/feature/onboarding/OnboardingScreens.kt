package com.suhood.feature.onboarding

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.suhood.core.designsystem.components.GlassButton
import com.suhood.core.designsystem.components.GlassCard
import com.suhood.core.designsystem.components.GlassSegmentedControl
import com.suhood.core.designsystem.theme.SuhoodSpacing
import com.suhood.core.designsystem.theme.SuhoodTypography
import com.suhood.core.designsystem.theme.ThemeMode
import com.suhood.core.designsystem.theme.suhoodColors
import com.suhood.feature.settings.SettingsViewModel
import com.suhood.core.notifications.QuranReminderScheduler
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.layout.PaddingValues

private const val TOTAL_STEPS = 7 // 0-3 intro, 4 quran goal, 5 permissions, 6 jazakallah

@Composable
fun OnboardingFlow(
    onComplete: () -> Unit,
    settingsViewModel: SettingsViewModel
) {
    var step by remember { mutableIntStateOf(0) }
    val colors = suhoodColors

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            colors.surfaceElevated.copy(alpha = 0.35f),
                            colors.background,
                            colors.background
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = SuhoodSpacing.xl)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OnboardingProgress(step = step, total = TOTAL_STEPS, modifier = Modifier.weight(1f))
                // Skip only before permissions; permissions page has its own Skip
                if (step < 5) {
                    Spacer(Modifier.width(12.dp))
                    GlassButton(
                        text = "Skip",
                        onClick = { step = 5 }, // jump to permissions
                        primary = false,
                        modifier = Modifier.height(36.dp)
                    )
                }
            }

            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    val forward = targetState > initialState
                    (fadeIn(tween(160)) + slideInHorizontally(tween(180)) {
                        if (forward) it / 8 else -it / 8
                    }) togetherWith (fadeOut(tween(120)) + slideOutHorizontally(tween(160)) {
                        if (forward) -it / 10 else it / 10
                    })
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                label = "onboardingPage"
            ) { page ->
                when (page) {
                    0 -> OnboardingPage(
                        illustration = { CrescentIllustration() },
                        title = "Make space for what matters.",
                        body = "A calmer way to reconnect with Salah, Quran and the habits that bring you closer to Allah."
                    )
                    1 -> OnboardingPage(
                        illustration = { FocusIllustration() },
                        title = "Small steps. Meaningful consistency.",
                        body = "Keep track of your worship routine, Dhikr and personal goals with a gentle, distraction-free experience."
                    )
                    2 -> LanguagePage()
                    3 -> AppearancePage(onSelect = { settingsViewModel.setThemeMode(it) })
                    4 -> QuranGoalPage(
                        settingsViewModel = settingsViewModel,
                        onContinue = { step = 5 },
                        onSkip = { step = 5 }
                    )
                    5 -> PermissionsPage(
                        onContinue = { step = 6 },
                        onSkip = { step = 6 }
                    )
                    else -> JazakallahPage(onFinished = onComplete)
                }
            }

            // Primary CTA — hidden on quran goal, permissions, jazakallah
            if (step < 4) {
                val cta = when (step) {
                    0 -> "Get Started"
                    else -> "Continue"
                }
                GlassButton(
                    text = cta,
                    onClick = { step++ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp, top = 8.dp)
                )
            } else if (step == 6) {
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun OnboardingProgress(step: Int, total: Int, modifier: Modifier = Modifier) {
    val colors = suhoodColors
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(total) { i ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(
                        if (i <= step) colors.accentLavender else colors.surfaceSelected
                    )
            )
        }
    }
}

@Composable
private fun OnboardingPage(
    illustration: @Composable () -> Unit,
    title: String,
    body: String
) {
    val colors = suhoodColors
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        illustration()
        Spacer(Modifier.height(32.dp))
        Text(
            title,
            style = SuhoodTypography.headlineLarge,
            color = colors.textPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            body,
            style = SuhoodTypography.bodyLarge,
            color = colors.textSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun LanguagePage() {
    val colors = suhoodColors
    var selected by remember { mutableIntStateOf(0) }
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GlobeIllustration()
        Spacer(Modifier.height(28.dp))
        Text("Language", style = SuhoodTypography.headlineLarge, color = colors.textPrimary)
        Spacer(Modifier.height(8.dp))
        Text(
            "Choose your interface language. Arabic layout support improves in later phases.",
            style = SuhoodTypography.bodyMedium,
            color = colors.textSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        GlassSegmentedControl(
            options = listOf("English", "العربية"),
            selectedIndex = selected,
            onSelect = { selected = it },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun AppearancePage(onSelect: (ThemeMode) -> Unit) {
    val colors = suhoodColors
    var selected by remember { mutableIntStateOf(0) }
    val modes = listOf(ThemeMode.SYSTEM, ThemeMode.DARK, ThemeMode.LIGHT)
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PaletteIllustration()
        Spacer(Modifier.height(28.dp))
        Text("Appearance", style = SuhoodTypography.headlineLarge, color = colors.textPrimary)
        Spacer(Modifier.height(8.dp))
        Text(
            "Pick a theme that feels calm and readable.",
            style = SuhoodTypography.bodyMedium,
            color = colors.textSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        GlassSegmentedControl(
            options = listOf("System", "Dark", "Light"),
            selectedIndex = selected,
            onSelect = {
                selected = it
                onSelect(modes[it])
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/* ── Lightweight Canvas illustrations (no network assets) ── */

@Composable
private fun CrescentIllustration() {
    val colors = suhoodColors
    Canvas(Modifier.size(160.dp)) {
        val c = Offset(size.width * 0.48f, size.height * 0.5f)
        val r = size.minDimension * 0.32f
        drawCircle(color = colors.silver.copy(alpha = 0.15f), radius = r * 1.55f, center = c)
        drawCircle(color = colors.silverBright.copy(alpha = 0.9f), radius = r, center = c)
        drawCircle(color = colors.background, radius = r * 0.78f, center = c + Offset(r * 0.38f, -r * 0.12f))
        // soft stars
        listOf(
            Offset(size.width * 0.78f, size.height * 0.28f),
            Offset(size.width * 0.82f, size.height * 0.55f),
            Offset(size.width * 0.18f, size.height * 0.32f)
        ).forEach { drawCircle(colors.silverBright.copy(alpha = 0.7f), radius = 3.dp.toPx(), center = it) }
    }
}

@Composable
private fun FocusIllustration() {
    val colors = suhoodColors
    Canvas(Modifier.size(160.dp)) {
        val cx = size.width / 2
        val cy = size.height / 2
        // rings
        for (i in 1..3) {
            drawCircle(
                color = colors.silver.copy(alpha = 0.2f * (4 - i)),
                radius = size.minDimension * (0.18f + i * 0.1f),
                center = Offset(cx, cy),
                style = Stroke(width = 2.dp.toPx())
            )
        }
        drawCircle(colors.silverBright.copy(alpha = 0.85f), radius = size.minDimension * 0.12f, center = Offset(cx, cy))
        // phone silhouette blocked
        val path = Path().apply {
            val w = size.width * 0.18f
            val h = size.height * 0.32f
            val left = size.width * 0.72f
            val top = size.height * 0.34f
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    left, top, left + w, top + h,
                    androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())
                )
            )
        }
        drawPath(path, colors.surfaceSelected)
        drawLine(
            colors.error.copy(alpha = 0.85f),
            start = Offset(size.width * 0.74f, size.height * 0.4f),
            end = Offset(size.width * 0.88f, size.height * 0.62f),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun GlobeIllustration() {
    val colors = suhoodColors
    Canvas(Modifier.size(140.dp)) {
        val c = Offset(size.width / 2, size.height / 2)
        val r = size.minDimension * 0.38f
        drawCircle(colors.surfaceSelected, radius = r, center = c)
        drawCircle(colors.silver.copy(alpha = 0.5f), radius = r, center = c, style = Stroke(2.dp.toPx()))
        // meridians
        drawArc(
            colors.silver.copy(alpha = 0.45f),
            startAngle = -90f, sweepAngle = 180f, useCenter = false,
            topLeft = Offset(c.x - r * 0.45f, c.y - r),
            size = Size(r * 0.9f, r * 2),
            style = Stroke(1.5.dp.toPx())
        )
        drawLine(colors.silver.copy(alpha = 0.4f), Offset(c.x - r, c.y), Offset(c.x + r, c.y), 1.5.dp.toPx())
        drawLine(colors.silver.copy(alpha = 0.35f), Offset(c.x - r * 0.85f, c.y - r * 0.4f), Offset(c.x + r * 0.85f, c.y - r * 0.4f), 1.dp.toPx())
    }
}

@Composable
private fun PaletteIllustration() {
    val colors = suhoodColors
    Canvas(Modifier.size(140.dp)) {
        val spots = listOf(
            colors.silverBright to Offset(size.width * 0.35f, size.height * 0.4f),
            colors.silver to Offset(size.width * 0.58f, size.height * 0.38f),
            colors.surfaceSelected to Offset(size.width * 0.48f, size.height * 0.58f),
            colors.textMuted to Offset(size.width * 0.68f, size.height * 0.55f)
        )
        spots.forEach { (col, off) ->
            drawCircle(col.copy(alpha = 0.9f), radius = size.minDimension * 0.16f, center = off)
        }
    }
}

@Composable
private fun CheckIllustration() {
    val colors = suhoodColors
    Canvas(Modifier.size(140.dp)) {
        val c = Offset(size.width / 2, size.height / 2)
        val r = size.minDimension * 0.36f
        drawCircle(colors.success.copy(alpha = 0.2f), radius = r * 1.15f, center = c)
        drawCircle(colors.success.copy(alpha = 0.85f), radius = r, center = c)
        // check mark
        val path = Path().apply {
            moveTo(c.x - r * 0.35f, c.y)
            lineTo(c.x - r * 0.08f, c.y + r * 0.28f)
            lineTo(c.x + r * 0.4f, c.y - r * 0.28f)
        }
        drawPath(path, Color.White, style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round))
    }
}

/**
 * Claude-style permission cards, Suhood Hybrid Glass theme.
 * Only permissions that improve core UX: notifications, exact alarms, music library.
 */

@Composable
private fun GreenTickIllustration() {
    // Soft green tick for Jazakallah celebration (not brand lavender)
    val green = Color(0xFF34D399)
    Canvas(Modifier.size(140.dp)) {
        val c = Offset(size.width / 2, size.height / 2)
        val r = size.minDimension * 0.36f
        drawCircle(green.copy(alpha = 0.22f), radius = r * 1.2f, center = c)
        drawCircle(green.copy(alpha = 0.9f), radius = r, center = c)
        val path = Path().apply {
            moveTo(c.x - r * 0.35f, c.y)
            lineTo(c.x - r * 0.08f, c.y + r * 0.28f)
            lineTo(c.x + r * 0.4f, c.y - r * 0.28f)
        }
        drawPath(path, Color.White, style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round))
    }
}


@Composable
private fun QuranGoalPage(
    settingsViewModel: SettingsViewModel,
    onContinue: () -> Unit,
    onSkip: () -> Unit
) {
    val colors = suhoodColors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var minutes by remember { mutableIntStateOf(10) }
    var hour by remember { mutableIntStateOf(9) }
    var remind by remember { mutableStateOf(true) }
    val options = listOf(5, 10, 20)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        Text(
            "Quran Companion",
            style = SuhoodTypography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = colors.textPrimary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "How much time would you like to spend with the Quran daily?",
            style = SuhoodTypography.bodyMedium,
            color = colors.textSecondary
        )
        Spacer(Modifier.height(18.dp))
        options.forEach { m ->
            val sel = minutes == m
            GlassCard(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                onClick = { minutes = m },
                selected = sel,
                contentPadding = PaddingValues(16.dp)
            ) {
                Text(
                    "$m minutes",
                    style = SuhoodTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = if (sel) colors.accentLavender else colors.textPrimary
                )
                Text(
                    when (m) {
                        5 -> "About 2 Mushaf pages · gentle pace"
                        10 -> "About 4 Mushaf pages · steady"
                        else -> "About 8 Mushaf pages · focused"
                    },
                    style = SuhoodTypography.bodySmall,
                    color = colors.textMuted
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("Daily reminder", style = SuhoodTypography.titleSmall, color = colors.textMuted)
        Spacer(Modifier.height(8.dp))
        GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (remind) "Reminder on" else "Reminder off",
                    style = SuhoodTypography.titleSmall,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                androidx.compose.material3.Switch(
                    checked = remind,
                    onCheckedChange = { remind = it }
                )
            }
            if (remind) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "Pick a time. Notification permission is requested on the next step.",
                    style = SuhoodTypography.bodySmall,
                    color = colors.textMuted
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(6, 9, 12, 18, 21).forEach { h ->
                        val sel = hour == h
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (sel) colors.accentLavender.copy(alpha = 0.2f) else colors.surfaceElevated)
                                .border(1.dp, if (sel) colors.accentLavender else colors.borderSubtle, RoundedCornerShape(12.dp))
                                .clickable { hour = h }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                "%02d:00".format(h),
                                color = if (sel) colors.accentLavender else colors.textSecondary,
                                style = SuhoodTypography.labelMedium
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        GlassButton(
            text = "Continue",
            onClick = {
                scope.launch {
                    settingsViewModel.setQuranCompanion(minutes, hour, 0, remind)
                    QuranReminderScheduler.cachePrefs(context, hour, 0, remind)
                    onContinue()
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        GlassButton(
            text = "Skip for now",
            onClick = {
                scope.launch {
                    settingsViewModel.setQuranCompanion(10, 9, 0, false)
                    QuranReminderScheduler.cachePrefs(context, 9, 0, false)
                    onSkip()
                }
            },
            primary = false,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PermissionsPage(
    onContinue: () -> Unit,
    onSkip: () -> Unit
) {
    val colors = suhoodColors
    val context = LocalContext.current
    var notifGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT < 33) true
            else ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var audioGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= 33) {
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.READ_MEDIA_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
            } else if (Build.VERSION.SDK_INT >= 23) {
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }
    var alarmOk by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= 31) {
                val am = context.getSystemService(AlarmManager::class.java)
                am?.canScheduleExactAlarms() == true
            } else true
        )
    }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> notifGranted = granted }

    val audioLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> audioGranted = granted }

    val alarmLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        alarmOk = if (Build.VERSION.SDK_INT >= 31) {
            context.getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() == true
        } else true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(12.dp))
        Text(
            "Enable\npermissions",
            style = SuhoodTypography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 34.sp,
                lineHeight = 40.sp
            ),
            color = colors.textPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Suhood works best with these permissions",
            style = SuhoodTypography.bodyLarge,
            color = colors.textSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))

        PermissionCard(
            icon = Icons.Filled.Notifications,
            title = "Notifications",
            body = "Prayer reminders, alarms and Focus session alerts when it is time to return to Allah.",
            enabled = notifGranted,
            onClick = {
                if (Build.VERSION.SDK_INT >= 33 && !notifGranted) {
                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        )
        Spacer(Modifier.height(12.dp))
        PermissionCard(
            icon = Icons.Filled.Alarm,
            title = "Alarms & reminders",
            body = "Exact alarms so Fajr and custom wake-up times ring on time, even in Do Not Disturb.",
            enabled = alarmOk,
            onClick = {
                if (Build.VERSION.SDK_INT >= 31 && !alarmOk) {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = android.net.Uri.parse("package:${context.packageName}")
                    }
                    runCatching { alarmLauncher.launch(intent) }
                }
            }
        )
        Spacer(Modifier.height(12.dp))
        PermissionCard(
            icon = Icons.Filled.MusicNote,
            title = "Music & audio",
            body = "Play nasheeds and tilawat from your device library in Nasheedify.",
            enabled = audioGranted,
            onClick = {
                if (audioGranted) return@PermissionCard
                if (Build.VERSION.SDK_INT >= 33) {
                    audioLauncher.launch(Manifest.permission.READ_MEDIA_AUDIO)
                } else {
                    audioLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
        )

        Spacer(Modifier.height(28.dp))
        GlassButton(
            text = "Continue",
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .border(1.dp, colors.borderSubtle, RoundedCornerShape(28.dp))
                .clickable(onClick = onSkip)
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Skip for now",
                style = SuhoodTypography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = colors.textSecondary
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "You can change these later in Settings → Permissions.",
            style = SuhoodTypography.labelSmall,
            color = colors.textMuted,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    body: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val colors = suhoodColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surfaceElevated.copy(alpha = 0.95f))
            .border(1.dp, colors.borderSubtle.copy(alpha = 0.8f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.accentLavender.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            if (enabled) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = colors.success, modifier = Modifier.size(24.dp))
            } else {
                Icon(icon, contentDescription = null, tint = colors.accentLavender, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = SuhoodTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = colors.textPrimary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                body,
                style = SuhoodTypography.bodySmall,
                color = colors.textSecondary
            )
            if (enabled) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Enabled",
                    style = SuhoodTypography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.success
                )
            }
        }
    }
}

@Composable
private fun JazakallahPage(onFinished: () -> Unit) {
    val colors = suhoodColors
    val scale = remember { Animatable(0.4f) }
    LaunchedEffect(Unit) {
        scale.animateTo(
            1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        delay(1600)
        onFinished()
    }
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(modifier = Modifier.scale(scale.value)) {
            GreenTickIllustration()
        }
        Spacer(Modifier.height(28.dp))
        Text(
            "JazakAllah Khair",
            style = SuhoodTypography.headlineLarge.copy(fontWeight = FontWeight.Bold),
            color = colors.textPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "You're all set. May Allah put barakah in your time.",
            style = SuhoodTypography.bodyLarge,
            color = colors.textSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        GlassButton(
            text = "Enter Suhood",
            onClick = onFinished,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

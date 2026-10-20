package com.suhood.feature.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.suhood.core.designsystem.components.GlassButton
import com.suhood.core.designsystem.components.GlassSegmentedControl
import com.suhood.core.designsystem.theme.SuhoodSpacing
import com.suhood.core.designsystem.theme.SuhoodTypography
import com.suhood.core.designsystem.theme.ThemeMode
import com.suhood.core.designsystem.theme.suhoodColors
import com.suhood.feature.settings.SettingsViewModel

private const val TOTAL_STEPS = 5

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
        // Soft background glow
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
            // Top bar: progress + skip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OnboardingProgress(step = step, total = TOTAL_STEPS, modifier = Modifier.weight(1f))
                if (step < TOTAL_STEPS - 1) {
                    Spacer(Modifier.width(12.dp))
                    GlassButton(
                        text = "Skip",
                        onClick = onComplete,
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
                    else -> OnboardingPage(
                        illustration = { CheckIllustration() },
                        title = "Protect your time. Find your focus.",
                        body = "Use Focus sessions, reminders and peaceful Islamic audio to make room for what matters. Permissions are asked only when a feature needs them."
                    )
                }
            }

            // Primary CTA
            val cta = when (step) {
                0 -> "Get Started"
                TOTAL_STEPS - 1 -> "Start your journey"
                else -> "Continue"
            }
            GlassButton(
                text = cta,
                onClick = {
                    if (step >= TOTAL_STEPS - 1) onComplete()
                    else step++
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp, top = 8.dp)
            )
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

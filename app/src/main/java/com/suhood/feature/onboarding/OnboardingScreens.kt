package com.suhood.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.suhood.core.designsystem.components.GlassButton
import com.suhood.core.designsystem.components.GlassCard
import com.suhood.core.designsystem.components.GlassSegmentedControl
import com.suhood.core.designsystem.theme.SuhoodSpacing
import com.suhood.core.designsystem.theme.SuhoodTypography
import com.suhood.core.designsystem.theme.ThemeMode
import com.suhood.core.designsystem.theme.suhoodColors
import com.suhood.feature.settings.SettingsViewModel

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
            .statusBarsPadding()
            .padding(SuhoodSpacing.xl)
    ) {
        when (step) {
            0 -> WelcomeStep(onNext = { step = 1 }, onSkip = onComplete)
            1 -> MissionStep(onNext = { step = 2 }, onSkip = onComplete)
            2 -> LanguageStep(onNext = { step = 3 }, onSkip = { step = 3 })
            3 -> AppearanceStep(
                onSelect = { mode -> settingsViewModel.setThemeMode(mode) },
                onNext = { step = 4 },
                onSkip = { step = 4 }
            )
            4 -> PrayerIntroStep(onNext = { step = 5 }, onSkip = { step = 5 })
            5 -> NotificationEduStep(onNext = { step = 6 }, onSkip = { step = 6 })
            6 -> CompleteStep(onFinish = onComplete)
        }
    }
}

@Composable
private fun WelcomeStep(onNext: () -> Unit, onSkip: () -> Unit) {
    val colors = suhoodColors
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Suhood", style = SuhoodTypography.displaySmall, color = colors.textPrimary)
        Spacer(Modifier.height(12.dp))
        Text(
            "Return to presence.\nReduce distraction.\nReconnect with Allah.",
            style = SuhoodTypography.bodyLarge,
            color = colors.textSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(40.dp))
        GlassButton(text = "Begin", onClick = onNext, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        GlassButton(text = "Skip intro", onClick = onSkip, primary = false, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun MissionStep(onNext: () -> Unit, onSkip: () -> Unit) {
    val colors = suhoodColors
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Text("Our purpose", style = SuhoodTypography.headlineLarge, color = colors.textPrimary)
        Spacer(Modifier.height(16.dp))
        Text(
            "Suhood helps Muslims spend less time on distracting apps and more time with Salah, Quran, Dhikr, and beneficial Islamic audio.\n\nThis is a free, non-commercial project. No ads. No subscriptions.",
            style = SuhoodTypography.bodyLarge,
            color = colors.textSecondary
        )
        Spacer(Modifier.height(32.dp))
        GlassButton(text = "Continue", onClick = onNext, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        GlassButton(text = "Skip", onClick = onSkip, primary = false, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun LanguageStep(onNext: () -> Unit, onSkip: () -> Unit) {
    val colors = suhoodColors
    var selected by remember { mutableIntStateOf(0) }
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Text("Language", style = SuhoodTypography.headlineLarge, color = colors.textPrimary)
        Spacer(Modifier.height(8.dp))
        Text("Choose your preferred interface language.", style = SuhoodTypography.bodyMedium, color = colors.textSecondary)
        Spacer(Modifier.height(24.dp))
        GlassSegmentedControl(
            options = listOf("English", "العربية"),
            selectedIndex = selected,
            onSelect = { selected = it }
        )
        Spacer(Modifier.height(32.dp))
        GlassButton(text = "Continue", onClick = onNext, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun AppearanceStep(
    onSelect: (ThemeMode) -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    val colors = suhoodColors
    var selected by remember { mutableIntStateOf(0) }
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Text("Appearance", style = SuhoodTypography.headlineLarge, color = colors.textPrimary)
        Spacer(Modifier.height(8.dp))
        Text("Dark is recommended for calm focus.", style = SuhoodTypography.bodyMedium, color = colors.textSecondary)
        Spacer(Modifier.height(24.dp))
        GlassSegmentedControl(
            options = listOf("Dark", "Light", "System"),
            selectedIndex = selected,
            onSelect = {
                selected = it
                onSelect(when (it) { 1 -> ThemeMode.LIGHT; 2 -> ThemeMode.SYSTEM; else -> ThemeMode.DARK })
            }
        )
        Spacer(Modifier.height(32.dp))
        GlassButton(text = "Continue", onClick = onNext, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun PrayerIntroStep(onNext: () -> Unit, onSkip: () -> Unit) {
    val colors = suhoodColors
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Text("Salah first", style = SuhoodTypography.headlineLarge, color = colors.textPrimary)
        Spacer(Modifier.height(16.dp))
        GlassCard {
            Text(
                "In Phase 1 you will see demo prayer times. Real location-based calculation and notifications arrive in Phase 2.",
                style = SuhoodTypography.bodyMedium,
                color = colors.textSecondary
            )
        }
        Spacer(Modifier.height(32.dp))
        GlassButton(text = "Continue", onClick = onNext, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun NotificationEduStep(onNext: () -> Unit, onSkip: () -> Unit) {
    val colors = suhoodColors
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Text("Gentle reminders", style = SuhoodTypography.headlineLarge, color = colors.textPrimary)
        Spacer(Modifier.height(16.dp))
        Text(
            "Suhood will later support prayer notifications and Fajr alarms. No permissions are requested in this demo phase.",
            style = SuhoodTypography.bodyMedium,
            color = colors.textSecondary
        )
        Spacer(Modifier.height(32.dp))
        GlassButton(text = "Continue", onClick = onNext, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun CompleteStep(onFinish: () -> Unit) {
    val colors = suhoodColors
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("You are ready", style = SuhoodTypography.headlineLarge, color = colors.textPrimary)
        Spacer(Modifier.height(12.dp))
        Text(
            "May Allah make it easy to remember Him throughout your day.",
            style = SuhoodTypography.bodyLarge,
            color = colors.textSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(40.dp))
        GlassButton(text = "Enter Suhood", onClick = onFinish, modifier = Modifier.fillMaxWidth())
    }
}

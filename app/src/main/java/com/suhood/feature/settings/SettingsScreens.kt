package com.suhood.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suhood.core.designsystem.components.*
import com.suhood.core.designsystem.theme.SuhoodSpacing
import com.suhood.core.designsystem.theme.SuhoodTypography
import com.suhood.core.designsystem.theme.ThemeMode
import com.suhood.core.designsystem.theme.suhoodColors
import com.suhood.core.navigation.Routes

object SettingsScreens {

    @Composable
    fun Home(onBack: () -> Unit, onNavigate: (String) -> Unit) {
        val colors = suhoodColors
        val items = listOf(
            "Appearance" to Routes.SETTINGS_APPEARANCE,
            "Language" to Routes.SETTINGS_LANGUAGE,
            "Prayer preferences" to Routes.SETTINGS_PRAYER,
            "Notifications" to Routes.SETTINGS_NOTIFICATIONS,
            "Focus" to Routes.SETTINGS_FOCUS,
            "Audio" to Routes.SETTINGS_AUDIO,
            "Accessibility" to Routes.SETTINGS_ACCESSIBILITY,
            "Privacy" to Routes.SETTINGS_PRIVACY,
            "About Suhood" to Routes.SETTINGS_ABOUT
        )
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            ScreenHeader(title = "Settings", onBack = onBack)
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = SuhoodSpacing.screenHorizontal)
                    .padding(bottom = 24.dp)
            ) {
                items.forEach { (label, route) ->
                    GlassCard(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        onClick = { onNavigate(route) }
                    ) {
                        Text(label, style = SuhoodTypography.titleMedium, color = colors.textPrimary)
                    }
                }
            }
        }
    }

    @Composable
    fun Appearance(settingsViewModel: SettingsViewModel, onBack: () -> Unit) {
        val colors = suhoodColors
        val mode by settingsViewModel.themeMode.collectAsStateWithLifecycle()
        val selected = when (mode) {
            ThemeMode.DARK -> 0
            ThemeMode.LIGHT -> 1
            ThemeMode.SYSTEM -> 2
        }
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = SuhoodSpacing.screenHorizontal)
        ) {
            ScreenHeader(title = "Appearance", onBack = onBack)
            Spacer(Modifier.height(16.dp))
            Text("Theme", style = SuhoodTypography.titleSmall, color = colors.textMuted)
            Spacer(Modifier.height(8.dp))
            GlassSegmentedControl(
                options = listOf("Dark", "Light", "System"),
                selectedIndex = selected,
                onSelect = {
                    settingsViewModel.setThemeMode(
                        when (it) {
                            1 -> ThemeMode.LIGHT
                            2 -> ThemeMode.SYSTEM
                            else -> ThemeMode.DARK
                        }
                    )
                }
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Changes apply immediately across the entire app.",
                style = SuhoodTypography.bodySmall,
                color = colors.textSecondary
            )
        }
    }

    @Composable
    fun Language(onBack: () -> Unit) {
        val colors = suhoodColors
        var selected by remember { mutableIntStateOf(0) }
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = SuhoodSpacing.screenHorizontal)) {
            ScreenHeader(title = "Language", onBack = onBack)
            Spacer(Modifier.height(16.dp))
            GlassSegmentedControl(
                options = listOf("English", "العربية"),
                selectedIndex = selected,
                onSelect = { selected = it }
            )
            Spacer(Modifier.height(12.dp))
            Text("Arabic RTL support is prepared for religious content.", style = SuhoodTypography.bodySmall, color = colors.textMuted)
        }
    }

    @Composable
    fun About(onBack: () -> Unit) {
        val colors = suhoodColors
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = SuhoodSpacing.screenHorizontal)) {
            ScreenHeader(title = "About Suhood", onBack = onBack)
            Spacer(Modifier.height(16.dp))
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Suhood", style = SuhoodTypography.headlineSmall, color = colors.textPrimary)
                Text("Version 1.0.0-phase1", style = SuhoodTypography.bodySmall, color = colors.textMuted)
                Spacer(Modifier.height(12.dp))
                Text(
                    "Help Muslims spend less time distracted and more time reconnecting with Allah.\n\nFree. Non-commercial. No ads. No subscriptions.",
                    style = SuhoodTypography.bodyMedium,
                    color = colors.textSecondary
                )
            }
        }
    }

    @Composable
    fun Privacy(onBack: () -> Unit) {
        val colors = suhoodColors
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = SuhoodSpacing.screenHorizontal)) {
            ScreenHeader(title = "Privacy", onBack = onBack)
            Spacer(Modifier.height(16.dp))
            GlassCard {
                Text(
                    "Suhood Phase 1 stores preferences and demo data only on your device. No account is required. No analytics or cloud sync is enabled in this phase.",
                    style = SuhoodTypography.bodyMedium,
                    color = colors.textSecondary
                )
            }
        }
    }

    @Composable
    fun PrayerPrefs(onBack: () -> Unit) {
        Phase2Note(title = "Prayer preferences", message = "Calculation method, madhab, and high-latitude rules will be configurable in Phase 2 when real prayer times are integrated.", onBack = onBack)
    }

    @Composable
    fun NotificationPrefs(onBack: () -> Unit) {
        Phase2Note(title = "Notifications", message = "Prayer notifications and Fajr alarms will be available in Phase 2. No permissions are requested in this demo.", onBack = onBack)
    }

    @Composable
    fun FocusPrefs(onBack: () -> Unit) {
        Phase2Note(title = "Focus preferences", message = "Real app blocking via Usage Access / Accessibility will be added in Phase 2. Current focus sessions are simulated.", onBack = onBack)
    }

    @Composable
    fun AudioPrefs(onBack: () -> Unit) {
        val colors = suhoodColors
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = SuhoodSpacing.screenHorizontal)) {
            ScreenHeader(title = "Audio preferences", onBack = onBack)
            Spacer(Modifier.height(16.dp))
            Text("Demo catalog is offline. Real Media3 playback arrives in Phase 2.", style = SuhoodTypography.bodyMedium, color = colors.textSecondary)
        }
    }

    @Composable
    fun Accessibility(onBack: () -> Unit) {
        val colors = suhoodColors
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = SuhoodSpacing.screenHorizontal)) {
            ScreenHeader(title = "Accessibility", onBack = onBack)
            Spacer(Modifier.height(16.dp))
            Text(
                "Content descriptions, minimum touch targets (~48 dp), and contrast are considered throughout. Reduce motion preference is prepared for Phase 2 polish.",
                style = SuhoodTypography.bodyMedium,
                color = colors.textSecondary
            )
        }
    }

    @Composable
    private fun Phase2Note(title: String, message: String, onBack: () -> Unit) {
        val colors = suhoodColors
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = SuhoodSpacing.screenHorizontal)) {
            ScreenHeader(title = title, onBack = onBack)
            Spacer(Modifier.height(16.dp))
            DemoStatusLabel("Phase 2")
            Spacer(Modifier.height(12.dp))
            Text(message, style = SuhoodTypography.bodyMedium, color = colors.textSecondary)
        }
    }
}

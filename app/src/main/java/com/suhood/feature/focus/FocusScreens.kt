package com.suhood.feature.focus

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.suhood.core.data.DemoData
import com.suhood.core.designsystem.components.*
import com.suhood.core.designsystem.theme.SuhoodSpacing
import com.suhood.core.designsystem.theme.SuhoodTypography
import com.suhood.core.designsystem.theme.suhoodColors
import com.suhood.core.navigation.Routes
import kotlinx.coroutines.delay

object FocusScreens {

    @Composable
    fun Dashboard(onNavigate: (String) -> Unit, bottomContentPadding: Dp) {
        val colors = suhoodColors
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(bottom = bottomContentPadding)
                .padding(horizontal = SuhoodSpacing.screenHorizontal)
                .padding(bottom = 32.dp)
        ) {
            Text("تركيز · Focus", style = SuhoodTypography.headlineLarge, color = colors.textPrimary)
            Spacer(Modifier.height(4.dp))
            Text("Reduce distraction before Salah", style = SuhoodTypography.bodyMedium, color = colors.textSecondary)
            Spacer(Modifier.height(8.dp))
            DemoStatusLabel("Simulated blocking — Phase 1")
            Spacer(Modifier.height(20.dp))

            GlassCard(modifier = Modifier.fillMaxWidth(), onClick = { onNavigate(Routes.FOCUS_DURATION) }) {
                Text("Start focus session", style = SuhoodTypography.titleMedium, color = colors.textPrimary)
                Text("Choose duration and begin a timed session", style = SuhoodTypography.bodySmall, color = colors.textMuted)
            }
            Spacer(Modifier.height(12.dp))
            GlassCard(modifier = Modifier.fillMaxWidth(), onClick = { onNavigate(Routes.FOCUS_SELECT_APPS) }) {
                Text("Distracting apps", style = SuhoodTypography.titleMedium, color = colors.textPrimary)
                Text("Select apps to silence during focus (demo list)", style = SuhoodTypography.bodySmall, color = colors.textMuted)
            }
            Spacer(Modifier.height(12.dp))
            GlassCard(modifier = Modifier.fillMaxWidth(), onClick = { onNavigate(Routes.FOCUS_HISTORY) }) {
                Text("History & stats", style = SuhoodTypography.titleMedium, color = colors.textPrimary)
                Text("Review past sessions", style = SuhoodTypography.bodySmall, color = colors.textMuted)
            }
            Spacer(Modifier.height(12.dp))
            GlassCard(modifier = Modifier.fillMaxWidth(), onClick = { onNavigate(Routes.FOCUS_TIME_FOR_SALAH) }) {
                Text("Preview: Time for Salah", style = SuhoodTypography.titleMedium, color = colors.textPrimary)
                Text("See the interruption screen", style = SuhoodTypography.bodySmall, color = colors.textMuted)
            }
        }
    }

    @Composable
    fun SelectApps(onBack: () -> Unit) {
        val colors = suhoodColors
        var apps by remember { mutableStateOf(DemoData.distractingApps) }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(bottom = 24.dp)
        ) {
            ScreenHeader(title = "Distracting apps", onBack = onBack)
            Text(
                "Demo list only. Real app blocking requires Usage Access in Phase 2.",
                style = SuhoodTypography.bodySmall,
                color = colors.textMuted,
                modifier = Modifier.padding(horizontal = SuhoodSpacing.screenHorizontal)
            )
            Spacer(Modifier.height(12.dp))
            apps.forEach { app ->
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = SuhoodSpacing.screenHorizontal, vertical = 4.dp),
                    selected = app.isSelected,
                    onClick = {
                        apps = apps.map {
                            if (it.id == app.id) it.copy(isSelected = !it.isSelected) else it
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(app.name, style = SuhoodTypography.titleSmall, color = colors.textPrimary)
                            Text(app.packageName, style = SuhoodTypography.bodySmall, color = colors.textMuted)
                        }
                        Text(
                            if (app.isSelected) "Selected" else "Off",
                            style = SuhoodTypography.labelMedium,
                            color = if (app.isSelected) colors.silverBright else colors.textMuted
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun DurationSelect(onBack: () -> Unit, onConfirm: (Int) -> Unit) {
        val colors = suhoodColors
        val options = listOf(5, 10, 15, 20)
        var selected by remember { mutableIntStateOf(15) }
        var showCustom by remember { mutableStateOf(false) }
        var customText by remember { mutableStateOf("25") }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = SuhoodSpacing.screenHorizontal)
        ) {
            ScreenHeader(title = "Focus duration", onBack = onBack)
            Spacer(Modifier.height(16.dp))
            options.forEach { mins ->
                GlassCard(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    selected = selected == mins && !showCustom,
                    onClick = { selected = mins; showCustom = false }
                ) {
                    Text("$mins minutes", style = SuhoodTypography.titleMedium, color = colors.textPrimary)
                }
            }
            GlassCard(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                selected = showCustom,
                onClick = { showCustom = true }
            ) {
                Text("Custom…", style = SuhoodTypography.titleMedium, color = colors.textPrimary)
            }
            if (showCustom) {
                Spacer(Modifier.height(12.dp))
                GlassSearchField(
                    query = customText,
                    onQueryChange = { customText = it.filter { c -> c.isDigit() }.take(3) },
                    placeholder = "Minutes"
                )
            }
            Spacer(Modifier.height(24.dp))
            GlassButton(
                text = "Start session",
                onClick = {
                    val mins = if (showCustom) customText.toIntOrNull()?.coerceIn(1, 120) ?: 15 else selected
                    onConfirm(mins)
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    @Composable
    fun ActiveSession(durationMinutes: Int, onComplete: () -> Unit, onCancel: () -> Unit) {
        val colors = suhoodColors
        var remaining by remember { mutableIntStateOf(durationMinutes * 60) }
        var paused by remember { mutableStateOf(false) }

        LaunchedEffect(paused) {
            while (!paused && remaining > 0) {
                delay(1000)
                remaining--
            }
            if (remaining <= 0) onComplete()
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(SuhoodSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            DemoStatusLabel("Simulated session")
            Spacer(Modifier.height(24.dp))
            Text(
                formatTime(remaining),
                style = SuhoodTypography.timer,
                color = colors.silverBright
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (paused) "Paused" else "Focus in progress",
                style = SuhoodTypography.bodyMedium,
                color = colors.textSecondary
            )
            Spacer(Modifier.height(40.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GlassButton(
                    text = if (paused) "Resume" else "Pause",
                    onClick = { paused = !paused },
                    primary = false
                )
                GlassButton(text = "Cancel", onClick = onCancel, primary = false)
            }
            Spacer(Modifier.height(12.dp))
            GlassButton(text = "End early", onClick = onComplete)
        }
    }

    @Composable
    fun SessionComplete(onDone: () -> Unit) {
        val colors = suhoodColors
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().padding(SuhoodSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Session complete", style = SuhoodTypography.headlineLarge, color = colors.textPrimary)
            Spacer(Modifier.height(12.dp))
            Text("May your focus return you to Salah with a calm heart.", style = SuhoodTypography.bodyMedium, color = colors.textSecondary)
            Spacer(Modifier.height(32.dp))
            GlassButton(text = "Done", onClick = onDone, modifier = Modifier.fillMaxWidth())
        }
    }

    @Composable
    fun History(onBack: () -> Unit) {
        val colors = suhoodColors
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            ScreenHeader(title = "Focus history", onBack = onBack)
            EmptyState(
                title = "No sessions yet",
                message = "Completed focus sessions will appear here.",
                modifier = Modifier.padding(top = 40.dp)
            )
        }
    }

    @Composable
    fun TimeForSalah(onDismiss: () -> Unit) {
        val colors = suhoodColors
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().padding(SuhoodSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Time for Salah", style = SuhoodTypography.headlineLarge, color = colors.textPrimary)
            Spacer(Modifier.height(12.dp))
            Text(
                "Your focus session paused for prayer.\nThis is a preview of the Phase 2 interruption screen.",
                style = SuhoodTypography.bodyMedium,
                color = colors.textSecondary
            )
            Spacer(Modifier.height(32.dp))
            GlassButton(text = "I have prayed", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            GlassButton(text = "Snooze 5 min", onClick = onDismiss, primary = false, modifier = Modifier.fillMaxWidth())
        }
    }

    private fun formatTime(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return "%02d:%02d".format(m, s)
    }
}

package com.suhood.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.suhood.core.data.DemoData
import com.suhood.core.designsystem.components.DemoStatusLabel
import com.suhood.core.designsystem.components.GlassCard
import com.suhood.core.designsystem.components.GlassPill
import com.suhood.core.designsystem.theme.SuhoodSpacing
import com.suhood.core.designsystem.theme.SuhoodTypography
import com.suhood.core.designsystem.theme.suhoodColors
import com.suhood.core.navigation.Routes

@Composable
fun HomeScreen(
    onNavigate: (String) -> Unit,
    topContentPadding: Dp
) {
    val colors = suhoodColors
    val schedule = remember { DemoData.demoPrayerSchedule() }
    val scroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(top = topContentPadding)
            .padding(horizontal = SuhoodSpacing.screenHorizontal)
            .padding(bottom = 32.dp)
    ) {
        // Greeting
        Text("Assalamu alaikum", style = SuhoodTypography.headlineMedium, color = colors.textPrimary)
        Spacer(Modifier.height(4.dp))
        Text(schedule.dateLabel, style = SuhoodTypography.bodyMedium, color = colors.textSecondary)
        Text(schedule.hijriLabel, style = SuhoodTypography.bodySmall, color = colors.textMuted)
        Spacer(Modifier.height(20.dp))

        // Next Salah card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Next: ${schedule.nextPrayer.name}", style = SuhoodTypography.titleMedium, color = colors.textPrimary)
                        Spacer(Modifier.width(8.dp))
                        DemoStatusLabel("Demo times")
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        formatCountdown(schedule.countdownSeconds),
                        style = SuhoodTypography.timerSmall,
                        color = colors.silverBright
                    )
                    Text(schedule.nextPrayer.time, style = SuhoodTypography.bodyMedium, color = colors.textSecondary)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Five prayer times
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text("Today's Salah", style = SuhoodTypography.titleMedium, color = colors.textPrimary)
            Spacer(Modifier.height(12.dp))
            schedule.times.forEach { pt ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        pt.name,
                        style = SuhoodTypography.bodyMedium,
                        color = when {
                            pt.isNext -> colors.silverBright
                            pt.isPassed -> colors.textMuted
                            else -> colors.textPrimary
                        }
                    )
                    Text(
                        pt.time,
                        style = SuhoodTypography.bodyMedium,
                        color = when {
                            pt.isNext -> colors.silverBright
                            pt.isPassed -> colors.textMuted
                            else -> colors.textSecondary
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Quick actions
        Text("Quick actions", style = SuhoodTypography.titleMedium, color = colors.textPrimary)
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickAction("Focus", Icons.Filled.Timelapse) { onNavigate(Routes.FOCUS) }
            QuickAction("Quran", Icons.Filled.MenuBook) { onNavigate(Routes.QURAN) }
            QuickAction("Tasbeeh", Icons.Filled.SelfImprovement) { onNavigate(Routes.TASBEEH) }
            QuickAction("Alarms", Icons.Filled.Alarm) { onNavigate(Routes.ALARMS) }
        }

        Spacer(Modifier.height(20.dp))

        // Daily reminder
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text("Daily reminder", style = SuhoodTypography.titleSmall, color = colors.textMuted)
            Spacer(Modifier.height(6.dp))
            Text(
                "\"Indeed, in the remembrance of Allah do hearts find rest.\" — Quran 13:28",
                style = SuhoodTypography.bodyMedium,
                color = colors.textPrimary
            )
        }

        Spacer(Modifier.height(20.dp))

        // Recently played demo
        Text("Recently played", style = SuhoodTypography.titleMedium, color = colors.textPrimary)
        Spacer(Modifier.height(8.dp))
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = { onNavigate(Routes.AUDIO) }
        ) {
            Text("Morning Light · Demo Ensemble", style = SuhoodTypography.titleSmall, color = colors.textPrimary)
            Text("Nasheeds · Demo playback", style = SuhoodTypography.bodySmall, color = colors.textMuted)
        }

        Spacer(Modifier.height(16.dp))
        GlassPill(onClick = { onNavigate(Routes.SETTINGS) }) {
            Text("Settings", style = SuhoodTypography.labelMedium, color = colors.textSecondary)
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.QuickAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    val colors = suhoodColors
    GlassCard(
        modifier = Modifier.weight(1f),
        onClick = onClick,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Icon(icon, contentDescription = label, tint = colors.silver, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(6.dp))
            Text(label, style = SuhoodTypography.labelSmall, color = colors.textSecondary)
        }
    }
}

private fun formatCountdown(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

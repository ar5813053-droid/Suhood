package com.suhood.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.suhood.core.data.DemoData
import com.suhood.core.prayer.PrayerTimesCalculator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import java.util.Calendar
import com.suhood.core.designsystem.components.DemoStatusLabel
import com.suhood.core.designsystem.components.GlassCard
import com.suhood.core.designsystem.components.GlassPill
import com.suhood.core.designsystem.components.GlassIconButton
import com.suhood.core.designsystem.theme.SuhoodSpacing
import com.suhood.core.designsystem.theme.SuhoodTypography
import com.suhood.core.designsystem.theme.suhoodColors
import com.suhood.core.navigation.Routes

@Composable
fun HomeScreen(
    onNavigate: (String) -> Unit,
    bottomContentPadding: Dp
) {
    val colors = suhoodColors
    // Calculated prayer times (offline). Default location: Makkah.
    // Change latitude/longitude in Settings when location prefs are wired.
    val prayerResult = remember {
        PrayerTimesCalculator.calculate(
            latitude = 21.4225,
            longitude = 39.8262,
            method = PrayerTimesCalculator.Method.MWL
        )
    }
    var countdown by remember {
        mutableIntStateOf(PrayerTimesCalculator.countdownSecondsToNext(prayerResult))
    }
    LaunchedEffect(Unit) {
        while (true) {
            countdown = PrayerTimesCalculator.countdownSecondsToNext(prayerResult)
            delay(1000)
        }
    }
    val nextPrayer = prayerResult.times.firstOrNull { it.isNext } ?: prayerResult.times.first()
    val schedule = DemoData.demoPrayerSchedule() // hijri label only until full hijri lib
    val scroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .statusBarsPadding()
                .padding(bottom = bottomContentPadding)
            .padding(horizontal = SuhoodSpacing.screenHorizontal)
            .padding(bottom = 32.dp)
    ) {
        // Greeting + settings
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("السلام عليكم", style = SuhoodTypography.arabicTitle, color = colors.textPrimary)
                Spacer(Modifier.height(2.dp))
                Text("Assalamu alaikum", style = SuhoodTypography.bodyMedium, color = colors.textSecondary)
                Spacer(Modifier.height(4.dp))
                Text(schedule.dateLabel, style = SuhoodTypography.bodySmall, color = colors.textMuted)
                Text(schedule.hijriLabel, style = SuhoodTypography.bodySmall, color = colors.textMuted)
            }
            GlassIconButton(
                icon = Icons.Filled.Settings,
                contentDescription = "Settings",
                onClick = { onNavigate(Routes.SETTINGS) },
                size = 44.dp,
                iconSize = 22.dp
            )
        }
        Spacer(Modifier.height(20.dp))

        // Next Salah card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Next: ${nextPrayer.name}", style = SuhoodTypography.titleMedium, color = colors.textPrimary)
                        Spacer(Modifier.width(8.dp))
                        DemoStatusLabel("Calculated times · not a mosque timetable")
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        formatCountdown(countdown),
                        style = SuhoodTypography.timerSmall,
                        color = colors.silverBright
                    )
                    Text(nextPrayer.time, style = SuhoodTypography.bodyMedium, color = colors.textSecondary)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Five prayer times
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text("صلوات اليوم · Today's Salah", style = SuhoodTypography.titleMedium, color = colors.textPrimary)
            Spacer(Modifier.height(12.dp))
            prayerResult.times.forEach { pt ->
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
        Text("اختصارات · Quick actions", style = SuhoodTypography.titleMedium, color = colors.textPrimary)
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
            Text("تذكير يومي · Daily reminder", style = SuhoodTypography.titleSmall, color = colors.textMuted)
            Spacer(Modifier.height(6.dp))
            Text(
                "أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ",
                style = SuhoodTypography.arabicBody,
                color = colors.textPrimary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Indeed, in the remembrance of Allah do hearts find rest. — Quran 13:28",
                style = SuhoodTypography.bodySmall,
                color = colors.textSecondary
            )
        }

        Spacer(Modifier.height(20.dp))

        // Recently played demo
        Text("استمعت مؤخراً · Recently played", style = SuhoodTypography.titleMedium, color = colors.textPrimary)
        Spacer(Modifier.height(8.dp))
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = { onNavigate(Routes.AUDIO) }
        ) {
            Text("Islamic audio", style = SuhoodTypography.titleSmall, color = colors.textPrimary)
            Text("Local library and online search", style = SuhoodTypography.bodySmall, color = colors.textMuted)
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

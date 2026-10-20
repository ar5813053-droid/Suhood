package com.suhood.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.suhood.core.data.DemoData
import com.suhood.core.designsystem.components.DemoStatusLabel
import com.suhood.core.designsystem.components.GlassCard
import com.suhood.core.designsystem.components.GlassIconButton
import com.suhood.core.designsystem.theme.SuhoodShapes
import com.suhood.core.designsystem.theme.SuhoodSpacing
import com.suhood.core.designsystem.theme.SuhoodTypography
import com.suhood.core.designsystem.theme.suhoodColors
import com.suhood.core.navigation.Routes
import com.suhood.core.prayer.PrayerTimesCalculator
import java.util.Calendar
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    onNavigate: (String) -> Unit,
    bottomContentPadding: Dp
) {
    val colors = suhoodColors
    val prayerResult = remember {
        PrayerTimesCalculator.calculate(
            latitude = 21.4225,
            longitude = 39.8262,
            calendar = Calendar.getInstance()
        )
    }
    var countdown by remember { mutableIntStateOf(PrayerTimesCalculator.countdownSecondsToNext(prayerResult)) }
    LaunchedEffect(Unit) {
        while (true) {
            countdown = PrayerTimesCalculator.countdownSecondsToNext(prayerResult)
            delay(1000)
        }
    }
    val nextPrayer = prayerResult.times.firstOrNull { it.isNext } ?: prayerResult.times.first()
    val schedule = DemoData.demoPrayerSchedule()
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
        // Brand + greeting
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Suhood",
                    style = SuhoodTypography.labelLarge,
                    color = colors.accentLavender
                )
                Spacer(Modifier.height(4.dp))
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

        Spacer(Modifier.height(22.dp))

        // Hero: Next Salah
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(SuhoodShapes.card)
                .background(
                    Brush.linearGradient(
                        listOf(
                            colors.accentLavender.copy(alpha = 0.22f),
                            colors.surfaceElevated,
                            colors.surface
                        )
                    )
                )
                .border(1.dp, colors.accentLavender.copy(alpha = 0.35f), SuhoodShapes.card)
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Next · ${nextPrayer.name}",
                        style = SuhoodTypography.titleMedium,
                        color = colors.textPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    DemoStatusLabel("Calculated · not a mosque timetable")
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    formatCountdown(countdown),
                    style = SuhoodTypography.timerSmall,
                    color = colors.accentLavender
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    nextPrayer.time,
                    style = SuhoodTypography.bodyMedium,
                    color = colors.textSecondary
                )
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    prayerResult.times.take(5).forEach { p ->
                        val active = p.isNext
                        Box(
                            Modifier
                                .clip(SuhoodShapes.pill)
                                .background(
                                    if (active) colors.accentLavender.copy(alpha = 0.25f)
                                    else colors.surfaceGlass
                                )
                                .border(
                                    1.dp,
                                    if (active) colors.accentLavender.copy(alpha = 0.5f)
                                    else colors.borderSubtle,
                                    SuhoodShapes.pill
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                p.name.take(3),
                                style = SuhoodTypography.labelSmall,
                                color = if (active) colors.accentLavender else colors.textMuted
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        // Quick actions
        Text("Quick access", style = SuhoodTypography.titleMedium, color = colors.textPrimary)
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickAction("Focus", Icons.Filled.Timelapse, colors.accentLavender) {
                onNavigate(Routes.FOCUS)
            }
            QuickAction("Quran", Icons.Filled.MenuBook, colors.accentMint) {
                onNavigate(Routes.QURAN)
            }
            QuickAction("Tasbeeh", Icons.Filled.SelfImprovement, colors.accentYellow) {
                onNavigate(Routes.TASBEEH)
            }
            QuickAction("Alarms", Icons.Filled.Alarm, colors.accentCoral) {
                onNavigate(Routes.ALARMS)
            }
        }

        Spacer(Modifier.height(18.dp))

        // Worship entry
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = { onNavigate(Routes.WORSHIP) }
        ) {
            Text("عبادة · Worship", style = SuhoodTypography.titleMedium, color = colors.textPrimary)
            Spacer(Modifier.height(4.dp))
            Text(
                "Salah checklist · Dhikr · Adhkar · Quiz",
                style = SuhoodTypography.bodySmall,
                color = colors.textMuted
            )
        }

        Spacer(Modifier.height(12.dp))

        // Daily reminder (existing verified verse 13:28)
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text("تذكير يومي · Daily reminder", style = SuhoodTypography.titleSmall, color = colors.textMuted)
            Spacer(Modifier.height(8.dp))
            Text(
                "أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ",
                style = SuhoodTypography.arabicBody,
                color = colors.textPrimary
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Indeed, in the remembrance of Allah do hearts find rest. — Quran 13:28",
                style = SuhoodTypography.bodySmall,
                color = colors.textSecondary
            )
        }

        Spacer(Modifier.height(18.dp))

        Text("Audio · Nasheedify", style = SuhoodTypography.titleMedium, color = colors.textPrimary)
        Spacer(Modifier.height(8.dp))
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = { onNavigate(Routes.AUDIO) }
        ) {
            Text("Islamic audio", style = SuhoodTypography.titleSmall, color = colors.textPrimary)
            Text(
                "Nasheed · naat · nazam · tilawat — Islamic only",
                style = SuhoodTypography.bodySmall,
                color = colors.textMuted
            )
        }
    }
}

@Composable
private fun RowScope.QuickAction(
    label: String,
    icon: ImageVector,
    accent: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    val colors = suhoodColors
    GlassCard(
        modifier = Modifier.weight(1f),
        onClick = onClick,
        contentPadding = PaddingValues(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.18f))
                    .border(1.dp, accent.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = label, tint = accent, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(8.dp))
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

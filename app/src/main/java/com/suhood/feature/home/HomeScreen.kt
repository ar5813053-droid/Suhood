package com.suhood.feature.home

import android.app.Application
import android.icu.util.IslamicCalendar
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.suhood.app.SuhoodApplication
import com.suhood.core.designsystem.components.GlassCard
import com.suhood.core.designsystem.components.GlassIconButton
import com.suhood.core.designsystem.theme.SuhoodTypography
import com.suhood.core.designsystem.theme.suhoodColors
import com.suhood.core.navigation.Routes
import com.suhood.core.prayer.PrayerTimesCalculator
import com.suhood.feature.salah.SalahStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = (app as SuhoodApplication).database.dao()
    private val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

    val salahToday = dao.observeSalahForDate(today)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val tasbeeh = dao.observeTasbeeh()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@Composable
fun HomeScreen(
    onNavigate: (String) -> Unit,
    bottomContentPadding: androidx.compose.ui.unit.Dp = 0.dp
) {
    val colors = suhoodColors
    val context = LocalContext.current
    val vm: HomeViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(context.applicationContext as Application) as T
            }
        }
    )
    val salahLogs by vm.salahToday.collectAsState()
    val tasbeeh by vm.tasbeeh.collectAsState()

    val prayerResult = remember {
        PrayerTimesCalculator.calculate(
            latitude = 21.4225,
            longitude = 39.8262,
            calendar = Calendar.getInstance()
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

    val prayerDone = salahLogs.count { it.status != SalahStatus.NOT_PRAYED.key }
    val prayerPct = ((prayerDone * 100) / 5)
    val goal = (tasbeeh?.goal ?: 33).coerceAtLeast(1)
    val tCount = tasbeeh?.count ?: 0
    val tasbeehPct = ((tCount * 100) / goal).coerceAtMost(100)

    val gregorian = remember {
        val c = Calendar.getInstance()
        String.format(
            Locale.US,
            "%s, %d %s %d",
            arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")[c.get(Calendar.DAY_OF_WEEK) - 1],
            c.get(Calendar.DAY_OF_MONTH),
            arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")[c.get(Calendar.MONTH)],
            c.get(Calendar.YEAR)
        )
    }
    val hijri = remember { formatHijri() }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = bottomContentPadding + 16.dp)
    ) {
        // Top bar: avatar + Hijri | settings (Muslim Pro–inspired, no login)
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceElevated)
                    .border(1.dp, colors.borderSubtle, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Person, null, tint = colors.accentLavender, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.size(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    hijri,
                    style = SuhoodTypography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary
                )
                Text(
                    gregorian,
                    style = SuhoodTypography.labelSmall,
                    color = colors.textMuted
                )
            }
            GlassIconButton(
                icon = Icons.Filled.Settings,
                contentDescription = "Settings",
                onClick = { onNavigate(Routes.SETTINGS) }
            )
        }

        Spacer(Modifier.height(14.dp))

        // Next prayer hero card
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = { onNavigate(Routes.SALAH) },
            contentPadding = PaddingValues(18.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(
                        "Next  ${nextPrayer.name}",
                        style = SuhoodTypography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.accentLavender
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        nextPrayer.time.lowercase(),
                        style = SuhoodTypography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "in ${formatCountdown(countdown)}",
                        style = SuhoodTypography.bodySmall,
                        color = colors.textSecondary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Suhood", style = SuhoodTypography.labelSmall, color = colors.textMuted)
                    Text(hijri.split(" ").take(3).joinToString(" "), style = SuhoodTypography.labelSmall, color = colors.textSecondary)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "View all prayer times  →",
                style = SuhoodTypography.labelMedium,
                color = colors.accentLavender
            )
        }

        Spacer(Modifier.height(16.dp))

        // Quick actions
        Text(
            "Features",
            style = SuhoodTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = colors.textPrimary
        )
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickAction("Qibla", Icons.Filled.Explore, colors.accentLavender) {
                onNavigate(Routes.QIBLA)
            }
            QuickAction("Focus", Icons.Filled.Timelapse, colors.silver) {
                onNavigate(Routes.FOCUS)
            }
            QuickAction("Tasbeeh", Icons.Filled.SelfImprovement, colors.accentYellow) {
                onNavigate(Routes.TASBEEH)
            }
            QuickAction("Duas", Icons.Filled.MenuBook, colors.accentCoral) {
                onNavigate(Routes.DUA_LIBRARY)
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickAction("Worship", Icons.Filled.SelfImprovement, colors.silverBright) {
                onNavigate(Routes.WORSHIP)
            }
            QuickAction("Alarm", Icons.Filled.Alarm, colors.accentCoral) {
                onNavigate(Routes.ALARMS)
            }
            QuickAction("Quran", Icons.Filled.MenuBook, colors.accentLavender) {
                onNavigate(Routes.QURAN)
            }
            QuickAction("Nasheed", Icons.Filled.SelfImprovement, colors.accentMint) {
                onNavigate(Routes.AUDIO)
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(
            "Today's Progress",
            style = SuhoodTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = colors.textPrimary
        )
        Spacer(Modifier.height(10.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ProgressCard(
                title = "Prayer",
                percent = prayerPct,
                subtitle = "$prayerDone/5 today",
                modifier = Modifier.weight(1f),
                onClick = { onNavigate(Routes.SALAH) }
            )
            ProgressCard(
                title = "Tasbeeh",
                percent = tasbeehPct,
                subtitle = "$tCount/$goal today",
                modifier = Modifier.weight(1f),
                onClick = { onNavigate(Routes.TASBEEH) }
            )
        }

        Spacer(Modifier.height(20.dp))
        Text(
            "Continue",
            style = SuhoodTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = colors.textPrimary
        )
        Spacer(Modifier.height(8.dp))
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = { onNavigate(Routes.QURAN_MUSHAF) }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.MenuBook, null, tint = colors.accentLavender, modifier = Modifier.size(28.dp))
                Spacer(Modifier.size(12.dp))
                Column {
                    Text(
                        "Continue Quran",
                        style = SuhoodTypography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )
                    Text("Open Mushaf pages", style = SuhoodTypography.bodySmall, color = colors.textMuted)
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text("Daily reminder", style = SuhoodTypography.labelSmall, color = colors.textMuted)
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
    }
}

private fun formatHijri(): String {
    return runCatching {
        val ic = IslamicCalendar.getInstance()
        val months = arrayOf(
            "Muharram", "Safar", "Rabiʻ I", "Rabiʻ II", "Jumada I", "Jumada II",
            "Rajab", "Shaʻban", "Ramadan", "Shawwal", "Dhul-Qaʻdah", "Dhul-Hijjah"
        )
        val d = ic.get(IslamicCalendar.DAY_OF_MONTH)
        val m = ic.get(IslamicCalendar.MONTH)
        val y = ic.get(IslamicCalendar.YEAR)
        "$d ${months.getOrElse(m) { "Hijri" }} $y AH"
    }.getOrElse { "Hijri date" }
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
        contentPadding = PaddingValues(10.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.18f))
                    .border(1.dp, accent.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = label, tint = accent, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text(label, style = SuhoodTypography.labelSmall, color = colors.textSecondary)
        }
    }
}

@Composable
private fun ProgressCard(
    title: String,
    percent: Int,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = suhoodColors
    GlassCard(modifier = modifier, onClick = onClick, contentPadding = PaddingValues(16.dp)) {
        Text(title, style = SuhoodTypography.labelMedium, color = colors.textMuted)
        Spacer(Modifier.height(6.dp))
        Text("$percent%", style = SuhoodTypography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary)
        Text(subtitle, style = SuhoodTypography.bodySmall, color = colors.textSecondary)
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { percent / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = colors.accentLavender,
            trackColor = colors.borderSubtle
        )
    }
}

private fun formatCountdown(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

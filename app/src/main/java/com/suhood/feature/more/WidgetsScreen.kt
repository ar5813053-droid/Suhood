package com.suhood.feature.more

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suhood.core.designsystem.components.GlassCard
import com.suhood.core.designsystem.theme.SuhoodTypography
import com.suhood.core.designsystem.theme.suhoodColors
import com.suhood.feature.quest.QuestRules
import com.suhood.feature.quest.QuestStore
import com.suhood.feature.quest.Ranks
import com.suhood.widgets.*

private data class WidgetSpec(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val provider: Class<*>,
    val previewKind: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetsScreen(onBack: () -> Unit) {
    val colors = suhoodColors
    val context = LocalContext.current
    val pinSupported = remember {
        if (Build.VERSION.SDK_INT >= 26) {
            AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported
        } else false
    }

    val specs = listOf(
        WidgetSpec("Next Salah", "Countdown · real prayer times", Icons.Filled.Schedule, NextSalahWidgetReceiver::class.java, "salah"),
        WidgetSpec("Glass Calendar", "Week strip · Hijri date", Icons.Filled.CalendarMonth, CalendarWidgetReceiver::class.java, "calendar"),
        WidgetSpec("Islamic Date", "Arabic weekday · Hijri", Icons.Filled.Today, IslamicDateWidgetReceiver::class.java, "date"),
        WidgetSpec("Worship Rank", "XP circle · tier progress", Icons.Filled.EmojiEvents, RankWidgetReceiver::class.java, "rank"),
        WidgetSpec("Prayer Tracker", "Today's 5 Salah", Icons.Filled.CheckCircle, PrayerTrackerWidgetReceiver::class.java, "tracker"),
        WidgetSpec("Prayer Marker", "Tap to mark Fajr–Isha", Icons.Filled.Edit, PrayerMarkerWidgetReceiver::class.java, "marker"),
        WidgetSpec("Dua of the Day", "Verified dua text", Icons.Filled.Favorite, DuaWidgetReceiver::class.java, "dua"),
        WidgetSpec("Quran Companion", "Daily pages goal", Icons.Filled.MenuBook, QuranCompanionWidgetReceiver::class.java, "quran"),
        WidgetSpec("Mushaf", "Continue reading", Icons.Filled.AutoStories, MushafWidgetReceiver::class.java, "mushaf"),
        WidgetSpec("Nasheedify", "Play / pause audio", Icons.Filled.MusicNote, NasheedifyWidgetReceiver::class.java, "audio"),
        WidgetSpec("Habits", "Journey checklist", Icons.Filled.Flag, HabitWidgetReceiver::class.java, "habit")
    )

    // Live preview data
    val salahSnap = remember { runCatching { WidgetBridge.loadSalahSync(context) }.getOrNull() }
    val date = remember { runCatching { WidgetBridge.dateLabels() }.getOrNull() }
    val xp = remember { QuestStore(context).totalXp }
    val tier = xp / QuestRules.TIER_XP
    val into = xp % QuestRules.TIER_XP
    val rank = Ranks.forTier(tier)

    LaunchedEffect(Unit) {
        WidgetBridge.refreshAll(context)
    }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Widgets", fontWeight = FontWeight.Bold, color = colors.textPrimary)
                        Text(
                            if (pinSupported) "Preview · tap Add to pin on home screen"
                            else "Preview · long-press home screen → Widgets → Suhood",
                            style = SuhoodTypography.labelSmall,
                            color = colors.textMuted
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = colors.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background)
            )
        }
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Glass-style Islamic widgets · live Suhood data (not placeholders).",
                style = SuhoodTypography.bodySmall,
                color = colors.textSecondary
            )

            specs.forEach { spec ->
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(spec.icon, null, tint = colors.accentLavender, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(spec.title, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                            Text(spec.subtitle, style = SuhoodTypography.labelSmall, color = colors.textMuted)
                        }
                        Button(
                            onClick = { pinWidget(context, spec.provider, pinSupported) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.accentLavender,
                                contentColor = Color(0xFF0B0A10)
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text("Add", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    // In-app glass preview
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xCC141519))
                            .border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        when (spec.previewKind) {
                            "salah" -> {
                                val s = salahSnap?.smart
                                Column {
                                    Text(
                                        if (s?.isActiveHighlight == true) s.primaryName.uppercase()
                                        else "NEXT · ${(s?.primaryName ?: "—").uppercase()}",
                                        color = Color(0xFFB9A7FF),
                                        fontSize = 11.sp,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        s?.primaryTimeDisplay ?: "Open Suhood",
                                        color = Color.White,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        s?.let { "in ${it.countdownSeconds / 60} min · ${salahSnap.locationLabel}" }
                                            ?: "Set location for times",
                                        color = Color(0xFFA8ADB5),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            "calendar" -> {
                                Column {
                                    Text(date?.hijriLineEn ?: "Hijri date", color = Color(0xFFB9A7FF), fontSize = 12.sp)
                                    Text(
                                        java.text.SimpleDateFormat("EEEE d MMMM", java.util.Locale.US)
                                            .format(java.util.Date()),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Text("Transparent week strip on home screen", color = Color(0xFFA8ADB5), fontSize = 11.sp)
                                }
                            }
                            "date" -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                    Text(date?.weekdayAr ?: "—", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    Text(date?.gregorianDay?.toString() ?: "—", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                                    Text(date?.hijriLineAr ?: "—", color = Color(0xFFC7CBD1), fontSize = 13.sp)
                                }
                            }
                            "rank" -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        Modifier
                                            .size(56.dp)
                                            .clip(RoundedCornerShape(28.dp))
                                            .border(3.dp, Color(0xFFB9A7FF), RoundedCornerShape(28.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("$xp", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text("XP", color = Color(0xFFA8ADB5), fontSize = 9.sp)
                                        }
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(rank.arabic, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text("${rank.latin} · Tier $tier", color = Color(0xFFA8ADB5), fontSize = 12.sp)
                                        Text(
                                            "${QuestRules.TIER_XP - into} XP to next tier",
                                            color = Color(0xFF6FCF97),
                                            fontSize = 11.sp
                                        )
                                        LinearProgressIndicator(
                                            progress = { into / QuestRules.TIER_XP.toFloat() },
                                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp).height(4.dp),
                                            color = Color(0xFFB9A7FF),
                                            trackColor = Color(0x33FFFFFF)
                                        )
                                    }
                                }
                            }
                            else -> Text(
                                "Live data from Suhood · pin to home for the system widget",
                                color = Color(0xFFA8ADB5),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Text(
                "Tip: After installing, remove old blank widgets and add them again from this page or the system picker. Previews show real content, not the Suhood logo alone.",
                style = SuhoodTypography.labelSmall,
                color = colors.textMuted
            )
        }
    }
}

private fun pinWidget(context: Context, providerClass: Class<*>, pinSupported: Boolean) {
    val appWidgetManager = AppWidgetManager.getInstance(context)
    val provider = ComponentName(context, providerClass)
    // Always push a content update first so pinned instances aren't blank
    WidgetBridge.refreshAll(context)
    if (Build.VERSION.SDK_INT >= 26 && pinSupported) {
        val success = runCatching {
            val callback = PendingIntent.getBroadcast(
                context,
                providerClass.name.hashCode(),
                Intent(context, WidgetRefreshReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            appWidgetManager.requestPinAppWidget(provider, null, callback)
        }.getOrDefault(false)
        if (!success) {
            Toast.makeText(
                context,
                "Long-press home → Widgets → Suhood → ${providerClass.simpleName.replace("WidgetReceiver", "")}",
                Toast.LENGTH_LONG
            ).show()
        } else {
            Toast.makeText(context, "Confirm on your home screen to place the widget", Toast.LENGTH_SHORT).show()
        }
    } else {
        Toast.makeText(
            context,
            "Long-press home screen → Widgets → Suhood to add this widget",
            Toast.LENGTH_LONG
        ).show()
    }
}

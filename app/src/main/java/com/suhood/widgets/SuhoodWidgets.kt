package com.suhood.widgets

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.suhood.app.MainActivity
import com.suhood.feature.salah.SalahStatus

private val Bg = Color(0xFF14131B)
private val TextPri = Color(0xFFF6F4FB)
private val TextSec = Color(0xFFAAA5B8)
private val Accent = Color(0xFFB9A7FF)
private val Green = Color(0xFF6FCF97)

private fun openAppComponent(context: Context) =
    android.content.ComponentName(context, MainActivity::class.java)

private fun formatCountdown(sec: Int): String {
    val h = sec / 3600
    val m = (sec % 3600) / 60
    val s = sec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

class NextSalahWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snap = runCatching { WidgetBridge.loadSalah(context) }.getOrNull()
        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier.fillMaxSize().background(Bg).padding(14.dp)
                        .clickable(actionStartActivity(openAppComponent(context)))
                ) {
                    if (snap == null) {
                        Text("Prayer times unavailable", style = TextStyle(color = ColorProvider(TextSec), fontSize = 12.sp))
                    } else {
                        val s = snap.smart
                        if (s.isActiveHighlight) {
                            Text(s.primaryName, style = TextStyle(color = ColorProvider(Green), fontSize = 18.sp, fontWeight = FontWeight.Bold))
                            Text("Prayer time started", style = TextStyle(color = ColorProvider(TextSec), fontSize = 12.sp))
                            Spacer(GlanceModifier.height(8.dp))
                            Text("Next Salah", style = TextStyle(color = ColorProvider(TextSec), fontSize = 11.sp))
                            Row(GlanceModifier.fillMaxWidth()) {
                                Text(s.nextName ?: "—", style = TextStyle(color = ColorProvider(TextPri), fontSize = 15.sp, fontWeight = FontWeight.Bold))
                                Spacer(GlanceModifier.defaultWeight())
                                Text(s.nextTimeDisplay ?: "", style = TextStyle(color = ColorProvider(TextPri), fontSize = 15.sp))
                            }
                        } else {
                            Text("Next · ${s.primaryName}", style = TextStyle(color = ColorProvider(Accent), fontSize = 13.sp, fontWeight = FontWeight.Medium))
                            Text(s.primaryTimeDisplay, style = TextStyle(color = ColorProvider(TextPri), fontSize = 22.sp, fontWeight = FontWeight.Bold))
                            Text("in ${formatCountdown(s.countdownSeconds)}", style = TextStyle(color = ColorProvider(TextSec), fontSize = 12.sp))
                        }
                        Spacer(GlanceModifier.height(6.dp))
                        Text(s.dateLabel, style = TextStyle(color = ColorProvider(TextSec), fontSize = 10.sp))
                        Text(snap.locationLabel, style = TextStyle(color = ColorProvider(TextSec), fontSize = 10.sp))
                    }
                }
            }
        }
    }
}
class NextSalahWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NextSalahWidget()
}

class DuaWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val dua = WidgetBridge.duaOfDay()
        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier.fillMaxSize().background(Bg).padding(14.dp)
                        .clickable(actionStartActivity(openAppComponent(context)))
                ) {
                    Text("Dua of the Day", style = TextStyle(color = ColorProvider(Accent), fontSize = 11.sp))
                    Spacer(GlanceModifier.height(6.dp))
                    Text(dua.arabic, style = TextStyle(color = ColorProvider(TextPri), fontSize = 15.sp, textAlign = TextAlign.End))
                    Spacer(GlanceModifier.height(6.dp))
                    Text(dua.translation.take(120) + if (dua.translation.length > 120) "…" else "", style = TextStyle(color = ColorProvider(TextSec), fontSize = 11.sp))
                    if (dua.reference.isNotBlank()) {
                        Spacer(GlanceModifier.height(4.dp))
                        Text(dua.reference, style = TextStyle(color = ColorProvider(Accent), fontSize = 10.sp))
                    }
                }
            }
        }
    }
}
class DuaWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DuaWidget()
}

class IslamicDateWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val (hijri, greg) = WidgetBridge.hijriGregorianLabels()
        val cal = java.util.Calendar.getInstance()
        val day = cal.get(java.util.Calendar.DAY_OF_MONTH)
        val month = java.text.SimpleDateFormat("MMMM", java.util.Locale.US).format(cal.time).uppercase()
        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier.fillMaxSize().background(Bg).padding(14.dp)
                        .clickable(actionStartActivity(openAppComponent(context))),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(day.toString(), style = TextStyle(color = ColorProvider(TextPri), fontSize = 36.sp, fontWeight = FontWeight.Bold))
                    Text(month, style = TextStyle(color = ColorProvider(TextSec), fontSize = 12.sp))
                    Spacer(GlanceModifier.height(8.dp))
                    Text(hijri, style = TextStyle(color = ColorProvider(Accent), fontSize = 12.sp))
                    Text(greg, style = TextStyle(color = ColorProvider(TextSec), fontSize = 10.sp))
                }
            }
        }
    }
}
class IslamicDateWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = IslamicDateWidget()
}

class PrayerTrackerWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snap = runCatching { WidgetBridge.loadSalah(context) }.getOrNull()
        val done = snap?.completedCount ?: 0
        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier.fillMaxSize().background(Bg).padding(14.dp)
                        .clickable(actionStartActivity(openAppComponent(context)))
                ) {
                    Text("Today's prayers", style = TextStyle(color = ColorProvider(Accent), fontSize = 11.sp))
                    Text("$done / 5", style = TextStyle(color = ColorProvider(TextPri), fontSize = 28.sp, fontWeight = FontWeight.Bold))
                    Text(if (done == 0) "Nothing recorded yet" else "$done of 5 recorded", style = TextStyle(color = ColorProvider(TextSec), fontSize = 11.sp))
                    Spacer(GlanceModifier.height(8.dp))
                    Row {
                        listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha").forEach { name ->
                            val ok = snap?.marked?.get(name)?.let { it != SalahStatus.NOT_PRAYED.key } == true
                            Text(if (ok) "●" else "○", style = TextStyle(color = ColorProvider(if (ok) Green else TextSec), fontSize = 14.sp))
                            Spacer(GlanceModifier.width(6.dp))
                        }
                    }
                }
            }
        }
    }
}
class PrayerTrackerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PrayerTrackerWidget()
}

class QuranCompanionWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val q = runCatching { WidgetBridge.loadQuran(context) }.getOrNull()
        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier.fillMaxSize().background(Bg).padding(14.dp)
                        .clickable(actionStartActivity(openAppComponent(context)))
                ) {
                    Text("Quran Companion", style = TextStyle(color = ColorProvider(Accent), fontSize = 11.sp))
                    if (q == null) {
                        Text("Open Suhood to set a goal", style = TextStyle(color = ColorProvider(TextSec), fontSize = 12.sp))
                    } else {
                        Text("${q.pagesDone} / ${q.targetPages} pages", style = TextStyle(color = ColorProvider(TextPri), fontSize = 20.sp, fontWeight = FontWeight.Bold))
                        Text("${q.dailyMinutes} min daily goal", style = TextStyle(color = ColorProvider(TextSec), fontSize = 12.sp))
                        if (q.lastSurah != null) {
                            Text("Last · Surah ${q.lastSurah}:${q.lastAyah ?: 1}", style = TextStyle(color = ColorProvider(TextSec), fontSize = 11.sp))
                        } else {
                            Text("Page ${q.lastPage}", style = TextStyle(color = ColorProvider(TextSec), fontSize = 11.sp))
                        }
                        Spacer(GlanceModifier.height(6.dp))
                        Text("Continue reading →", style = TextStyle(color = ColorProvider(Accent), fontSize = 12.sp))
                    }
                }
            }
        }
    }
}
class QuranCompanionWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuranCompanionWidget()
}

class NasheedifyWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val a = WidgetBridge.loadAudio(context)
        provideContent {
            GlanceTheme {
                Column(modifier = GlanceModifier.fillMaxSize().background(Bg).padding(14.dp)) {
                    Text("Nasheedify", style = TextStyle(color = ColorProvider(Accent), fontSize = 11.sp))
                    Text(a.title ?: "Nothing playing", style = TextStyle(color = ColorProvider(TextPri), fontSize = 15.sp, fontWeight = FontWeight.Bold))
                    Text(a.artist ?: "Open Audio to play", style = TextStyle(color = ColorProvider(TextSec), fontSize = 11.sp))
                    Spacer(GlanceModifier.height(10.dp))
                    Row {
                        Text(
                            if (a.isPlaying) "Pause" else "Play",
                            style = TextStyle(color = ColorProvider(Accent), fontSize = 13.sp, fontWeight = FontWeight.Bold),
                            modifier = GlanceModifier.clickable(actionRunCallback<PlayPauseAction>())
                        )
                        Spacer(GlanceModifier.width(16.dp))
                        Text(
                            "Next",
                            style = TextStyle(color = ColorProvider(TextSec), fontSize = 13.sp),
                            modifier = GlanceModifier.clickable(actionRunCallback<SkipNextAction>())
                        )
                    }
                }
            }
        }
    }
}
class NasheedifyWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NasheedifyWidget()
}

class HabitWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val habits = runCatching { WidgetBridge.loadHabits(context) }.getOrElse { emptyList() }
        val done = habits.count { it.doneToday }
        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier.fillMaxSize().background(Bg).padding(14.dp)
                        .clickable(actionStartActivity(openAppComponent(context)))
                ) {
                    Text("My Journey", style = TextStyle(color = ColorProvider(Accent), fontSize = 11.sp))
                    Text(
                        if (habits.isEmpty()) "No habits yet" else "$done / ${habits.size} today",
                        style = TextStyle(color = ColorProvider(TextPri), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    )
                    habits.take(3).forEach { h ->
                        Spacer(GlanceModifier.height(4.dp))
                        Text(
                            "${if (h.doneToday) "✓" else "○"}  ${h.title}",
                            style = TextStyle(color = ColorProvider(if (h.doneToday) Green else TextSec), fontSize = 12.sp),
                            modifier = GlanceModifier.clickable(
                                actionRunCallback<ToggleHabitAction>(
                                    actionParametersOf(ToggleHabitAction.HabitKey to h.id)
                                )
                            )
                        )
                    }
                }
            }
        }
    }
}
class HabitWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HabitWidget()
}

class MushafWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val q = runCatching { WidgetBridge.loadQuran(context) }.getOrNull()
        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier.fillMaxSize().background(Bg).padding(14.dp)
                        .clickable(actionStartActivity(openAppComponent(context))),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Mushaf", style = TextStyle(color = ColorProvider(Accent), fontSize = 11.sp))
                    Text("Page ${q?.lastPage ?: 1}", style = TextStyle(color = ColorProvider(TextPri), fontSize = 26.sp, fontWeight = FontWeight.Bold))
                    if (q?.lastSurah != null) {
                        Text("Surah ${q.lastSurah} · Ayah ${q.lastAyah ?: 1}", style = TextStyle(color = ColorProvider(TextSec), fontSize = 12.sp))
                    }
                    Spacer(GlanceModifier.height(6.dp))
                    Text("Continue reading →", style = TextStyle(color = ColorProvider(Accent), fontSize = 12.sp))
                }
            }
        }
    }
}
class MushafWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MushafWidget()
}

class PrayerMarkerWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snap = runCatching { WidgetBridge.loadSalah(context) }.getOrNull()
        val done = snap?.completedCount ?: 0
        provideContent {
            GlanceTheme {
                Column(modifier = GlanceModifier.fillMaxSize().background(Bg).padding(12.dp)) {
                    Row(GlanceModifier.fillMaxWidth()) {
                        Text("Prayer marker", style = TextStyle(color = ColorProvider(Accent), fontSize = 11.sp))
                        Spacer(GlanceModifier.defaultWeight())
                        Text("$done/5", style = TextStyle(color = ColorProvider(TextPri), fontSize = 12.sp, fontWeight = FontWeight.Bold))
                    }
                    Spacer(GlanceModifier.height(8.dp))
                    listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha").forEach { name ->
                        val st = snap?.marked?.get(name)
                        val label = when (st) {
                            SalahStatus.ON_TIME.key, SalahStatus.JAMA_AH.key -> "✓ On time"
                            SalahStatus.LATE.key -> "Late"
                            SalahStatus.NOT_PRAYED.key -> "Not prayed"
                            else -> "Mark"
                        }
                        val col = when (st) {
                            SalahStatus.ON_TIME.key, SalahStatus.JAMA_AH.key -> Green
                            SalahStatus.LATE.key -> Color(0xFFE0C36A)
                            else -> TextSec
                        }
                        Row(
                            modifier = GlanceModifier.fillMaxWidth().padding(vertical = 3.dp).clickable(
                                actionRunCallback<TogglePrayerAction>(
                                    actionParametersOf(TogglePrayerAction.PrayerKey to name)
                                )
                            )
                        ) {
                            Text(name, style = TextStyle(color = ColorProvider(TextPri), fontSize = 13.sp))
                            Spacer(GlanceModifier.defaultWeight())
                            Text(label, style = TextStyle(color = ColorProvider(col), fontSize = 12.sp))
                        }
                    }
                }
            }
        }
    }
}
class PrayerMarkerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PrayerMarkerWidget()
}

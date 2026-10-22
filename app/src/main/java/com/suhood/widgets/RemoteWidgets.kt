package com.suhood.widgets

import java.util.Calendar
import java.util.Locale

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.suhood.app.R
import com.suhood.app.MainActivity
import com.suhood.feature.salah.SalahStatus

private fun openApp(context: Context, navigate: String? = null): PendingIntent {
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        if (navigate != null) putExtra("navigate", navigate)
    }
    return PendingIntent.getActivity(
        context, navigate?.hashCode() ?: 0, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

private fun clickPi(context: Context, action: String, requestCode: Int, extras: Bundle = Bundle()): PendingIntent {
    val intent = Intent(context, WidgetClickReceiver::class.java).setAction(action).putExtras(extras)
    return PendingIntent.getBroadcast(
        context, requestCode, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

private fun formatCountdown(sec: Int): String {
    val h = sec / 3600
    val m = (sec % 3600) / 60
    val s = sec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

/** Push content for every installed instance of [providerClass]. */
fun updateProvider(context: Context, providerClass: Class<out AppWidgetProvider>, build: (Context) -> RemoteViews) {
    val mgr = AppWidgetManager.getInstance(context)
    val ids = mgr.getAppWidgetIds(ComponentName(context, providerClass))
    if (ids.isEmpty()) return
    val views = build(context)
    ids.forEach { mgr.updateAppWidget(it, views) }
}

// ── Builders ────────────────────────────────────────────────────────────────

fun buildNextSalah(context: Context): RemoteViews {
    val rv = RemoteViews(context.packageName, R.layout.widget_next_salah)
    rv.setOnClickPendingIntent(R.id.widget_root, openApp(context, "salah"))
    val snap = runCatching { WidgetBridge.loadSalahSync(context) }.getOrNull()
    if (snap == null) {
        rv.setTextViewText(R.id.w_label, "NEXT SALAH")
        rv.setTextViewText(R.id.w_title, "Open Suhood")
        rv.setTextViewText(R.id.w_sub, "Set location for prayer times")
        rv.setViewVisibility(R.id.w_progress, View.GONE)
        return rv
    }
    val s = snap.smart
    // Never leave placeholder dashes if we have a name
    if (s.primaryName.isBlank() || s.primaryName == "—") {
        rv.setTextViewText(R.id.w_title, "Open Suhood")
        rv.setTextViewText(R.id.w_sub, "Set location in Settings")
        return rv
    }
    if (s.isActiveHighlight) {
        rv.setTextViewText(R.id.w_label, s.primaryName.uppercase())
        rv.setTextViewText(R.id.w_title, "Prayer time started")
        rv.setTextViewText(R.id.w_sub, "Next · ${s.nextName ?: "—"}  ${s.nextTimeDisplay ?: ""}")
        rv.setViewVisibility(R.id.w_progress, View.GONE)
    } else {
        rv.setTextViewText(R.id.w_label, "NEXT · ${s.primaryName.uppercase()}")
        rv.setTextViewText(R.id.w_title, s.primaryTimeDisplay)
        rv.setTextViewText(R.id.w_sub, "in ${formatCountdown(s.countdownSeconds)} · ${snap.locationLabel}")
        rv.setViewVisibility(R.id.w_progress, View.VISIBLE)
        // Approximate progress through current interval (visual only)
        val total = (s.countdownSeconds + 1).coerceAtLeast(1)
        val elapsed = (3600 - (s.countdownSeconds % 3600)).coerceIn(0, 3600)
        rv.setProgressBar(R.id.w_progress, 3600, elapsed, false)
    }
    return rv
}

fun buildDua(context: Context): RemoteViews {
    val rv = RemoteViews(context.packageName, R.layout.widget_dua)
    rv.setOnClickPendingIntent(R.id.widget_root, openApp(context, null))
    val dua = runCatching { WidgetBridge.duaOfDay() }.getOrNull()
    rv.setTextViewText(R.id.w_label, "DUA OF THE DAY")
    if (dua != null) {
        rv.setTextViewText(R.id.w_arabic, dua.arabic.ifBlank { "—" })
        rv.setTextViewText(R.id.w_translation, dua.translation.take(160).ifBlank { "—" })
        rv.setTextViewText(R.id.w_ref, dua.reference.ifBlank { "" })
    } else {
        rv.setTextViewText(R.id.w_arabic, "رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً")
        rv.setTextViewText(R.id.w_translation, "Our Lord, give us good in this world and the Hereafter.")
        rv.setTextViewText(R.id.w_ref, "Qur'an 2:201")
    }
    return rv
}

fun buildIslamicDate(context: Context): RemoteViews {
    val rv = RemoteViews(context.packageName, R.layout.widget_islamic_date)
    rv.setOnClickPendingIntent(R.id.widget_root, openApp(context, null))
    val d = WidgetBridge.dateLabels()
    rv.setTextViewText(R.id.w_weekday, d.weekdayAr)
    rv.setTextViewText(R.id.w_day, d.gregorianDay.toString())
    rv.setTextViewText(R.id.w_month, d.gregorianMonthEn)
    rv.setTextViewText(R.id.w_hijri_ar, d.hijriLineAr)
    rv.setTextViewText(R.id.w_hijri_en, d.hijriLineEn)
    return rv
}

fun buildPrayerTracker(context: Context): RemoteViews {
    val rv = RemoteViews(context.packageName, R.layout.widget_prayer_tracker)
    rv.setOnClickPendingIntent(R.id.widget_root, openApp(context, "salah"))
    val snap = runCatching { WidgetBridge.loadSalahSync(context) }.getOrNull()
    val done = snap?.completedCount ?: 0
    rv.setTextViewText(R.id.w_label, "TODAY'S PRAYERS")
    rv.setTextViewText(R.id.w_title, "$done / 5")
    rv.setTextViewText(
        R.id.w_sub,
        if (done == 0) "Nothing recorded yet" else "$done of 5 recorded"
    )
    val prayers = listOf(
        R.id.dot_fajr to "Fajr",
        R.id.dot_dhuhr to "Dhuhr",
        R.id.dot_asr to "Asr",
        R.id.dot_maghrib to "Maghrib",
        R.id.dot_isha to "Isha"
    )
    prayers.forEachIndexed { idx, (viewId, name) ->
        val st = snap?.marked?.get(name)
        val filled = st != null && st != SalahStatus.NOT_PRAYED.key
        rv.setTextViewText(viewId, if (filled) "●" else "○")
        val b = Bundle().apply { putString(WidgetClickReceiver.EXTRA_PRAYER, name) }
        rv.setOnClickPendingIntent(
            viewId,
            clickPi(context, WidgetClickReceiver.ACTION_TOGGLE_PRAYER, 100 + idx, b)
        )
    }
    return rv
}

fun buildQuranCompanion(context: Context): RemoteViews {
    val rv = RemoteViews(context.packageName, R.layout.widget_quran_companion)
    val q = runCatching { runBlocking(Dispatchers.IO) { WidgetBridge.loadQuran(context) } }.getOrNull()
    rv.setOnClickPendingIntent(R.id.widget_root, openApp(context, "mushaf"))
    rv.setTextViewText(R.id.w_label, "QURAN COMPANION")
    if (q == null) {
        rv.setTextViewText(R.id.w_title, "Set a daily goal")
        rv.setTextViewText(R.id.w_sub, "Open Suhood → onboarding / settings")
        rv.setTextViewText(R.id.w_pct, "—")
    } else {
        val pct = if (q.targetPages > 0) (q.pagesDone * 100 / q.targetPages).coerceIn(0, 100) else 0
        rv.setTextViewText(R.id.w_title, "${q.pagesDone} / ${q.targetPages} pages")
        rv.setTextViewText(R.id.w_sub, "${q.dailyMinutes} min daily · Continue →")
        rv.setTextViewText(R.id.w_pct, "$pct%")
        rv.setProgressBar(R.id.w_progress, 100, pct, false)
    }
    return rv
}

fun buildNasheedify(context: Context): RemoteViews {
    val rv = RemoteViews(context.packageName, R.layout.widget_nasheedify)
    val a = WidgetBridge.loadAudio(context)
    rv.setOnClickPendingIntent(R.id.widget_root, openApp(context, "audio"))
    rv.setTextViewText(R.id.w_title, a.title ?: "Nothing playing")
    rv.setTextViewText(R.id.w_sub, a.artist ?: "Open Nasheedify to play")
    rv.setTextViewText(R.id.w_status, if (a.isPlaying) "● playing" else "paused")
    rv.setTextViewText(R.id.btn_play, if (a.isPlaying) "Pause" else "Play")
    rv.setOnClickPendingIntent(R.id.btn_play, clickPi(context, WidgetClickReceiver.ACTION_PLAY_PAUSE, 201))
    rv.setOnClickPendingIntent(R.id.btn_next, clickPi(context, WidgetClickReceiver.ACTION_NEXT, 202))
    return rv
}

fun buildHabits(context: Context): RemoteViews {
    val rv = RemoteViews(context.packageName, R.layout.widget_habit)
    rv.setOnClickPendingIntent(R.id.widget_root, openApp(context, "journey"))
    val habits = runCatching { runBlocking(Dispatchers.IO) { WidgetBridge.loadHabits(context) } }.getOrElse { emptyList() }
    val done = habits.count { it.doneToday }
    rv.setTextViewText(R.id.w_label, "MY JOURNEY")
    rv.setTextViewText(
        R.id.w_title,
        if (habits.isEmpty()) "No habits yet" else "$done / ${habits.size} today"
    )
    val rows = listOf(
        Triple(R.id.h1, R.id.h1_check, 0),
        Triple(R.id.h2, R.id.h2_check, 1),
        Triple(R.id.h3, R.id.h3_check, 2)
    )
    rows.forEach { (rowId, checkId, idx) ->
        val h = habits.getOrNull(idx)
        if (h == null) {
            rv.setViewVisibility(rowId, View.GONE)
        } else {
            rv.setViewVisibility(rowId, View.VISIBLE)
            rv.setTextViewText(rowId, h.title)
            rv.setTextViewText(checkId, if (h.doneToday) "✓" else "○")
            val b = Bundle().apply { putString(WidgetClickReceiver.EXTRA_HABIT, h.id) }
            rv.setOnClickPendingIntent(
                rowId,
                clickPi(context, WidgetClickReceiver.ACTION_TOGGLE_HABIT, 300 + idx, b)
            )
        }
    }
    return rv
}

fun buildMushaf(context: Context): RemoteViews {
    val rv = RemoteViews(context.packageName, R.layout.widget_mushaf)
    rv.setOnClickPendingIntent(R.id.widget_root, openApp(context, "mushaf"))
    val q = runCatching { runBlocking(Dispatchers.IO) { WidgetBridge.loadQuran(context) } }.getOrNull()
    rv.setTextViewText(R.id.w_label, "MUSHAF")
    rv.setTextViewText(R.id.w_page, (q?.lastPage ?: 1).toString())
    val line = if (q?.lastSurah != null) "Surah ${q.lastSurah} · Ayah ${q.lastAyah ?: 1}" else "Continue reading"
    rv.setTextViewText(R.id.w_sub, line)
    return rv
}

fun buildPrayerMarker(context: Context): RemoteViews {
    val rv = RemoteViews(context.packageName, R.layout.widget_prayer_marker)
    val snap = runCatching { WidgetBridge.loadSalahSync(context) }.getOrNull()
    val done = snap?.completedCount ?: 0
    rv.setTextViewText(R.id.w_label, "PRAYER MARKER")
    rv.setTextViewText(R.id.w_count, "$done/5")
    val rows = listOf(
        Triple(R.id.row_fajr, R.id.st_fajr, "Fajr"),
        Triple(R.id.row_dhuhr, R.id.st_dhuhr, "Dhuhr"),
        Triple(R.id.row_asr, R.id.st_asr, "Asr"),
        Triple(R.id.row_maghrib, R.id.st_maghrib, "Maghrib"),
        Triple(R.id.row_isha, R.id.st_isha, "Isha")
    )
    rows.forEachIndexed { idx, (rowId, stId, name) ->
        val st = snap?.marked?.get(name)
        val label = when (st) {
            SalahStatus.ON_TIME.key, SalahStatus.JAMA_AH.key -> "✓ On time"
            SalahStatus.LATE.key -> "Late"
            SalahStatus.NOT_PRAYED.key -> "Not prayed"
            else -> "Mark"
        }
        rv.setTextViewText(stId, label)
        val b = Bundle().apply { putString(WidgetClickReceiver.EXTRA_PRAYER, name) }
        rv.setOnClickPendingIntent(
            rowId,
            clickPi(context, WidgetClickReceiver.ACTION_TOGGLE_PRAYER, 400 + idx, b)
        )
    }
    return rv
}

// ── Providers ───────────────────────────────────────────────────────────────

/**
 * MUST run synchronously. AppWidgetProvider.onUpdate is killed if we only
 * launch a coroutine without goAsync — that left widgets stuck on "—" placeholders.
 */
private fun providerUpdate(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetIds: IntArray,
    build: (Context) -> RemoteViews
) {
    val views = try {
        build(context)
    } catch (e: Exception) {
        android.util.Log.e("SuhoodWidget", "build failed", e)
        RemoteViews(context.packageName, R.layout.widget_error).apply {
            setTextViewText(R.id.w_title, "Suhood")
            setTextViewText(R.id.w_sub, (e.message ?: "Tap to open").take(80))
            setOnClickPendingIntent(R.id.widget_root, openApp(context, null))
        }
    }
    for (id in appWidgetIds) {
        appWidgetManager.updateAppWidget(id, views)
    }
}

class NextSalahWidgetReceiver : AppWidgetProvider() {
    override fun onUpdate(c: Context, m: AppWidgetManager, ids: IntArray) =
        providerUpdate(c, m, ids, ::buildNextSalah)
}

class DuaWidgetReceiver : AppWidgetProvider() {
    override fun onUpdate(c: Context, m: AppWidgetManager, ids: IntArray) =
        providerUpdate(c, m, ids, ::buildDua)
}

class IslamicDateWidgetReceiver : AppWidgetProvider() {
    override fun onUpdate(c: Context, m: AppWidgetManager, ids: IntArray) =
        providerUpdate(c, m, ids, ::buildIslamicDate)
}

class PrayerTrackerWidgetReceiver : AppWidgetProvider() {
    override fun onUpdate(c: Context, m: AppWidgetManager, ids: IntArray) =
        providerUpdate(c, m, ids, ::buildPrayerTracker)
}

class QuranCompanionWidgetReceiver : AppWidgetProvider() {
    override fun onUpdate(c: Context, m: AppWidgetManager, ids: IntArray) =
        providerUpdate(c, m, ids, ::buildQuranCompanion)
}

class NasheedifyWidgetReceiver : AppWidgetProvider() {
    override fun onUpdate(c: Context, m: AppWidgetManager, ids: IntArray) =
        providerUpdate(c, m, ids, ::buildNasheedify)
}

class HabitWidgetReceiver : AppWidgetProvider() {
    override fun onUpdate(c: Context, m: AppWidgetManager, ids: IntArray) =
        providerUpdate(c, m, ids, ::buildHabits)
}

class MushafWidgetReceiver : AppWidgetProvider() {
    override fun onUpdate(c: Context, m: AppWidgetManager, ids: IntArray) =
        providerUpdate(c, m, ids, ::buildMushaf)
}

class PrayerMarkerWidgetReceiver : AppWidgetProvider() {
    override fun onUpdate(c: Context, m: AppWidgetManager, ids: IntArray) =
        providerUpdate(c, m, ids, ::buildPrayerMarker)
}


fun buildCalendar(context: Context): RemoteViews {
    val rv = RemoteViews(context.packageName, R.layout.widget_calendar)
    rv.setOnClickPendingIntent(R.id.widget_root, openApp(context, "salah"))
    val cal = Calendar.getInstance()
    val monthTitle = String.format(
        Locale.US, "%tB %tY", cal, cal
    )
    rv.setTextViewText(R.id.w_month_title, monthTitle)
    val d = runCatching { WidgetBridge.dateLabels() }.getOrNull()
    rv.setTextViewText(R.id.w_hijri_mini, d?.hijriLineEn ?: d?.hijriLineAr ?: "")

    // Build current week (Sun–Sat) with today numbers
    val today = cal.get(Calendar.DAY_OF_MONTH)
    val dow = cal.get(Calendar.DAY_OF_WEEK) // 1=Sun
    val start = cal.clone() as Calendar
    start.add(Calendar.DAY_OF_MONTH, -(dow - Calendar.SUNDAY))
    val dayIds = intArrayOf(R.id.n0, R.id.n1, R.id.n2, R.id.n3, R.id.n4, R.id.n5, R.id.n6)
    val labelIds = intArrayOf(R.id.d0, R.id.d1, R.id.d2, R.id.d3, R.id.d4, R.id.d5, R.id.d6)
    val labels = arrayOf("S", "M", "T", "W", "T", "F", "S")
    for (i in 0..6) {
        val dayCal = start.clone() as Calendar
        dayCal.add(Calendar.DAY_OF_MONTH, i)
        val dayNum = dayCal.get(Calendar.DAY_OF_MONTH)
        rv.setTextViewText(labelIds[i], labels[i])
        rv.setTextViewText(dayIds[i], dayNum.toString())
        if (dayNum == today && dayCal.get(Calendar.MONTH) == cal.get(Calendar.MONTH)) {
            rv.setTextColor(dayIds[i], 0xFFB9A7FF.toInt())
            rv.setTextColor(labelIds[i], 0xFFB9A7FF.toInt())
        } else {
            rv.setTextColor(dayIds[i], 0xFFF5F6F7.toInt())
            rv.setTextColor(labelIds[i], 0xFFA8ADB5.toInt())
        }
    }
    return rv
}

fun buildRank(context: Context): RemoteViews {
    val rv = RemoteViews(context.packageName, R.layout.widget_rank)
    rv.setOnClickPendingIntent(R.id.widget_root, openApp(context, "quest"))
    val store = runCatching { com.suhood.feature.quest.QuestStore(context) }.getOrNull()
    val xp = store?.totalXp ?: 0
    val tier = xp / com.suhood.feature.quest.QuestRules.TIER_XP
    val into = xp % com.suhood.feature.quest.QuestRules.TIER_XP
    val remain = com.suhood.feature.quest.QuestRules.TIER_XP - into
    val rank = com.suhood.feature.quest.Ranks.forTier(tier)
    rv.setTextViewText(R.id.w_xp, xp.toString())
    rv.setTextViewText(R.id.w_rank_ar, rank.arabic)
    rv.setTextViewText(R.id.w_rank_en, "${rank.latin} · Tier $tier")
    rv.setTextViewText(R.id.w_remain, "$remain XP to next tier")
    rv.setProgressBar(R.id.w_bar, com.suhood.feature.quest.QuestRules.TIER_XP, into, false)
    val pct = ((into * 100) / com.suhood.feature.quest.QuestRules.TIER_XP).coerceIn(0, 100)
    rv.setProgressBar(R.id.w_ring, 100, pct, false)
    return rv
}

class CalendarWidgetReceiver : AppWidgetProvider() {
    override fun onUpdate(c: Context, m: AppWidgetManager, ids: IntArray) =
        providerUpdate(c, m, ids, ::buildCalendar)
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        val m = AppWidgetManager.getInstance(context)
        val ids = m.getAppWidgetIds(android.content.ComponentName(context, CalendarWidgetReceiver::class.java))
        if (ids.isNotEmpty()) onUpdate(context, m, ids)
    }
}

class RankWidgetReceiver : AppWidgetProvider() {
    override fun onUpdate(c: Context, m: AppWidgetManager, ids: IntArray) =
        providerUpdate(c, m, ids, ::buildRank)
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        val m = AppWidgetManager.getInstance(context)
        val ids = m.getAppWidgetIds(android.content.ComponentName(context, RankWidgetReceiver::class.java))
        if (ids.isNotEmpty()) onUpdate(context, m, ids)
    }
}

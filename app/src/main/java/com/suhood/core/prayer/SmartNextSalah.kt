package com.suhood.core.prayer

import androidx.compose.ui.graphics.Color
import java.util.Calendar
import java.util.Locale

/**
 * Home Next-Salah state:
 * - Before a prayer: upcoming name + countdown + calculated time
 * - For 10 minutes after start: active prayer green + "Prayer time started" + next as Next Salah
 * - After 10 minutes: following prayer becomes primary Next
 * - After Isha (+10m): roll to next day's Fajr
 */
enum class PrayerPeriod {
    FAJR, DHUHR, ASR, MAGHRIB, ISHA
}

data class SmartSalahBarState(
    val primaryName: String,
    val primaryTime: String,
    val primaryTimeDisplay: String,
    val countdownSeconds: Int,
    val isActiveHighlight: Boolean,
    val nextName: String?,
    val nextTime: String?,
    val nextTimeDisplay: String?,
    val period: PrayerPeriod,
    val dateLabel: String
)

object SmartNextSalah {

    private const val HIGHLIGHT_MINUTES = 10

    fun resolve(
        result: PrayerTimesCalculator.Result,
        now: Calendar = Calendar.getInstance()
    ): SmartSalahBarState {
        val times = result.times
        val dateLabel = result.dateLabel.ifBlank {
            String.format(
                Locale.US,
                "%1\$tA, %1\$td %1\$tb %1\$tY",
                now
            )
        }
        if (times.isEmpty()) {
            return SmartSalahBarState(
                "—", "--:--", "--:--", 0, false, null, null, null,
                PrayerPeriod.FAJR, dateLabel
            )
        }
        val nowMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val nowSec = nowMin * 60 + now.get(Calendar.SECOND)
        val withMins = times.map { p ->
            val parts = p.time.split(":")
            val m = if (parts.size == 2) {
                (parts[0].toIntOrNull() ?: 0) * 60 + (parts[1].toIntOrNull() ?: 0)
            } else 0
            p to m
        }

        // Active highlight window [start, start+10)
        for (i in withMins.indices) {
            val (p, start) = withMins[i]
            val end = start + HIGHLIGHT_MINUTES
            if (nowMin in start until end) {
                val nextIdx = (i + 1) % withMins.size
                val (nextP, _) = withMins[nextIdx]
                val targetMin = if (nextIdx == 0) withMins[0].second + 24 * 60 else withMins[nextIdx].second
                return SmartSalahBarState(
                    primaryName = p.name,
                    primaryTime = p.time,
                    primaryTimeDisplay = format12h(p.time),
                    countdownSeconds = countdownToMinutes(nowSec, targetMin),
                    isActiveHighlight = true,
                    nextName = nextP.name,
                    nextTime = nextP.time,
                    nextTimeDisplay = format12h(nextP.time),
                    period = periodOf(p.name),
                    dateLabel = dateLabel
                )
            }
        }

        val nextIdx = withMins.indexOfFirst { (_, m) -> m > nowMin }.let { if (it < 0) 0 else it }
        val (nextP, nextStart) = withMins[nextIdx]
        val targetMin = if (nextIdx == 0 && nextStart <= nowMin) nextStart + 24 * 60 else nextStart
        return SmartSalahBarState(
            primaryName = nextP.name,
            primaryTime = nextP.time,
            primaryTimeDisplay = format12h(nextP.time),
            countdownSeconds = countdownToMinutes(nowSec, targetMin),
            isActiveHighlight = false,
            nextName = null,
            nextTime = null,
            nextTimeDisplay = null,
            period = periodOf(nextP.name),
            dateLabel = dateLabel
        )
    }

    fun periodOf(name: String): PrayerPeriod = when (name.lowercase(Locale.US)) {
        "fajr" -> PrayerPeriod.FAJR
        "dhuhr", "zuhr" -> PrayerPeriod.DHUHR
        "asr" -> PrayerPeriod.ASR
        "maghrib" -> PrayerPeriod.MAGHRIB
        else -> PrayerPeriod.ISHA
    }

    /** Soft period tint for the bar (not the active green). */
    fun periodAccent(period: PrayerPeriod): Color = when (period) {
        PrayerPeriod.FAJR -> Color(0xFF7EB8E8)      // morning sky
        PrayerPeriod.DHUHR -> Color(0xFFE0C36A)     // afternoon gold
        PrayerPeriod.ASR -> Color(0xFFE8A06A)       // evening amber
        PrayerPeriod.MAGHRIB -> Color(0xFFE07A6A)   // sunset
        PrayerPeriod.ISHA -> Color(0xFF9A8AD4)      // night lavender
    }

    fun periodLabel(period: PrayerPeriod): String = when (period) {
        PrayerPeriod.FAJR -> "Morning"
        PrayerPeriod.DHUHR -> "Afternoon"
        PrayerPeriod.ASR -> "Evening"
        PrayerPeriod.MAGHRIB -> "Sunset"
        PrayerPeriod.ISHA -> "Night"
    }

    fun format12h(hhmm: String): String {
        val parts = hhmm.split(":")
        if (parts.size != 2) return hhmm
        val h24 = parts[0].toIntOrNull() ?: return hhmm
        val m = parts[1].toIntOrNull() ?: return hhmm
        val am = h24 < 12
        val h12 = when {
            h24 == 0 -> 12
            h24 > 12 -> h24 - 12
            else -> h24
        }
        return String.format(Locale.US, "%d:%02d %s", h12, m, if (am) "AM" else "PM")
    }

    private fun countdownToMinutes(nowSecOfDay: Int, targetMinutes: Int): Int {
        val targetSec = targetMinutes * 60
        var diff = targetSec - nowSecOfDay
        if (diff < 0) diff += 24 * 3600
        return diff.coerceAtLeast(0)
    }
}

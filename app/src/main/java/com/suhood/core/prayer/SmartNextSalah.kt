package com.suhood.core.prayer

import androidx.compose.ui.graphics.Color
import java.util.Calendar
import java.util.Locale

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
    val dateLabel: String,
    /** All five times for debugging / optional UI. */
    val allTimes: List<Pair<String, String>> = emptyList()
)

object SmartNextSalah {

    private const val HIGHLIGHT_MINUTES = 10

    fun resolve(
        result: PrayerTimesCalculator.Result,
        now: Calendar = Calendar.getInstance()
    ): SmartSalahBarState {
        val times = result.times
        val dateLabel = result.dateLabel.ifBlank {
            String.format(Locale.US, "%1\$tA, %1\$td %1\$tb %1\$tY", now)
        }
        val allTimes = times.map { it.name to format12h(it.time) }
        if (times.isEmpty()) {
            return SmartSalahBarState(
                "—", "--:--", "--:--", 0, false, null, null, null,
                PrayerPeriod.FAJR, dateLabel, allTimes
            )
        }

        val nowMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val nowSec = nowMin * 60 + now.get(Calendar.SECOND)

        val withMins = times.map { p ->
            val m = if (p.minutesSinceMidnight > 0) p.minutesSinceMidnight else parseHm(p.time)
            p to m
        }.sortedBy { it.second }

        // Active window: prayer started within last 10 minutes
        for (i in withMins.indices) {
            val (p, start) = withMins[i]
            if (nowMin in start until (start + HIGHLIGHT_MINUTES)) {
                val nextIdx = (i + 1) % withMins.size
                val (nextP, nextStart) = withMins[nextIdx]
                val targetMin = if (nextIdx == 0) nextStart + 24 * 60 else nextStart
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
                    dateLabel = dateLabel,
                    allTimes = allTimes
                )
            }
        }

        // Next upcoming: first prayer with start > nowMin
        val upcoming = withMins.firstOrNull { (_, m) -> m > nowMin }
        if (upcoming != null) {
            val (nextP, nextStart) = upcoming
            return SmartSalahBarState(
                primaryName = nextP.name,
                primaryTime = nextP.time,
                primaryTimeDisplay = format12h(nextP.time),
                countdownSeconds = countdownToMinutes(nowSec, nextStart),
                isActiveHighlight = false,
                nextName = null,
                nextTime = null,
                nextTimeDisplay = null,
                period = periodOf(nextP.name),
                dateLabel = dateLabel,
                allTimes = allTimes
            )
        }

        // After Isha: tomorrow's Fajr
        val (fajr, fajrStart) = withMins.first()
        return SmartSalahBarState(
            primaryName = fajr.name,
            primaryTime = fajr.time,
            primaryTimeDisplay = format12h(fajr.time),
            countdownSeconds = countdownToMinutes(nowSec, fajrStart + 24 * 60),
            isActiveHighlight = false,
            nextName = null,
            nextTime = null,
            nextTimeDisplay = null,
            period = periodOf(fajr.name),
            dateLabel = dateLabel,
            allTimes = allTimes
        )
    }

    fun periodOf(name: String): PrayerPeriod = when (name.lowercase(Locale.US)) {
        "fajr" -> PrayerPeriod.FAJR
        "dhuhr", "zuhr" -> PrayerPeriod.DHUHR
        "asr" -> PrayerPeriod.ASR
        "maghrib" -> PrayerPeriod.MAGHRIB
        else -> PrayerPeriod.ISHA
    }

    fun periodAccent(period: PrayerPeriod): Color = when (period) {
        PrayerPeriod.FAJR -> Color(0xFF7EB8E8)
        PrayerPeriod.DHUHR -> Color(0xFFE0C36A)
        PrayerPeriod.ASR -> Color(0xFFE8A06A)
        PrayerPeriod.MAGHRIB -> Color(0xFFE07A6A)
        PrayerPeriod.ISHA -> Color(0xFF9A8AD4)
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

    private fun parseHm(hhmm: String): Int {
        val parts = hhmm.split(":")
        if (parts.size != 2) return 0
        return (parts[0].toIntOrNull() ?: 0) * 60 + (parts[1].toIntOrNull() ?: 0)
    }

    private fun countdownToMinutes(nowSecOfDay: Int, targetMinutes: Int): Int {
        val targetSec = targetMinutes * 60
        var diff = targetSec - nowSecOfDay
        if (diff < 0) diff += 24 * 3600
        return diff.coerceAtLeast(0)
    }
}

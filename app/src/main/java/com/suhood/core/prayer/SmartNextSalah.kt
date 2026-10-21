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
    val allTimes: List<Pair<String, String>> = emptyList()
)

object SmartNextSalah {

    private const val HIGHLIGHT_MINUTES = 10

    /**
     * @param now local calendar for the user
     *
     * Rules:
     * - If within 10 minutes after a prayer start → highlight that prayer; next = following prayer
     *   (after Isha highlight, next = tomorrow Fajr).
     * - Else primary = next upcoming prayer today; nextName = the one after that (or null).
     * - After last prayer of the day → primary = tomorrow's Fajr.
     */
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
            // Prefer calculator minutes; fall back to parsing HH:mm
            val m = when {
                p.minutesSinceMidnight > 0 -> p.minutesSinceMidnight
                else -> parseHm(p.time)
            }
            p to m
        }.sortedBy { it.second }

        // 1) Active 10-minute highlight window
        for (i in withMins.indices) {
            val (p, start) = withMins[i]
            if (nowMin >= start && nowMin < start + HIGHLIGHT_MINUTES) {
                val nextIdx = i + 1
                val (nextName, nextTime, nextDisplay, targetMin) = if (nextIdx < withMins.size) {
                    val (np, ns) = withMins[nextIdx]
                    Quad(np.name, np.time, format12h(np.time), ns)
                } else {
                    // After Isha highlight → tomorrow Fajr
                    val (fp, fs) = withMins.first()
                    Quad(fp.name, fp.time, format12h(fp.time), fs + 24 * 60)
                }
                return SmartSalahBarState(
                    primaryName = p.name,
                    primaryTime = p.time,
                    primaryTimeDisplay = format12h(p.time),
                    countdownSeconds = countdownToMinutes(nowSec, targetMin),
                    isActiveHighlight = true,
                    nextName = nextName,
                    nextTime = nextTime,
                    nextTimeDisplay = nextDisplay,
                    period = periodOf(p.name),
                    dateLabel = dateLabel,
                    allTimes = allTimes
                )
            }
        }

        // 2) Next upcoming today (start strictly after now)
        val upcomingIdx = withMins.indexOfFirst { (_, m) -> m > nowMin }
        if (upcomingIdx >= 0) {
            val (nextP, nextStart) = withMins[upcomingIdx]
            // Optional: prayer after the upcoming one (for secondary line)
            val after = withMins.getOrNull(upcomingIdx + 1)
            return SmartSalahBarState(
                primaryName = nextP.name,
                primaryTime = nextP.time,
                primaryTimeDisplay = format12h(nextP.time),
                countdownSeconds = countdownToMinutes(nowSec, nextStart),
                isActiveHighlight = false,
                nextName = after?.first?.name,
                nextTime = after?.first?.time,
                nextTimeDisplay = after?.first?.time?.let { format12h(it) },
                period = periodOf(nextP.name),
                dateLabel = dateLabel,
                allTimes = allTimes
            )
        }

        // 3) After last prayer of the day → tomorrow Fajr
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

    private data class Quad(
        val name: String,
        val time: String,
        val display: String,
        val targetMin: Int
    )

    private fun periodOf(name: String): PrayerPeriod = when (name.lowercase(Locale.US)) {
        "fajr" -> PrayerPeriod.FAJR
        "dhuhr", "zuhr" -> PrayerPeriod.DHUHR
        "asr" -> PrayerPeriod.ASR
        "maghrib" -> PrayerPeriod.MAGHRIB
        else -> PrayerPeriod.ISHA
    }

    fun periodAccent(period: PrayerPeriod): Color = when (period) {
        PrayerPeriod.FAJR -> Color(0xFF7EB6FF)      // dawn blue
        PrayerPeriod.DHUHR -> Color(0xFFE0C36A)     // afternoon gold
        PrayerPeriod.ASR -> Color(0xFFE8A87C)       // evening amber
        PrayerPeriod.MAGHRIB -> Color(0xFFE07A5F)   // sunset coral
        PrayerPeriod.ISHA -> Color(0xFF9A82F5)      // night violet
    }

    fun periodLabel(period: PrayerPeriod): String = when (period) {
        PrayerPeriod.FAJR -> "Morning"
        PrayerPeriod.DHUHR -> "Afternoon"
        PrayerPeriod.ASR -> "Evening"
        PrayerPeriod.MAGHRIB -> "Sunset"
        PrayerPeriod.ISHA -> "Night"
    }

    private fun parseHm(hm: String): Int {
        val parts = hm.split(":")
        if (parts.size < 2) return 0
        return parts[0].toIntOrNull()?.times(60)?.plus(parts[1].toIntOrNull() ?: 0) ?: 0
    }

    /** [targetMin] may be > 24*60 for tomorrow. */
    private fun countdownToMinutes(nowSec: Int, targetMin: Int): Int {
        val targetSec = targetMin * 60
        var diff = targetSec - nowSec
        if (diff < 0) diff += 24 * 3600
        return diff.coerceAtLeast(0)
    }

    fun format12h(time24: String): String {
        val parts = time24.split(":")
        if (parts.size < 2) return time24
        var h = parts[0].toIntOrNull() ?: return time24
        val m = parts[1].toIntOrNull() ?: return time24
        val ampm = if (h >= 12) "PM" else "AM"
        h = h % 12
        if (h == 0) h = 12
        return "%d:%02d %s".format(h, m, ampm)
    }
}

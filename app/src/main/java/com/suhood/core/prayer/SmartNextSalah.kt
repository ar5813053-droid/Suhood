package com.suhood.core.prayer

import androidx.compose.ui.graphics.Color
import java.util.Calendar
import java.util.Locale

enum class PrayerPeriod {
    FAJR, DHUHR, JUMUAH, ASR, MAGHRIB, ISHA
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
    val allTimes: List<Pair<String, String>> = emptyList(),
    /** True when the bar is showing Jumu'ah (Friday only) — drives green theme. */
    val useJumuahTheme: Boolean = false,
    /**
     * When true, [primaryTime] is calculated Dhuhr used as a Jumu'ah reference
     * (not a confirmed mosque congregation time).
     */
    val jumuahIsCalculatedReference: Boolean = false
)

object SmartNextSalah {

    private const val HIGHLIGHT_MINUTES = 10
    const val JUMUAH_NAME = "Jumu'ah"

    /**
     * @param now local calendar (user timezone / prayer location calendar)
     * @param customJumuahTime optional HH:mm congregation override (e.g. jamaat Dhuhr).
     *        When blank on Friday, Dhuhr's calculated time is used as a reference only.
     */
    fun resolve(
        result: PrayerTimesCalculator.Result,
        now: Calendar = Calendar.getInstance(),
        customJumuahTime: String? = null
    ): SmartSalahBarState {
        val friday = isFriday(now)
        val dhuhrOriginal = result.times.find { it.name.equals("Dhuhr", true) || it.name.equals("Zuhr", true) }
        val appliedCustom = friday && !customJumuahTime.isNullOrBlank() && parseHm(customJumuahTime) > 0
        val times = applyFridayJumuah(result.times, friday, customJumuahTime)
        val dateLabel = result.dateLabel.ifBlank {
            String.format(Locale.US, "%1\$tA, %1\$td %1\$tb %1\$tY", now)
        }
        val allTimes = times.map { it.name to format12h(it.time) }
        if (times.isEmpty()) {
            return SmartSalahBarState(
                "—", "--:--", "--:--", 0, false, null, null, null,
                PrayerPeriod.FAJR, dateLabel, allTimes, useJumuahTheme = false
            )
        }

        val nowMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val nowSec = nowMin * 60 + now.get(Calendar.SECOND)

        val withMins = times.map { p ->
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
                    val (fp, fs) = withMins.first()
                    Quad(fp.name, fp.time, format12h(fp.time), fs + 24 * 60)
                }
                val jumuah = isJumuahName(p.name)
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
                    allTimes = allTimes,
                    useJumuahTheme = jumuah,
                    jumuahIsCalculatedReference = jumuah && !appliedCustom
                )
            }
        }

        // 2) Next upcoming today
        val upcomingIdx = withMins.indexOfFirst { (_, m) -> m > nowMin }
        if (upcomingIdx >= 0) {
            val (nextP, nextStart) = withMins[upcomingIdx]
            val after = withMins.getOrNull(upcomingIdx + 1)
            val jumuah = isJumuahName(nextP.name)
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
                allTimes = allTimes,
                useJumuahTheme = jumuah,
                jumuahIsCalculatedReference = jumuah && !appliedCustom
            )
        }

        // 3) After last prayer → tomorrow Fajr
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
            allTimes = allTimes,
            useJumuahTheme = false,
            jumuahIsCalculatedReference = false
        )
    }

    /** Friday in the calendar's timezone (local / location calendar). */
    fun isFriday(now: Calendar): Boolean =
        now.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY

    fun isJumuahName(name: String): Boolean {
        val n = name.lowercase(Locale.US).replace("'", "").replace(" ", "")
        return n == "jumuah" || n == "jummah" || n == "jumua"
    }

    /**
     * On Friday only: replace Dhuhr with Jumu'ah (never both).
     * Other days: leave list unchanged (Dhuhr stays Dhuhr).
     */
    fun applyFridayJumuah(
        times: List<com.suhood.core.model.PrayerTime>,
        isFriday: Boolean,
        customJumuahTime: String? = null
    ): List<com.suhood.core.model.PrayerTime> {
        if (!isFriday) return times
        return times.map { p ->
            if (p.name.equals("Dhuhr", true) || p.name.equals("Zuhr", true)) {
                val customMins = customJumuahTime?.let { parseHm(it) }?.takeIf { it > 0 }
                val mins = customMins ?: when {
                    p.minutesSinceMidnight > 0 -> p.minutesSinceMidnight
                    else -> parseHm(p.time)
                }
                val hhmm = if (customMins != null) {
                    "%02d:%02d".format(mins / 60, mins % 60)
                } else {
                    p.time
                }
                p.copy(
                    name = JUMUAH_NAME,
                    time = hhmm,
                    minutesSinceMidnight = mins
                )
            } else {
                p
            }
        }
    }

    private data class Quad(
        val name: String,
        val time: String,
        val display: String,
        val targetMin: Int
    )

    private fun periodOf(name: String): PrayerPeriod = when {
        name.equals("Fajr", true) -> PrayerPeriod.FAJR
        isJumuahName(name) -> PrayerPeriod.JUMUAH
        name.equals("Dhuhr", true) || name.equals("Zuhr", true) -> PrayerPeriod.DHUHR
        name.equals("Asr", true) -> PrayerPeriod.ASR
        name.equals("Maghrib", true) -> PrayerPeriod.MAGHRIB
        else -> PrayerPeriod.ISHA
    }

    fun periodAccent(period: PrayerPeriod): Color = when (period) {
        PrayerPeriod.FAJR -> Color(0xFF7EB6FF)
        PrayerPeriod.DHUHR -> Color(0xFFE0C36A)
        PrayerPeriod.JUMUAH -> Color(0xFF2E8B57) // deep Islamic sea-green
        PrayerPeriod.ASR -> Color(0xFFE8A87C)
        PrayerPeriod.MAGHRIB -> Color(0xFFE07A5F)
        PrayerPeriod.ISHA -> Color(0xFF9A82F5)
    }

    fun periodLabel(period: PrayerPeriod): String = when (period) {
        PrayerPeriod.FAJR -> "Morning"
        PrayerPeriod.DHUHR -> "Afternoon"
        PrayerPeriod.JUMUAH -> "Friday"
        PrayerPeriod.ASR -> "Evening"
        PrayerPeriod.MAGHRIB -> "Sunset"
        PrayerPeriod.ISHA -> "Night"
    }

    private fun parseHm(hm: String): Int {
        val parts = hm.trim().split(":")
        if (parts.size < 2) return 0
        return parts[0].toIntOrNull()?.times(60)?.plus(parts[1].toIntOrNull() ?: 0) ?: 0
    }

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

package com.suhood.core.prayer

import com.suhood.core.model.PrayerTime
import java.util.Calendar

/**
 * Home Next-Salah state:
 * - Before a prayer: show upcoming name + countdown
 * - For 10 minutes after a prayer starts: highlight that prayer (green) and show the following as Next
 * - After 10 minutes: following prayer becomes primary
 * - After Isha (+10m): roll to tomorrow's Fajr
 */
data class SmartSalahBarState(
    val primaryName: String,
    val primaryTime: String,
    val countdownSeconds: Int,
    /** True when primary is in the 10-minute active window after start. */
    val isActiveHighlight: Boolean,
    /** e.g. "Next · Dhuhr" shown under the highlighted prayer. */
    val nextHint: String?,
    val secondaryName: String? = null,
    val secondaryTime: String? = null
)

object SmartNextSalah {

    private const val HIGHLIGHT_MINUTES = 10

    fun resolve(
        result: PrayerTimesCalculator.Result,
        now: Calendar = Calendar.getInstance()
    ): SmartSalahBarState {
        val times = result.times
        if (times.isEmpty()) {
            return SmartSalahBarState("—", "--:--", 0, false, null)
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

        // Active highlight window
        for (i in withMins.indices) {
            val (p, start) = withMins[i]
            val end = start + HIGHLIGHT_MINUTES
            if (nowMin in start until end) {
                val nextIdx = (i + 1) % withMins.size
                val (nextP, nextStart) = withMins[nextIdx]
                // If last prayer (Isha), next is tomorrow Fajr — still show Fajr from list
                val hint = "Next · ${nextP.name}"
                val elapsedSec = (nowMin - start) * 60 + now.get(Calendar.SECOND)
                // countdown to end of highlight is optional; show countdown to following prayer
                val countdown = countdownToMinutes(nowSec, if (nextIdx == 0) nextStart + 24 * 60 else nextStart)
                return SmartSalahBarState(
                    primaryName = p.name,
                    primaryTime = p.time,
                    countdownSeconds = countdown,
                    isActiveHighlight = true,
                    nextHint = hint,
                    secondaryName = nextP.name,
                    secondaryTime = nextP.time
                )
            }
        }

        // Find next upcoming
        val nextIdx = withMins.indexOfFirst { (_, m) -> m > nowMin }.let { if (it < 0) 0 else it }
        val (nextP, nextStart) = withMins[nextIdx]
        val targetMin = if (nextIdx == 0 && nextStart <= nowMin) nextStart + 24 * 60 else nextStart
        val countdown = countdownToMinutes(nowSec, targetMin)
        return SmartSalahBarState(
            primaryName = nextP.name,
            primaryTime = nextP.time,
            countdownSeconds = countdown,
            isActiveHighlight = false,
            nextHint = null,
            secondaryName = null,
            secondaryTime = null
        )
    }

    private fun countdownToMinutes(nowSecOfDay: Int, targetMinutes: Int): Int {
        val targetSec = targetMinutes * 60
        var diff = targetSec - nowSecOfDay
        if (diff < 0) diff += 24 * 3600
        return diff.coerceAtLeast(0)
    }
}

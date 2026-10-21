package com.suhood

import com.suhood.core.model.PrayerTime
import com.suhood.core.prayer.PrayerTimesCalculator
import com.suhood.core.prayer.SmartNextSalah
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class SmartNextSalahTest {

    private fun result(times: List<PrayerTime>) = PrayerTimesCalculator.Result(
        dateLabel = "Test",
        times = times,
        method = PrayerTimesCalculator.Method.MWL,
        latitude = 21.4,
        longitude = 39.8
    )

    private fun cal(h: Int, m: Int): Calendar =
        Calendar.getInstance(TimeZone.getDefault()).apply {
            set(Calendar.HOUR_OF_DAY, h)
            set(Calendar.MINUTE, m)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

    private fun pt(name: String, mins: Int) = PrayerTime(
        name = name,
        time = "%02d:%02d".format(mins / 60, mins % 60),
        minutesSinceMidnight = mins
    )

    @Test
    fun beforeDhuhr_nextIsDhuhr() {
        val times = listOf(
            pt("Fajr", 5 * 60),
            pt("Dhuhr", 12 * 60 + 30),
            pt("Asr", 15 * 60 + 45),
            pt("Maghrib", 18 * 60),
            pt("Isha", 19 * 60 + 30)
        )
        val s = SmartNextSalah.resolve(result(times), cal(10, 0))
        assertEquals("Dhuhr", s.primaryName)
        assertFalse(s.isActiveHighlight)
    }

    @Test
    fun atFajrStart_highlightAndNextDhuhr() {
        val times = listOf(
            pt("Fajr", 5 * 60),
            pt("Dhuhr", 12 * 60 + 30),
            pt("Asr", 15 * 60 + 45),
            pt("Maghrib", 18 * 60),
            pt("Isha", 19 * 60 + 30)
        )
        val s = SmartNextSalah.resolve(result(times), cal(5, 3))
        assertTrue(s.isActiveHighlight)
        assertEquals("Fajr", s.primaryName)
        assertEquals("Dhuhr", s.nextName)
    }

    @Test
    fun afterIsha_rollsToFajr() {
        val times = listOf(
            pt("Fajr", 5 * 60),
            pt("Dhuhr", 12 * 60 + 30),
            pt("Asr", 15 * 60 + 45),
            pt("Maghrib", 18 * 60),
            pt("Isha", 19 * 60 + 30)
        )
        val s = SmartNextSalah.resolve(result(times), cal(22, 0))
        assertEquals("Fajr", s.primaryName)
        assertFalse(s.isActiveHighlight)
    }
}

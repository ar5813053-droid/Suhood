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
        latitude = 28.6,
        longitude = 77.2
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

    private fun standardDay() = listOf(
        pt("Fajr", 5 * 60 + 23),
        pt("Dhuhr", 13 * 60 + 11),
        pt("Asr", 17 * 60 + 50),
        pt("Maghrib", 19 * 60 + 42),
        pt("Isha", 20 * 60 + 58)
    )

    @Test
    fun beforeFajr_nextIsFajr() {
        val s = SmartNextSalah.resolve(result(standardDay()), cal(4, 0))
        assertEquals("Fajr", s.primaryName)
        assertFalse(s.isActiveHighlight)
    }

    @Test
    fun fajrHighlight_nextDhuhr() {
        val s = SmartNextSalah.resolve(result(standardDay()), cal(5, 25))
        assertTrue(s.isActiveHighlight)
        assertEquals("Fajr", s.primaryName)
        assertEquals("Dhuhr", s.nextName)
    }

    @Test
    fun afterFajrHighlight_nextDhuhr() {
        val s = SmartNextSalah.resolve(result(standardDay()), cal(5, 40))
        assertFalse(s.isActiveHighlight)
        assertEquals("Dhuhr", s.primaryName)
    }

    @Test
    fun beforeMaghrib_nextIsMaghrib() {
        val s = SmartNextSalah.resolve(result(standardDay()), cal(19, 0))
        assertEquals("Maghrib", s.primaryName)
        assertFalse(s.isActiveHighlight)
    }

    @Test
    fun atMaghrib_highlight_nextIsha() {
        val s = SmartNextSalah.resolve(result(standardDay()), cal(19, 45))
        assertTrue(s.isActiveHighlight)
        assertEquals("Maghrib", s.primaryName)
        assertEquals("Isha", s.nextName)
    }

    @Test
    fun afterMaghribHighlight_nextIsha() {
        val s = SmartNextSalah.resolve(result(standardDay()), cal(20, 0))
        assertFalse(s.isActiveHighlight)
        assertEquals("Isha", s.primaryName)
    }

    @Test
    fun afterIsha_nextIsFajrTomorrow() {
        val s = SmartNextSalah.resolve(result(standardDay()), cal(22, 0))
        assertEquals("Fajr", s.primaryName)
        assertFalse(s.isActiveHighlight)
        assertTrue(s.countdownSeconds > 0)
    }

    @Test
    fun ishaHighlight_nextFajr() {
        val s = SmartNextSalah.resolve(result(standardDay()), cal(21, 0))
        assertTrue(s.isActiveHighlight)
        assertEquals("Isha", s.primaryName)
        assertEquals("Fajr", s.nextName)
    }
}

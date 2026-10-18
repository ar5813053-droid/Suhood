package com.suhood

import com.suhood.core.prayer.PrayerTimesCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class PrayerTimesTest {

    @Test
    fun makkah_returnsFiveTimes() {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Riyadh")).apply {
            set(2026, Calendar.OCTOBER, 18, 12, 0, 0)
        }
        val result = PrayerTimesCalculator.calculate(
            latitude = 21.4225,
            longitude = 39.8262,
            method = PrayerTimesCalculator.Method.MWL,
            calendar = cal,
            timeZoneId = "Asia/Riyadh"
        )
        assertEquals(5, result.times.size)
        assertTrue(result.times.any { it.isNext })
        assertTrue(result.isCalculated)
        result.times.forEach {
            assertTrue(it.time.matches(Regex("\\d{2}:\\d{2}")))
        }
    }

    @Test
    fun countdown_nonNegative() {
        val result = PrayerTimesCalculator.calculate(21.4225, 39.8262)
        val sec = PrayerTimesCalculator.countdownSecondsToNext(result)
        assertTrue(sec >= 0)
    }
}

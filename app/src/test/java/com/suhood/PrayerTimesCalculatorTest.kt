package com.suhood

import com.suhood.core.prayer.PrayerTimesCalculator
import com.suhood.core.prayer.SmartNextSalah
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class PrayerTimesCalculatorTest {

    private fun calAt(h: Int, m: Int, tz: String = "Asia/Kolkata"): Calendar =
        Calendar.getInstance(TimeZone.getTimeZone(tz)).apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.OCTOBER)
            set(Calendar.DAY_OF_MONTH, 22)
            set(Calendar.HOUR_OF_DAY, h)
            set(Calendar.MINUTE, m)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

    @Test
    fun delhiTimesAreDaytimeNotNight() {
        val cal = calAt(12, 0)
        val r = PrayerTimesCalculator.calculate(
            28.6139, 77.2090, calendar = cal, timeZoneId = "Asia/Kolkata"
        )
        val map = r.times.associate { it.name to it.minutesSinceMidnight }
        // Dhuhr must be around midday (11:00–14:00), not 2 AM
        val dhuhr = map["Dhuhr"]!!
        assertTrue("Dhuhr was $dhuhr min (~${dhuhr/60}:${dhuhr%60})", dhuhr in 11 * 60..14 * 60)
        val fajr = map["Fajr"]!!
        assertTrue("Fajr was $fajr", fajr in 4 * 60..7 * 60)
        val maghrib = map["Maghrib"]!!
        assertTrue("Maghrib was $maghrib", maghrib in 17 * 60..19 * 60 + 30)
        val isha = map["Isha"]!!
        assertTrue("Isha was $isha", isha in 18 * 60..22 * 60)
    }

    @Test
    fun format12hAmPmCorrect() {
        assertEquals("12:05 PM", SmartNextSalah.format12h("12:05"))
        assertEquals("2:35 PM", SmartNextSalah.format12h("14:35"))
        assertEquals("2:35 AM", SmartNextSalah.format12h("02:35"))
        assertEquals("12:00 AM", SmartNextSalah.format12h("00:00"))
    }

    @Test
    fun afternoonShowsAsrOrDhuhrNotFajr() {
        val cal = calAt(15, 30) // 3:30 PM IST
        val r = PrayerTimesCalculator.calculate(
            28.6139, 77.2090, calendar = cal, timeZoneId = "Asia/Kolkata"
        )
        val smart = SmartNextSalah.resolve(r, cal)
        // Should not primary be Fajr in the afternoon
        assertTrue(
            "Got ${smart.primaryName} at 15:30",
            smart.primaryName in listOf("Asr", "Dhuhr", "Maghrib", "Isha")
        )
    }

    @Test
    fun afterMaghribNextIsIshaOrFajr() {
        val cal = calAt(20, 30)
        val r = PrayerTimesCalculator.calculate(
            28.6139, 77.2090, calendar = cal, timeZoneId = "Asia/Kolkata"
        )
        val smart = SmartNextSalah.resolve(r, cal)
        assertTrue(
            "Got ${smart.primaryName}",
            smart.primaryName in listOf("Isha", "Fajr", "Maghrib")
        )
    }
}

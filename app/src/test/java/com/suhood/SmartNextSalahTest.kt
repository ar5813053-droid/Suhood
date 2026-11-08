package com.suhood

import com.suhood.core.model.PrayerTime
import com.suhood.core.prayer.PrayerPeriod
import com.suhood.core.prayer.PrayerTimesCalculator
import com.suhood.core.prayer.SmartNextSalah
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
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

    private fun cal(h: Int, m: Int, dayOfWeek: Int = Calendar.MONDAY): Calendar =
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.JANUARY)
            // 2026-01-02 is Friday; 2026-01-05 is Monday
            set(Calendar.DAY_OF_MONTH, if (dayOfWeek == Calendar.FRIDAY) 2 else 5)
            set(Calendar.DAY_OF_WEEK, dayOfWeek) // may be overwritten by day fields
            set(Calendar.HOUR_OF_DAY, h)
            set(Calendar.MINUTE, m)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // Force weekday after date set
            while (get(Calendar.DAY_OF_WEEK) != dayOfWeek) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
            set(Calendar.HOUR_OF_DAY, h)
            set(Calendar.MINUTE, m)
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

    @Test
    fun friday_replacesDhuhrWithJumuah_notBoth() {
        val friday = cal(10, 0, Calendar.FRIDAY)
        assertTrue(SmartNextSalah.isFriday(friday))
        val applied = SmartNextSalah.applyFridayJumuah(standardDay(), isFriday = true)
        val names = applied.map { it.name }
        assertTrue(names.contains(SmartNextSalah.JUMUAH_NAME))
        assertFalse(names.any { it.equals("Dhuhr", true) })
        assertEquals(1, names.count { it == SmartNextSalah.JUMUAH_NAME })
    }

    @Test
    fun nonFriday_keepsDhuhr() {
        val mon = cal(10, 0, Calendar.MONDAY)
        assertFalse(SmartNextSalah.isFriday(mon))
        val s = SmartNextSalah.resolve(result(standardDay()), mon)
        assertEquals("Dhuhr", s.primaryName)
        assertFalse(s.useJumuahTheme)
        assertFalse(s.allTimes.any { it.first == SmartNextSalah.JUMUAH_NAME })
    }

    @Test
    fun friday_beforeJumuah_primaryIsJumuah_withTheme() {
        val friday = cal(11, 0, Calendar.FRIDAY)
        val s = SmartNextSalah.resolve(result(standardDay()), friday)
        assertEquals(SmartNextSalah.JUMUAH_NAME, s.primaryName)
        assertTrue(s.useJumuahTheme)
        assertEquals(PrayerPeriod.JUMUAH, s.period)
        assertTrue(s.jumuahIsCalculatedReference)
        assertTrue(s.countdownSeconds > 0)
    }

    @Test
    fun friday_customJumuahTime_used() {
        val friday = cal(11, 0, Calendar.FRIDAY)
        val s = SmartNextSalah.resolve(
            result(standardDay()),
            friday,
            customJumuahTime = "13:45"
        )
        assertEquals(SmartNextSalah.JUMUAH_NAME, s.primaryName)
        assertEquals("13:45", s.primaryTime)
        assertFalse(s.jumuahIsCalculatedReference)
        assertTrue(s.useJumuahTheme)
    }

    @Test
    fun friday_duringJumuahHighlight_themeOn_nextAsr() {
        val friday = cal(13, 12, Calendar.FRIDAY) // just after 13:11
        val s = SmartNextSalah.resolve(result(standardDay()), friday)
        assertEquals(SmartNextSalah.JUMUAH_NAME, s.primaryName)
        assertTrue(s.isActiveHighlight)
        assertTrue(s.useJumuahTheme)
        assertEquals("Asr", s.nextName)
    }

    @Test
    fun friday_afterJumuah_nextAsr_themeOff() {
        val friday = cal(14, 0, Calendar.FRIDAY)
        val s = SmartNextSalah.resolve(result(standardDay()), friday)
        assertEquals("Asr", s.primaryName)
        assertFalse(s.useJumuahTheme)
        assertNotEquals(PrayerPeriod.JUMUAH, s.period)
    }

    @Test
    fun friday_evening_maghrib_notJumuah() {
        val friday = cal(19, 0, Calendar.FRIDAY)
        val s = SmartNextSalah.resolve(result(standardDay()), friday)
        assertEquals("Maghrib", s.primaryName)
        assertFalse(s.useJumuahTheme)
    }

    @Test
    fun saturday_showsDhuhrNotJumuah() {
        val sat = cal(11, 0, Calendar.SATURDAY)
        val s = SmartNextSalah.resolve(result(standardDay()), sat)
        assertEquals("Dhuhr", s.primaryName)
        assertFalse(s.useJumuahTheme)
        assertFalse(s.allTimes.any { it.first.contains("Jumu", ignoreCase = true) })
    }
}

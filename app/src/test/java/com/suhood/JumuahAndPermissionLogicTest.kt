package com.suhood

import com.suhood.core.model.PrayerTime
import com.suhood.core.permissions.BlockingMode
import com.suhood.core.permissions.PermissionApplicability
import com.suhood.core.permissions.PermissionItem
import com.suhood.core.permissions.PermissionKind
import com.suhood.core.permissions.PermissionSnapshot
import com.suhood.core.permissions.BlockingCapability
import com.suhood.core.prayer.PrayerSettings
import com.suhood.core.prayer.PrayerTimesCalculator
import com.suhood.core.prayer.SmartNextSalah
import com.suhood.core.prayer.effectiveJumuahCustomTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class JumuahAndPermissionLogicTest {

    private fun result(times: List<PrayerTime>) = PrayerTimesCalculator.Result(
        dateLabel = "T",
        times = times,
        method = PrayerTimesCalculator.Method.MWL,
        latitude = 1.0,
        longitude = 1.0
    )

    private fun pt(name: String, mins: Int) = PrayerTime(
        name = name,
        time = "%02d:%02d".format(mins / 60, mins % 60),
        minutesSinceMidnight = mins
    )

    private fun day() = listOf(
        pt("Fajr", 5 * 60),
        pt("Dhuhr", 13 * 60),
        pt("Asr", 16 * 60),
        pt("Maghrib", 18 * 60),
        pt("Isha", 19 * 60 + 30)
    )

    private fun cal(h: Int, m: Int, dow: Int): Calendar =
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.JANUARY)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, h)
            set(Calendar.MINUTE, m)
            set(Calendar.SECOND, 0)
            while (get(Calendar.DAY_OF_WEEK) != dow) add(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, h)
            set(Calendar.MINUTE, m)
        }

    @Test
    fun customJumuahTime_priority() {
        val enabled = PrayerSettings(customJumuahEnabled = true, customJumuahTime = "13:15")
        assertEquals("13:15", enabled.effectiveJumuahCustomTime())
        val disabled = PrayerSettings(customJumuahEnabled = false, customJumuahTime = "13:15")
        assertNull(disabled.effectiveJumuahCustomTime())
        val blank = PrayerSettings(customJumuahEnabled = true, customJumuahTime = "")
        assertNull(blank.effectiveJumuahCustomTime())
    }

    @Test
    fun friday_usesCustomJumuah_notDhuhr() {
        val fri = cal(11, 0, Calendar.FRIDAY)
        val s = SmartNextSalah.resolve(result(day()), fri, customJumuahTime = "13:15")
        assertEquals(SmartNextSalah.JUMUAH_NAME, s.primaryName)
        assertEquals("13:15", s.primaryTime)
        assertTrue(s.useJumuahTheme)
        assertFalse(s.jumuahIsCalculatedReference)
    }

    @Test
    fun friday_fallbackCalculated_labelled() {
        val fri = cal(11, 0, Calendar.FRIDAY)
        val s = SmartNextSalah.resolve(result(day()), fri, customJumuahTime = null)
        assertEquals(SmartNextSalah.JUMUAH_NAME, s.primaryName)
        assertEquals("13:00", s.primaryTime)
        assertTrue(s.jumuahIsCalculatedReference)
    }

    @Test
    fun saturday_dhuhrRestored() {
        val sat = cal(11, 0, Calendar.SATURDAY)
        val s = SmartNextSalah.resolve(result(day()), sat, customJumuahTime = "13:15")
        assertEquals("Dhuhr", s.primaryName)
        assertFalse(s.useJumuahTheme)
    }

    @Test
    fun permissionSnapshot_missingCount() {
        val items = listOf(
            PermissionItem(
                PermissionKind.NOTIFICATIONS, "N", "r", "f",
                granted = false, applicability = PermissionApplicability.OPTIONAL
            ),
            PermissionItem(
                PermissionKind.EXACT_ALARM, "E", "r", "f",
                granted = false, applicability = PermissionApplicability.NOT_APPLICABLE
            ),
            PermissionItem(
                PermissionKind.ACCESSIBILITY, "A", "r", "f",
                granted = true, applicability = PermissionApplicability.OPTIONAL
            )
        )
        val snap = PermissionSnapshot(
            items = items,
            missingApplicable = items.filter {
                it.applicability != PermissionApplicability.NOT_APPLICABLE && !it.granted
            },
            blockingCapability = BlockingCapability(
                mode = BlockingMode.ACCESSIBILITY,
                canDetectForeground = true,
                canPresentBlockUi = true,
                isReliableBlocking = true,
                statusMessage = "ok"
            )
        )
        assertEquals(1, snap.missingCount)
        assertFalse(snap.allApplicableComplete)
    }

    @Test
    fun blockingCapability_usageOnly_notReliable() {
        val cap = BlockingCapability(
            mode = BlockingMode.USAGE_ONLY_DETECT,
            canDetectForeground = true,
            canPresentBlockUi = false,
            isReliableBlocking = false,
            statusMessage = "detect only"
        )
        assertFalse(cap.isReliableBlocking)
        assertFalse(cap.canPresentBlockUi)
    }
}

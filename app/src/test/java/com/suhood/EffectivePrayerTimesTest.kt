package com.suhood

import com.suhood.core.prayer.EffectivePrayerTimes
import org.junit.Assert.assertEquals
import org.junit.Test

class EffectivePrayerTimesTest {
    @Test
    fun addMinutes_wrapsDay() {
        assertEquals("00:10", EffectivePrayerTimes.addMinutes("23:50", 20))
        assertEquals("04:45", EffectivePrayerTimes.addMinutes("05:00", -15))
    }
}

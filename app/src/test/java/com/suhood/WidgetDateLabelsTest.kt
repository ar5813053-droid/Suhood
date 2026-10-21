package com.suhood

import com.suhood.widgets.WidgetBridge
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetDateLabelsTest {
    @Test
    fun dateLabels_includeArabicWeekdayAndHijri() {
        val d = WidgetBridge.dateLabels()
        assertTrue(d.weekdayAr.isNotBlank())
        assertTrue(d.hijriDay in 1..30)
        assertTrue(d.hijriMonthAr.isNotBlank())
        assertTrue(d.hijriLineEn.contains("approx"))
        assertTrue(d.gregorianDay in 1..31)
    }

    @Test
    fun duaOfDay_stableAndHasArabic() {
        val dua = WidgetBridge.duaOfDay()
        assertTrue(dua.arabic.isNotBlank())
        assertTrue(dua.reference.isNotBlank())
        assertTrue(dua.translation.isNotBlank())
    }
}

package com.suhood

import com.suhood.core.database.SalahLogEntity
import com.suhood.feature.salah.SalahStats
import com.suhood.feature.salah.SalahStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class SalahStatsTest {
    @Test
    fun percentages_among_recorded_only() {
        val logs = listOf(
            SalahLogEntity("1", "2026-10-20", "Fajr", SalahStatus.JAMA_AH.key),
            SalahLogEntity("2", "2026-10-20", "Dhuhr", SalahStatus.ON_TIME.key),
            SalahLogEntity("3", "2026-10-20", "Asr", SalahStatus.LATE.key),
            SalahLogEntity("4", "2026-10-20", "Maghrib", SalahStatus.NOT_PRAYED.key)
        )
        val s = SalahStats.summarize(logs, "2026-10-20")
        assertEquals(4, s.totalRecorded)
        assertEquals(25, s.jamaahPct)
        assertEquals(25, s.onTimePct)
        assertEquals(25, s.latePct)
        assertEquals(25, s.notPrayedPct)
        assertEquals(3, s.completedOfFiveToday)
    }
}

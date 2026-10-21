package com.suhood

import com.suhood.feature.quest.QuestRules
import com.suhood.feature.quest.Ranks
import com.suhood.feature.salah.SalahStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestRulesTest {
    @Test
    fun xpValues() {
        assertEquals(3, QuestRules.XP_SALAH)
        assertEquals(15, QuestRules.XP_JUZ)
        assertEquals(50, QuestRules.TIER_XP)
    }

    @Test
    fun ranksProgress() {
        assertEquals("Mubtadi’", Ranks.forTier(0).latin)
        assertEquals("Mujtahid", Ranks.forTier(1).latin)
        assertTrue(Ranks.forTier(10).latin.contains("Mutamayyiz"))
    }

    @Test
    fun qazaPersistsAsLate() {
        assertEquals("LATE", SalahStatus.QAZA.key)
        assertEquals(SalahStatus.LATE.key, SalahStatus.QAZA.key)
    }
}

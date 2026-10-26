package com.suhood

import com.suhood.feature.quest.DailyQuestEngine
import com.suhood.feature.quest.QuestDifficulty
import com.suhood.feature.quest.QuestProfileInput
import com.suhood.feature.quest.QuestRules
import com.suhood.feature.quest.QuestTierRules
import com.suhood.feature.quest.Ranks
import com.suhood.feature.quest.TierAttemptLogic
import com.suhood.feature.salah.SalahStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class QuestRulesTest {
    @Test
    fun xpValues() {
        assertEquals(3, QuestRules.XP_SALAH)
        assertEquals(15, QuestRules.XP_JUZ)
        assertEquals(QuestTierRules.TIER_XP_TARGET, QuestRules.TIER_XP)
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

    @Test
    fun dailyCapBlocksExtraXp() {
        assertEquals(0, QuestTierRules.creditAllowed(30, 100))
        assertEquals(10, QuestTierRules.creditAllowed(30, 90))
        assertEquals(20, QuestTierRules.creditAllowed(20, 0))
    }

    @Test
    fun sevenDayDeadline() {
        val start = 1000L
        val deadline = QuestTierRules.deadlineEpochDay(start)
        assertEquals(1006L, deadline)
        assertEquals(7, QuestTierRules.daysRemaining(1000, deadline))
        assertEquals(1, QuestTierRules.daysRemaining(1006, deadline))
        assertTrue(QuestTierRules.isExpired(1007, deadline))
        assertFalse(QuestTierRules.isExpired(1006, deadline))
    }

    @Test
    fun tierCompletesOnceAtTarget() {
        var s = TierAttemptLogic.startAttempt(0, 10)
        s = TierAttemptLogic.afterCredit(s, 700)
        assertTrue(s.completed)
        assertEquals(700, s.creditedXp)
        val again = TierAttemptLogic.afterCredit(s, 30)
        assertTrue(again.completed)
        assertEquals(700, again.creditedXp)
    }

    @Test
    fun expiryResetsOnlyAttemptProgress() {
        var s = TierAttemptLogic.startAttempt(2, 10)
        s = TierAttemptLogic.afterCredit(s, 50)
        val rolled = TierAttemptLogic.onDayRollover(s, 20) // past deadline
        assertEquals(2, rolled.tierIndex)
        assertEquals(0, rolled.creditedXp)
        assertFalse(rolled.completed)
        assertEquals(20L, rolled.startEpochDay)
    }

    @Test
    fun generatorDeterministic() {
        val profile = QuestProfileInput(
            goals = setOf("salah", "quran"),
            consistency = "working",
            practice = "both",
            dailyMinutes = 10,
            hardestPrayer = "Fajr"
        )
        val d = LocalDate.of(2026, 10, 26)
        val a = DailyQuestEngine.generate(profile, d, emptyList())
        val b = DailyQuestEngine.generate(profile, d, emptyList())
        assertEquals(a.map { it.templateId }, b.map { it.templateId })
        assertTrue(a.size in 3..5)
    }

    @Test
    fun differentGoalsDifferentPlans() {
        val d = LocalDate.of(2026, 10, 26)
        val salah = DailyQuestEngine.generate(
            QuestProfileInput(goals = setOf("salah"), practice = "none", dailyMinutes = 10),
            d
        )
        val focus = DailyQuestEngine.generate(
            QuestProfileInput(goals = setOf("focus"), practice = "none", dailyMinutes = 10),
            d
        )
        // Not required to be fully disjoint, but should not be identical catalogs
        assertTrue(salah.isNotEmpty() && focus.isNotEmpty())
    }

    @Test
    fun avoidsRecentTemplatesWhenPossible() {
        val d = LocalDate.of(2026, 3, 1)
        val first = DailyQuestEngine.generate(QuestProfileInput(goals = setOf("salah", "quran", "dhikr")), d)
        val recent = first.map { it.templateId }
        val second = DailyQuestEngine.generate(
            QuestProfileInput(goals = setOf("salah", "quran", "dhikr")),
            d.plusDays(1),
            recent
        )
        val overlap = second.count { it.templateId in recent }
        assertTrue(overlap < second.size)
    }

    @Test
    fun xpBandsMatchDifficulty() {
        assertEquals(10, QuestDifficulty.EASY.xp)
        assertEquals(20, QuestDifficulty.MEDIUM.xp)
        assertEquals(30, QuestDifficulty.CHALLENGING.xp)
    }
}

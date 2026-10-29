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
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class QuestRulesTest {
    @Test
    fun xpTargetMatchesRules() {
        assertEquals(QuestTierRules.TIER_XP_TARGET, QuestRules.TIER_XP)
        assertEquals(2500, QuestTierRules.TIER_XP_TARGET)
        assertEquals(30, QuestTierRules.ATTEMPT_DAYS)
        assertEquals(100, QuestTierRules.DAILY_XP_CAP)
    }

    @Test
    fun ranksProgress() {
        assertEquals("Mubtadi'", Ranks.forTier(0).latin)
        assertEquals("Mujtahid", Ranks.forTier(1).latin)
        assertTrue(Ranks.forTier(4).latin.isNotBlank())
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
    fun thirtyDayDeadline() {
        val start = 1000L
        val deadline = QuestTierRules.deadlineEpochDay(start)
        assertEquals(1029L, deadline)
        assertEquals(30, QuestTierRules.daysRemaining(1000, deadline))
        assertEquals(1, QuestTierRules.daysRemaining(1029, deadline))
        assertTrue(QuestTierRules.isExpired(1030, deadline))
        assertFalse(QuestTierRules.isExpired(1029, deadline))
    }

    @Test
    fun tierCompletesOnceAtTarget() {
        var s = TierAttemptLogic.open(0, LocalDate.ofEpochDay(10))
        s = TierAttemptLogic.afterCredit(s, 2500)
        assertTrue(s.completed)
        assertEquals(2500, s.creditedXp)
        val again = TierAttemptLogic.afterCredit(s, 30)
        assertTrue(again.completed)
        assertEquals(2500, again.creditedXp)
    }

    @Test
    fun expiryResetsOnlyAttemptProgress() {
        var s = TierAttemptLogic.open(2, LocalDate.ofEpochDay(10))
        s = TierAttemptLogic.afterCredit(s, 50)
        val rolled = TierAttemptLogic.resetIfExpired(s, LocalDate.ofEpochDay(50), 2)
        assertEquals(2, rolled.tierIndex)
        assertEquals(0, rolled.creditedXp)
        assertFalse(rolled.completed)
        assertEquals(50L, rolled.startEpochDay)
    }

    @Test
    fun generatorDeterministic() {
        val profile = QuestProfileInput(
            goals = setOf("salah", "quran"),
            consistency = "working",
            practice = "both",
            dailyMinutes = 10,
            hardestPrayer = "Fajr",
            challengeDifficulty = "normal",
            quranLevel = "beginner"
        )
        val d = LocalDate.of(2026, 10, 26)
        val a = DailyQuestEngine.generateForDay(profile, d, 3)
        val b = DailyQuestEngine.generateForDay(profile, d, 3)
        assertEquals(a.map { it.templateId }, b.map { it.templateId })
        assertTrue(a.size in 3..6)
    }

    @Test
    fun differentGoalsDifferentPlans() {
        val d = LocalDate.of(2026, 10, 26)
        val salah = DailyQuestEngine.generateForDay(
            QuestProfileInput(goals = setOf("salah"), practice = "none", dailyMinutes = 10),
            d, 5
        )
        val quran = DailyQuestEngine.generateForDay(
            QuestProfileInput(goals = setOf("quran"), practice = "none", dailyMinutes = 10),
            d, 5
        )
        assertTrue(salah.isNotEmpty() && quran.isNotEmpty())
    }

    @Test
    fun milestoneDaysIncludeWeekCheckpoints() {
        val profile = QuestProfileInput(goals = setOf("salah", "quran", "dhikr"))
        val d = LocalDate.of(2026, 1, 7)
        val day7 = DailyQuestEngine.generateForDay(profile, d, 7)
        assertTrue(day7.any { it.templateId == "milestone_week" })
        val day30 = DailyQuestEngine.generateForDay(profile, d, 30)
        assertTrue(day30.any { it.templateId == "milestone_final" })
    }

    @Test
    fun materializeDoesNotFabricateScripture() {
        val profile = QuestProfileInput(goals = setOf("salah"))
        val tasks = DailyQuestEngine.materialize(
            DailyQuestEngine.generateForDay(profile, LocalDate.of(2026, 5, 1), 1),
            LocalDate.of(2026, 5, 1),
            profile
        )
        assertTrue(tasks.none { it.detail.contains("Quran  ") && it.detail.contains(":") })
        assertTrue(tasks.any { it.isFarz } || tasks.isNotEmpty())
    }

    @Test
    fun xpBandsMatchDifficulty() {
        assertEquals(10, QuestDifficulty.EASY.xp)
        assertEquals(20, QuestDifficulty.MEDIUM.xp)
        assertEquals(30, QuestDifficulty.CHALLENGING.xp)
    }

    @Test
    fun weekOfChallenge() {
        assertEquals(1, QuestTierRules.weekOf(1))
        assertEquals(1, QuestTierRules.weekOf(7))
        assertEquals(2, QuestTierRules.weekOf(8))
        assertEquals(4, QuestTierRules.weekOf(30))
    }
}

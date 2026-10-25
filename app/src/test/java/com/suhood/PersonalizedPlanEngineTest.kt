package com.suhood

import com.suhood.feature.onboarding.OnboardingAnswers
import com.suhood.feature.onboarding.OnboardingValidation
import com.suhood.feature.onboarding.PersonalizedPlanEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalizedPlanEngineTest {

    @Test
    fun sameAnswers_samePlan() {
        val a = OnboardingAnswers(
            goals = setOf("salah", "focus"),
            hardestPrayer = "Fajr",
            consistency = "struggling",
            reminderStyle = "Gentle",
            phoneUsage = "6+ hours",
            disttime = "evening",
            selectedAppLabels = listOf("Instagram"),
            practice = "both",
            dailyMinutes = 10
        )
        val p1 = PersonalizedPlanEngine.generate(a)
        val p2 = PersonalizedPlanEngine.generate(a)
        assertEquals(p1.mainFocus, p2.mainFocus)
        assertEquals(p1.firstDayActions.map { it.id }, p2.firstDayActions.map { it.id })
        assertEquals(p1.weeklyProgression, p2.weeklyProgression)
    }

    @Test
    fun differentGoals_changeMainFocus() {
        val salah = PersonalizedPlanEngine.generate(OnboardingAnswers(goals = setOf("salah")))
        val quran = PersonalizedPlanEngine.generate(OnboardingAnswers(goals = setOf("quran")))
        assertNotEquals(salah.mainFocus, quran.mainFocus)
        assertTrue(salah.mainFocus.contains("Salah", ignoreCase = true))
        assertTrue(quran.mainFocus.contains("Quran", ignoreCase = true))
    }

    @Test
    fun quranOnly_doesNotForceDhikrAction() {
        val p = PersonalizedPlanEngine.generate(
            OnboardingAnswers(goals = setOf("quran"), practice = "quran", dailyMinutes = 15)
        )
        assertTrue(p.firstDayActions.any { it.feature == "quran" })
        assertFalse(p.firstDayActions.any { it.feature == "dhikr" })
    }

    @Test
    fun focusWithApps_mentionsApps() {
        val p = PersonalizedPlanEngine.generate(
            OnboardingAnswers(
                goals = setOf("focus"),
                selectedAppLabels = listOf("TikTok", "Instagram")
            )
        )
        val joined = p.distractionStrategy.joinToString()
        assertTrue(joined.contains("TikTok") || joined.contains("Instagram"))
    }

    @Test
    fun validation_goalsRequireOne() {
        assertFalse(OnboardingValidation.goalsValid(emptySet()))
        assertTrue(OnboardingValidation.goalsValid(setOf("salah")))
    }

    @Test
    fun validation_aboutRequiresAgeAndLife() {
        assertFalse(OnboardingValidation.aboutValid("", "Student"))
        assertFalse(OnboardingValidation.aboutValid("18–24", ""))
        assertTrue(OnboardingValidation.aboutValid("18–24", "Student"))
    }
}

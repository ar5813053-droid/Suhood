package com.suhood

import com.suhood.core.data.DemoData
import org.junit.Assert.*
import org.junit.Test

class QuizAndTasbeehTest {

    @Test
    fun quizQuestions_haveValidCorrectIndex() {
        DemoData.quizQuestions.forEach { q ->
            assertTrue("correctIndex out of range for ${q.id}", q.correctIndex in q.options.indices)
            assertTrue("options empty for ${q.id}", q.options.isNotEmpty())
            assertTrue("explanation blank for ${q.id}", q.explanation.isNotBlank())
        }
    }

    @Test
    fun quizEasyQuestions_exist() {
        val easy = DemoData.quizQuestions.filter { it.difficulty.name == "EASY" }
        assertTrue(easy.isNotEmpty())
    }

    @Test
    fun dhikrItems_havePositiveTargets() {
        DemoData.dhikrItems.forEach {
            assertTrue(it.defaultTarget > 0)
            assertTrue(it.arabic.isNotBlank())
        }
    }

    @Test
    fun audioTracks_havePositiveDuration() {
        DemoData.audioTracks.forEach {
            assertTrue(it.durationSeconds > 0)
            assertTrue(it.title.isNotBlank())
        }
    }

    @Test
    fun surahList_containsFatihah() {
        val fatihah = DemoData.surahList.find { it.id == 1 }
        assertNotNull(fatihah)
        assertEquals(7, fatihah!!.ayahCount)
    }

    @Test
    fun demoPrayerSchedule_hasFiveTimes() {
        val schedule = DemoData.demoPrayerSchedule()
        assertEquals(5, schedule.times.size)
        assertTrue(schedule.times.any { it.isNext })
    }
}

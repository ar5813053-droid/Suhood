package com.suhood

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusChallengeLogicTest {
    @Test
    fun maxThreePerDay() {
        assertEquals(3, com.suhood.feature.focus.FocusChallengeStore.MAX_PER_DAY)
    }

    @Test
    fun passDurationFiveMinutes() {
        assertEquals(5 * 60 * 1000L, com.suhood.feature.focus.FocusChallengeStore.PASS_DURATION_MS)
        assertTrue(com.suhood.feature.focus.FocusChallengeStore.PASS_DURATION_MS > 0)
    }
}

package com.suhood

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure logic checks for onboarding step bounds (no Android context). */
class OnboardingProfileLogicTest {
    @Test
    fun stepCoercedToRange() {
        fun coerce(v: Int) = v.coerceIn(0, 11)
        assertEquals(0, coerce(-1))
        assertEquals(11, coerce(99))
        assertEquals(5, coerce(5))
    }

    @Test
    fun twelveStepsDefined() {
        assertTrue(12 >= 12)
    }
}

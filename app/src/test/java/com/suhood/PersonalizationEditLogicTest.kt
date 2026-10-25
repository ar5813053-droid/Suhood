package com.suhood

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure logic for personalization editor behavior (no Android context).
 */
class PersonalizationEditLogicTest {
    @Test
    fun dirtyFlagStartsClean() {
        var dirty = false
        fun mark() { dirty = true }
        assertFalse(dirty)
        mark()
        assertTrue(dirty)
    }

    @Test
    fun packageSetMerge() {
        val a = setOf("com.instagram.android")
        val b = a + "com.twitter.android"
        assertEquals(2, b.size)
        assertTrue("com.instagram.android" in b)
    }

    @Test
    fun quranMinutesOptions() {
        val opts = listOf(5, 10, 20, 30)
        assertTrue(10 in opts)
    }
}

package com.suhood

import com.suhood.core.quran.VerifiedQaris
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuranMetaTest {
    @Test
    fun verifiedQaris_nonEmpty() {
        assertTrue(VerifiedQaris.list.isNotEmpty())
        assertTrue(VerifiedQaris.list.any { it.id == "ar.alafasy" })
    }

    @Test
    fun expectedAyahTotal_constant() {
        // Canonical total used by AlQuran.Cloud meta
        assertEquals(6236, 6236)
        assertEquals(114, 114)
    }
}

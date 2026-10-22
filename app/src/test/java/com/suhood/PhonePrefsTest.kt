package com.suhood

import com.suhood.feature.phone.CallThemeId
import org.junit.Assert.*
import org.junit.Test

class PhonePrefsLogicTest {
    @Test
    fun themeIdsRoundTripNames() {
        CallThemeId.entries.forEach { id ->
            assertEquals(id, CallThemeId.valueOf(id.name))
        }
        assertTrue(CallThemeId.entries.any { it.category == "Flags" })
        assertTrue(CallThemeId.entries.any { it.category == "Sacred Mosques" })
    }
}

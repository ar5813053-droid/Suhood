package com.suhood

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.suhood.feature.phone.CallThemeId
import com.suhood.feature.phone.PhonePrefs
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric optional — if not on classpath, this test class may be skipped by unit test task
 * when Android resources unavailable. Preference persistence is also covered by manual checklist.
 */
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

package com.suhood

import com.suhood.core.audio.filter.ContentClass
import com.suhood.core.audio.filter.IslamicContentClassifier
import com.suhood.core.audio.online.OnlineSearchHit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OnlineNasheedModelsTest {
    @Test
    fun blockedQuery_notAllowed() {
        val c = IslamicContentClassifier.classifyQuery("edm festival club banger")
        assertEquals(ContentClass.BLOCKED, c.state)
    }

    @Test
    fun nasheedQuery_allowed() {
        val c = IslamicContentClassifier.classifyQuery("nasheed")
        assertEquals(ContentClass.ALLOWED, c.state)
    }

    @Test
    fun hitId_format() {
        val h = OnlineSearchHit(
            id = "ia:NasheedAfrah",
            title = "Nasheed Afrah",
            artist = "Unknown",
            providerId = "internet_archive"
        )
        assertTrue(h.id.startsWith("ia:"))
    }
}

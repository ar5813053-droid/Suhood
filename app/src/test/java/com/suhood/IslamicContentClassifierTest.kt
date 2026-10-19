package com.suhood

import com.suhood.core.audio.filter.ContentClass
import com.suhood.core.audio.filter.IslamicContentClassifier
import com.suhood.core.model.AudioTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IslamicContentClassifierTest {

    private fun track(
        title: String,
        category: String = "unknown",
        isDemo: Boolean = false
    ) = AudioTrack(
        id = "x", title = title, creator = "Artist", collection = "",
        categoryId = category, language = "en", durationSeconds = 60, isDemo = isDemo
    )

    @Test
    fun nasheedCategory_allowed() {
        val c = IslamicContentClassifier.classify(track("Anything", "nasheeds"))
        assertEquals(ContentClass.ALLOWED, c.state)
    }

    @Test
    fun tilawat_allowed() {
        val c = IslamicContentClassifier.classify(track("Al-Fatihah", "tilawat"))
        assertEquals(ContentClass.ALLOWED, c.state)
    }

    @Test
    fun demoSample_allowed() {
        val c = IslamicContentClassifier.classify(track("Tone", isDemo = true))
        assertEquals(ContentClass.ALLOWED, c.state)
    }

    @Test
    fun wordSongAlone_notBlockedIfNasheedSignal() {
        val c = IslamicContentClassifier.classify(track("Nasheed song of peace", "unknown"))
        assertEquals(ContentClass.ALLOWED, c.state)
    }

    @Test
    fun clubBanger_blocked() {
        val c = IslamicContentClassifier.classify(track("Summer club banger mix", "pop"))
        assertEquals(ContentClass.BLOCKED, c.state)
    }

    @Test
    fun filterRemovesBlocked() {
        val list = listOf(
            track("Ya Rahman", "nasheeds"),
            track("EDM festival night", "pop")
        )
        val f = IslamicContentClassifier.filterTracks(list)
        assertTrue(f.any { it.title.contains("Rahman") })
        assertFalse(f.any { it.title.contains("EDM") })
    }
}

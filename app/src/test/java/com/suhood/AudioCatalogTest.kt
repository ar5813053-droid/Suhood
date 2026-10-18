package com.suhood

import com.suhood.core.audio.LocalSampleCatalogProvider
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioCatalogTest {
    private val catalog = LocalSampleCatalogProvider()

    @Test
    fun search_findsArabicTitle() {
        val hits = catalog.search("رحمن")
        assertTrue(hits.isNotEmpty())
    }

    @Test
    fun search_emptyQuery_returnsEmpty() {
        assertTrue(catalog.search("").isEmpty())
    }

    @Test
    fun allTracks_nonEmpty() {
        assertTrue(catalog.allTracks().isNotEmpty())
    }
}

package com.suhood

import com.suhood.core.quran.FawazQuranProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FawazQuranParseTest {
    @Test
    fun parseEdition_mapsChapterVerse() {
        val json = """
        {"quran":[
          {"chapter":1,"verse":1,"text":"In the name of God"},
          {"chapter":1,"verse":2,"text":"Praise be to God"},
          {"chapter":2,"verse":1,"text":"Alif Lam Mim"}
        ]}
        """.trimIndent()
        val verses = FawazQuranProvider.parseEditionJson(json)
        assertEquals(3, verses.size)
        val map = FawazQuranProvider.verseMap(verses)
        assertEquals("In the name of God", map[1 to 1])
        assertEquals("Alif Lam Mim", map[2 to 1])
        assertTrue(map.containsKey(1 to 2))
    }
}

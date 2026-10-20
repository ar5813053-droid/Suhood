package com.suhood

import com.suhood.core.data.DemoData
import com.suhood.core.dua.DuaSearch
import org.junit.Assert.assertTrue
import org.junit.Test

class DuaSearchTest {
    @Test
    fun sleep_hinglish_finds_before_sleeping() {
        val r = DuaSearch.search("sone ki dua", DemoData.duaItems)
        assertTrue(r.any { it.id == "2" || it.title.contains("sleep", true) })
    }

    @Test
    fun clothes_hinglish() {
        val r = DuaSearch.search("kapde pehenne ki dua", DemoData.duaItems)
        assertTrue(r.any { it.id == "5" })
    }

    @Test
    fun travel_safar() {
        val r = DuaSearch.search("safar ki dua", DemoData.duaItems)
        assertTrue(r.any { it.category == "Travel" })
    }

    @Test
    fun distress_pareshani() {
        val r = DuaSearch.search("pareshani ki dua", DemoData.duaItems)
        assertTrue(r.any { it.category == "Distress" })
    }
}

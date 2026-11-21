package com.suhood

import com.suhood.core.quran.AyahText
import com.suhood.core.quran.TranslationEditions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuranMultiTranslationTest {
    @Test
    fun availableEditions_includeMajorLanguages() {
        val langs = TranslationEditions.availableTextEditions.map { it.langCode }.toSet()
        listOf("en", "ur", "hi", "bn", "tr", "fr", "de", "es", "id", "ms", "fa", "ru", "zh", "ta", "ml")
            .forEach { assertTrue("missing $it", it in langs) }
    }

    @Test
    fun romanEntries_markedUnavailable() {
        val roman = TranslationEditions.textEditions.filter { it.langCode.contains("Latn") }
        assertTrue(roman.isNotEmpty())
        assertTrue(roman.all { !it.available })
    }

    @Test
    fun ayahText_holdsMultipleTranslations() {
        val a = AyahText(
            globalNumber = 1,
            numberInSurah = 1,
            arabic = "بِسْمِ اللَّهِ",
            translation = "In the name of Allah",
            translations = mapOf(
                "en.sahih" to "In the name of Allah",
                "ur.jalandhry" to "الله کے نام سے"
            )
        )
        assertEquals(2, a.translations.size)
        assertEquals("In the name of Allah", a.translation)
    }

    @Test
    fun audioOnly_forEnglishAndUrdu() {
        assertTrue(TranslationEditions.hasAudioForText("en.sahih"))
        assertTrue(TranslationEditions.hasAudioForText("ur.jalandhry"))
        assertFalse(TranslationEditions.hasAudioForText("hi.hindi"))
        assertFalse(TranslationEditions.hasAudioForText("tr.diyanet"))
    }
}

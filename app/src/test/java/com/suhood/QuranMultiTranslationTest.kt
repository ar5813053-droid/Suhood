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
    fun oneEditionPerLanguage() {
        val codes = TranslationEditions.availableTextEditions.map { it.langCode.lowercase() }
        assertEquals(codes.size, codes.toSet().size)
    }

    @Test
    fun ayahText_holdsTranslation() {
        val a = AyahText(
            globalNumber = 1,
            numberInSurah = 1,
            arabic = "بِسْمِ اللَّهِ",
            translation = "In the name of Allah",
            translations = mapOf("en.sahih" to "In the name of Allah")
        )
        assertEquals("In the name of Allah", a.translation)
        assertEquals(1, a.translations.size)
    }

    @Test
    fun audioOnly_forEnglishAndUrdu() {
        assertTrue(TranslationEditions.hasAudioForText("en.sahih"))
        assertTrue(TranslationEditions.hasAudioForText("ur.jalandhry"))
        assertFalse(TranslationEditions.hasAudioForText("hi.hindi"))
        assertFalse(TranslationEditions.hasAudioForText("tr.diyanet"))
    }

    @Test
    fun preferredLanguages_humanLabelsOnly() {
        val list = TranslationEditions.preferredLanguages()
        assertTrue(list.isNotEmpty())
        list.forEach { opt ->
            assertFalse(opt.language.contains('_'))
            assertFalse(opt.displayLabel.contains("en.sahih"))
            assertTrue(opt.edition.available)
        }
        assertTrue(list.any { it.edition.id == "en.sahih" })
        assertTrue(list.any { it.edition.id == "ur.jalandhry" })
    }
}

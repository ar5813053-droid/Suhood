package com.suhood

import com.suhood.core.quran.CanonicalAyahId
import com.suhood.core.quran.TranslationEditions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ensures Arabic recitation and translation audio always share the same
 * canonical Ayah identity (never list-index or stale queue position).
 */
class QuranAyahSyncTest {

    @Test
    fun mediaIds_roundTrip_sameIdentity() {
        val id = CanonicalAyahId(surah = 2, ayahInSurah = 255, globalNumber = 262)
        val ar = id.arabicMediaId()
        val tr = id.translationMediaId()
        assertEquals(id, CanonicalAyahId.parseMediaId(ar))
        assertEquals(id, CanonicalAyahId.parseMediaId(tr))
        assertFalse(CanonicalAyahId.isTranslationMediaId(ar))
        assertTrue(CanonicalAyahId.isTranslationMediaId(tr))
        assertEquals(262, CanonicalAyahId.globalNumberFromMediaId(ar))
        assertEquals(262, CanonicalAyahId.globalNumberFromMediaId(tr))
    }

    @Test
    fun differentAyahs_neverShareMediaId() {
        val a1 = CanonicalAyahId(1, 1, 1)
        val a2 = CanonicalAyahId(1, 2, 2)
        assertNotEquals(a1.arabicMediaId(), a2.arabicMediaId())
        assertNotEquals(a1.translationMediaId(), a2.translationMediaId())
        assertNotEquals(a1.translationMediaId(), a2.arabicMediaId())
    }

    @Test
    fun everyAyahFileName_matchesSurahAndAyah() {
        assertEquals("001001.mp3", TranslationEditions.everyAyahFileName(1, 1))
        assertEquals("002255.mp3", TranslationEditions.everyAyahFileName(2, 255))
        assertEquals("114006.mp3", TranslationEditions.everyAyahFileName(114, 6))
    }

    @Test
    fun translationUrl_embedsSameSurahAyah() {
        val en = TranslationEditions.audioById("en.walk")
        assertNotNull(en)
        val url = TranslationEditions.everyAyahUrl(en!!.everyAyahFolder!!, 2, 255)
        assertTrue(url.endsWith("/002255.mp3"))
        assertTrue(url.contains("Sahih_Intnl_Ibrahim_Walk"))

        val ur = TranslationEditions.audioById("ur.khan")
        assertNotNull(ur)
        val urUrl = TranslationEditions.everyAyahUrl(ur!!.everyAyahFolder!!, 1, 1)
        assertTrue(urUrl.endsWith("/001001.mp3"))
        assertTrue(urUrl.contains("urdu_shamshad_ali_khan"))
    }

    @Test
    fun audioEdition_matchesTextLanguage() {
        val enAudio = TranslationEditions.audioForTextEdition("en.sahih")
        assertNotNull(enAudio)
        assertEquals("en", enAudio!!.langCode)

        val urAudio = TranslationEditions.audioForTextEdition("ur.jalandhry")
        assertNotNull(urAudio)
        assertEquals("ur", urAudio!!.langCode)

        // Hindi has text but no verified verse audio
        assertNull(TranslationEditions.audioForTextEdition("hi.hindi"))
    }

    @Test
    fun legacyMediaId_globalOnly_stillParsed() {
        assertEquals(5, CanonicalAyahId.globalNumberFromMediaId("quran:5"))
        assertEquals(5, CanonicalAyahId.globalNumberFromMediaId("quran-tr:5"))
        assertNull(CanonicalAyahId.parseMediaId("quran:5")) // full identity requires s/a/g form
    }

    @Test
    fun invalidIdentity_rejected() {
        assertTrue(runCatching { CanonicalAyahId(0, 1, 1) }.isFailure)
        assertTrue(runCatching { CanonicalAyahId(1, 0, 1) }.isFailure)
        assertTrue(runCatching { CanonicalAyahId(1, 1, 0) }.isFailure)
        assertTrue(runCatching { CanonicalAyahId(115, 1, 1) }.isFailure)
    }

    @Test
    fun queuePairing_arabicThenTranslation_sameGlobal() {
        val ayahs = listOf(
            CanonicalAyahId(1, 1, 1),
            CanonicalAyahId(1, 2, 2),
            CanonicalAyahId(1, 3, 3)
        )
        val items = mutableListOf<Pair<String, Int>>()
        for (a in ayahs) {
            items += a.arabicMediaId() to a.globalNumber
            items += a.translationMediaId() to a.globalNumber
        }
        assertEquals(6, items.size)
        // Each consecutive pair must share the same global number
        for (i in items.indices step 2) {
            assertEquals(items[i].second, items[i + 1].second)
            assertFalse(CanonicalAyahId.isTranslationMediaId(items[i].first))
            assertTrue(CanonicalAyahId.isTranslationMediaId(items[i + 1].first))
        }
    }
}

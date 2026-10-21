package com.suhood.core.quran

import org.json.JSONObject

/**
 * Quran text, Urdu translation, and audio via the public AlQuran.Cloud API
 * (Islamic Network). No API key required for these endpoints.
 *
 * Text: https://api.alquran.cloud/v1/
 * Audio CDN: https://cdn.islamic.network/quran/audio/{bitrate}/{edition}/{ayah}.mp3
 *
 * Attribution: AlQuran.Cloud / Islamic Network — use responsibly; do not claim
 * ownership of the Quran text or recitations.
 */
class AlQuranCloudProvider(
    private val arabicEdition: String = "quran-uthmani",
    private val urduEdition: String = "ur.jalandhry"
) : QuranTextProvider, QuranTranslationProvider, QuranAudioProvider {

    override suspend fun listSurahs(): List<SurahMeta> {
        val json = HttpJson.get("$BASE/meta")
        val refs = JSONObject(json).getJSONObject("data")
            .getJSONObject("surahs").getJSONArray("references")
        return buildList {
            for (i in 0 until refs.length()) {
                val o = refs.getJSONObject(i)
                add(
                    SurahMeta(
                        number = o.getInt("number"),
                        nameArabic = o.getString("name"),
                        nameEnglish = o.getString("englishName"),
                        nameTranslated = o.optString("englishNameTranslation"),
                        ayahCount = o.getInt("numberOfAyahs"),
                        revelationType = o.optString("revelationType")
                    )
                )
            }
        }
    }

    override suspend fun loadSurahArabic(surah: Int): SurahContent {
        val json = HttpJson.get("$BASE/surah/$surah/$arabicEdition")
        return parseSurah(json, translation = false)
    }

    override suspend fun loadSurahWithUrdu(surah: Int): SurahContent {
        // Multi-edition: arabic + urdu in one call
        val json = HttpJson.get(
            "$BASE/surah/$surah/editions/$arabicEdition,$urduEdition"
        )
        val root = JSONObject(json)
        val data = root.getJSONArray("data")
        val ar = data.getJSONObject(0)
        val ur = data.getJSONObject(1)
        val meta = SurahMeta(
            number = ar.getInt("number"),
            nameArabic = ar.getString("name"),
            nameEnglish = ar.getString("englishName"),
            nameTranslated = ar.optString("englishNameTranslation"),
            ayahCount = ar.getInt("numberOfAyahs"),
            revelationType = ar.optString("revelationType")
        )
        val arAyahs = ar.getJSONArray("ayahs")
        val urAyahs = ur.getJSONArray("ayahs")
        val ayahs = buildList {
            for (i in 0 until arAyahs.length()) {
                val a = arAyahs.getJSONObject(i)
                val u = urAyahs.optJSONObject(i)
                add(
                    AyahText(
                        globalNumber = a.getInt("number"),
                        numberInSurah = a.getInt("numberInSurah"),
                        arabic = a.getString("text").trim().removePrefix("\uFEFF"),
                        translation = u?.optString("text").orEmpty()
                    )
                )
            }
        }
        return SurahContent(meta, ayahs)
    }

    override fun ayahAudioUrl(qariId: String, globalAyahNumber: Int, bitrate: Int): String =
        "https://cdn.islamic.network/quran/audio/$bitrate/$qariId/$globalAyahNumber.mp3"

    override fun surahAudioUrl(qariId: String, surah: Int, bitrate: Int): String =
        "https://cdn.islamic.network/quran/audio-surah/$bitrate/$qariId/$surah.mp3"

    override fun translatorAttribution(): String =
        "Urdu translation: Fateh Muhammad Jalandhry (ur.jalandhry) via AlQuran.Cloud"

    private fun parseSurah(json: String, translation: Boolean): SurahContent {
        val data = JSONObject(json).getJSONObject("data")
        val meta = SurahMeta(
            number = data.getInt("number"),
            nameArabic = data.getString("name"),
            nameEnglish = data.getString("englishName"),
            nameTranslated = data.optString("englishNameTranslation"),
            ayahCount = data.getInt("numberOfAyahs"),
            revelationType = data.optString("revelationType")
        )
        val arr = data.getJSONArray("ayahs")
        val ayahs = buildList {
            for (i in 0 until arr.length()) {
                val a = arr.getJSONObject(i)
                add(
                    AyahText(
                        globalNumber = a.getInt("number"),
                        numberInSurah = a.getInt("numberInSurah"),
                        arabic = a.getString("text").trim().removePrefix("\uFEFF"),
                        translation = if (translation) a.optString("text") else ""
                    )
                )
            }
        }
        return SurahContent(meta, ayahs)
    }


    /** Madinah Mushaf page 1–604 (quran-uthmani edition). */
    suspend fun loadPage(page: Int): PageContent {
        require(page in 1..604) { "Page must be 1–604" }
        val json = HttpJson.get("$BASE/page/$page/$arabicEdition")
        val data = JSONObject(json).getJSONObject("data")
        val ayahsJson = data.getJSONArray("ayahs")
        val ayahs = buildList {
            for (i in 0 until ayahsJson.length()) {
                val a = ayahsJson.getJSONObject(i)
                val surahObj = a.optJSONObject("surah")
                add(
                    PageAyah(
                        globalNumber = a.getInt("number"),
                        numberInSurah = a.getInt("numberInSurah"),
                        surahNumber = surahObj?.optInt("number") ?: a.optInt("surah"),
                        surahName = surahObj?.optString("name").orEmpty(),
                        arabic = a.getString("text").trim().removePrefix("﻿")
                    )
                )
            }
        }
        return PageContent(pageNumber = page, ayahs = ayahs)
    }


    suspend fun loadSurahWithTranslation(surah: Int, translationEdition: String): SurahContent {
        val arJson = HttpJson.get("$BASE/surah/$surah/$arabicEdition")
        val trJson = HttpJson.get("$BASE/surah/$surah/$translationEdition")
        val ar = JSONObject(arJson).getJSONObject("data")
        val tr = JSONObject(trJson).getJSONObject("data")
        val meta = SurahMeta(
            number = ar.getInt("number"),
            nameArabic = ar.getString("name"),
            nameEnglish = ar.getString("englishName"),
            nameTranslated = ar.optString("englishNameTranslation"),
            ayahCount = ar.getInt("numberOfAyahs"),
            revelationType = ar.optString("revelationType")
        )
        val arAyahs = ar.getJSONArray("ayahs")
        val trAyahs = tr.getJSONArray("ayahs")
        val ayahs = buildList {
            for (i in 0 until arAyahs.length()) {
                val a = arAyahs.getJSONObject(i)
                val u = trAyahs.optJSONObject(i)
                add(
                    AyahText(
                        globalNumber = a.getInt("number"),
                        numberInSurah = a.getInt("numberInSurah"),
                        arabic = a.getString("text"),
                        translation = u?.optString("text").orEmpty()
                    )
                )
            }
        }
        return SurahContent(meta, ayahs)
    }

    fun translationAudioUrl(editionId: String, globalAyahNumber: Int, bitrate: Int = 128): String =
        "https://cdn.islamic.network/quran/audio/$bitrate/$editionId/$globalAyahNumber.mp3"

    companion object {
        const val BASE = "https://api.alquran.cloud/v1"
    }
}
interface QuranTextProvider {
    suspend fun listSurahs(): List<SurahMeta>
    suspend fun loadSurahArabic(surah: Int): SurahContent
}

interface QuranTranslationProvider {
    suspend fun loadSurahWithUrdu(surah: Int): SurahContent
    fun translatorAttribution(): String
}

interface QuranAudioProvider {
    fun ayahAudioUrl(qariId: String, globalAyahNumber: Int, bitrate: Int = 128): String
    fun surahAudioUrl(qariId: String, surah: Int, bitrate: Int = 128): String
}

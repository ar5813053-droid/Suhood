package com.suhood.core.quran

import org.json.JSONObject

/**
 * Remote client for Fawaz Ahmed Quran API editions hosted on jsDelivr.
 * Does not redistribute bundled translation text in the APK — downloads on demand.
 */
object FawazQuranProvider {
    const val EDITIONS_URL =
        "https://cdn.jsdelivr.net/gh/fawazahmed0/quran-api@1/editions.min.json"
    const val EDITION_MIN_URL =
        "https://cdn.jsdelivr.net/gh/fawazahmed0/quran-api@1/editions/%s.min.json"

    suspend fun fetchCatalogue(): List<FawazEdition> {
        val json = HttpJson.get(EDITIONS_URL, timeoutMs = 30_000)
        val root = JSONObject(json)
        val out = ArrayList<FawazEdition>(root.length())
        val keys = root.keys()
        while (keys.hasNext()) {
            val id = keys.next()
            val o = root.optJSONObject(id) ?: continue
            out.add(
                FawazEdition(
                    id = id,
                    name = o.optString("name").orEmpty(),
                    author = o.optString("author").orEmpty(),
                    language = o.optString("language").orEmpty().ifBlank { "Unknown" },
                    direction = o.optString("direction").orEmpty().ifBlank { "ltr" },
                    source = o.optString("source").orEmpty(),
                    comments = o.optString("comments").orEmpty(),
                    linkMin = o.optString("linkmin").ifBlank {
                        EDITION_MIN_URL.format(id.replace('_', '-').let {
                            // API uses hyphenated names in URLs often; prefer linkmin from JSON
                            EDITION_MIN_URL.format(id)
                        })
                    }.ifBlank { EDITION_MIN_URL.format(id) },
                    linkFull = o.optString("link").orEmpty()
                )
            )
        }
        return out.sortedWith(compareBy({ it.language.lowercase() }, { it.author.lowercase() }))
    }

    fun groupByLanguage(editions: List<FawazEdition>): List<FawazLanguageGroup> {
        return editions
            .groupBy { it.language.ifBlank { "Unknown" } }
            .map { (lang, list) -> FawazLanguageGroup(lang, list.sortedBy { it.author }) }
            .sortedBy { it.language.lowercase() }
    }

    /**
     * Load full edition verses. Uses chapter+verse identity — never list index alone.
     */
    suspend fun fetchEditionVerses(edition: FawazEdition): List<FawazVerse> {
        val url = edition.linkMin.ifBlank { EDITION_MIN_URL.format(edition.id) }
        val json = HttpJson.get(url, timeoutMs = 60_000)
        return parseEditionJson(json)
    }

    fun parseEditionJson(json: String): List<FawazVerse> {
        val root = JSONObject(json)
        val arr = root.optJSONArray("quran") ?: return emptyList()
        val out = ArrayList<FawazVerse>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val ch = o.optInt("chapter", -1)
            val v = o.optInt("verse", -1)
            if (ch in 1..114 && v >= 1) {
                out.add(FawazVerse(ch, v, o.optString("text").orEmpty()))
            }
        }
        return out
    }

    /** Map (surah, ayahInSurah) → text for merging into CanonicalAyahId-based reader. */
    fun verseMap(verses: List<FawazVerse>): Map<Pair<Int, Int>, String> {
        val m = HashMap<Pair<Int, Int>, String>(verses.size)
        for (v in verses) {
            if (v.text.isNotBlank()) m[v.chapter to v.verse] = v.text
        }
        return m
    }

    /**
     * Preferred trending edition ids (exact keys from editions.min.json).
     * Runtime [QuranLibraryRepository.trending] also matches stems and always
     * surfaces Roman Urdu / Roman Hindi when present.
     */
    val trendingPreferredIds = listOf(
        "eng_abdelhaleem",
        "eng_mustafakhattaba",
        "urd_fatehmuhammadja",
        "urd_abulaalamaududi_la",
        "urd_muhammadjunagar_la",
        "hin_suhelfarooqkhan_la",
        "hin_muhammadfarooqk_la",
        "ben_muhiuddinkhan",
        "tur_diyanetisleri",
        "fra_muhammadhamidul",
        "spa_muhammadisagarc",
        "ind_indonesianislam",
        "rus_ministryofawqaf"
    )
}

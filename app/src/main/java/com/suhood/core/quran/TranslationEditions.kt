package com.suhood.core.quran

/**
 * Curated translation editions from AlQuran.Cloud / Islamic Network.
 * Text editions load ayah translations; audio editions play verse-by-verse
 * translation audio from the public CDN (same path pattern as Arabic qaris).
 *
 * Attribution: AlQuran.Cloud / Islamic Network — not owned by Suhood.
 */
data class TranslationEdition(
    val id: String,
    val language: String,
    val name: String,
    val hasAudio: Boolean = false
)

object TranslationEditions {
    /** Text translations for on-screen display. */
    val textEditions = listOf(
        TranslationEdition("en.sahih", "English", "Sahih International"),
        TranslationEdition("en.pickthall", "English", "Pickthall"),
        TranslationEdition("en.yusufali", "English", "Yusuf Ali"),
        TranslationEdition("en.asad", "English", "Muhammad Asad"),
        TranslationEdition("ur.jalandhry", "Urdu", "Fateh Muhammad Jalandhry"),
        TranslationEdition("ur.ahmedali", "Urdu", "Ahmed Ali"),
        TranslationEdition("ur.jawadi", "Urdu", "Syed Zeeshan Haider Jawadi"),
        TranslationEdition("hi.hindi", "Hindi", "Suhel Farooq Khan & Naik"),
        TranslationEdition("bn.bengali", "Bengali", "Muhiuddin Khan"),
        TranslationEdition("id.indonesian", "Indonesian", "Indonesian Ministry"),
        TranslationEdition("tr.diyanet", "Turkish", "Diyanet İşleri"),
        TranslationEdition("fr.hamidullah", "French", "Muhammad Hamidullah"),
        TranslationEdition("de.aburida", "German", "Abu Rida"),
        TranslationEdition("es.cortes", "Spanish", "Julio Cortes"),
        TranslationEdition("ru.kuliev", "Russian", "Elmir Kuliev"),
        TranslationEdition("bs.mlivo", "Bosnian", "Besim Korkut"),
        TranslationEdition("nl.siregar", "Dutch", "Sofian S. Siregar"),
        TranslationEdition("pt.elhayek", "Portuguese", "Samir El-Hayek"),
        TranslationEdition("fa.makarem", "Persian", "Naser Makarem Shirazi"),
        TranslationEdition("zh.jian", "Chinese", "Ma Jian")
    )

    /**
     * Editions known to have verse-by-verse audio on
     * cdn.islamic.network/quran/audio/{bitrate}/{id}/{ayah}.mp3
     */
    val audioEditions = listOf(
        TranslationEdition("en.walk", "English", "Ibrahim Walk", hasAudio = true),
        TranslationEdition("ur.khan", "Urdu", "Shamshad Ali Khan", hasAudio = true)
    )

    fun textById(id: String): TranslationEdition? = textEditions.find { it.id == id }
    fun audioById(id: String): TranslationEdition? = audioEditions.find { it.id == id }

    fun searchText(query: String): List<TranslationEdition> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return textEditions
        return textEditions.filter {
            it.language.lowercase().contains(q) ||
                it.name.lowercase().contains(q) ||
                it.id.lowercase().contains(q)
        }
    }

    fun searchAudio(query: String): List<TranslationEdition> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return audioEditions
        return audioEditions.filter {
            it.language.lowercase().contains(q) ||
                it.name.lowercase().contains(q) ||
                it.id.lowercase().contains(q)
        }
    }
}

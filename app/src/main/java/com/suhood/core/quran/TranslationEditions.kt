package com.suhood.core.quran

/**
 * Curated translation editions.
 * Text: AlQuran.Cloud. Audio: everyayah.com verse files (SSS AAA = surah/ayah padded).
 * Attribution: sources retain ownership; Suhood does not own Quran text or audio.
 */
data class TranslationEdition(
    val id: String,
    val language: String,
    val name: String,
    val hasAudio: Boolean = false,
    /** everyayah.com relative folder under /data/ when hasAudio */
    val everyAyahFolder: String? = null
)

object TranslationEditions {
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
     * Verified verse-by-verse translation audio on everyayah.com
     * (cdn.islamic.network returns 403 for translation edition ids).
     */
    val audioEditions = listOf(
        TranslationEdition(
            id = "en.walk",
            language = "English",
            name = "Sahih Intl · Ibrahim Walk",
            hasAudio = true,
            everyAyahFolder = "English/Sahih_Intnl_Ibrahim_Walk_192kbps"
        ),
        TranslationEdition(
            id = "ur.khan",
            language = "Urdu",
            name = "Shamshad Ali Khan",
            hasAudio = true,
            everyAyahFolder = "translations/urdu_shamshad_ali_khan_46kbps"
        )
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

    /** everyayah file name: SSSAAA.mp3 (surah 3 digits + ayah-in-surah 3 digits). */
    fun everyAyahFileName(surah: Int, ayahInSurah: Int): String =
        "%03d%03d.mp3".format(surah.coerceIn(1, 114), ayahInSurah.coerceAtLeast(1))

    fun everyAyahUrl(folder: String, surah: Int, ayahInSurah: Int): String =
        "https://everyayah.com/data/$folder/${everyAyahFileName(surah, ayahInSurah)}"
}

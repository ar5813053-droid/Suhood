package com.suhood.core.quran

/**
 * Text: AlQuran.Cloud. Audio: everyayah.com (SSS AAA file names).
 * Only languages with verified public verse audio are listed in [audioEditions].
 */
data class TranslationEdition(
    val id: String,
    val language: String,
    /** ISO-like code for matching text ↔ audio (en, ur, …). */
    val langCode: String,
    val name: String,
    val hasAudio: Boolean = false,
    val everyAyahFolder: String? = null
)

object TranslationEditions {
    val textEditions = listOf(
        TranslationEdition("en.sahih", "English", "en", "Sahih International"),
        TranslationEdition("en.pickthall", "English", "en", "Pickthall"),
        TranslationEdition("en.yusufali", "English", "en", "Yusuf Ali"),
        TranslationEdition("en.asad", "English", "en", "Muhammad Asad"),
        TranslationEdition("ur.jalandhry", "Urdu", "ur", "Fateh Muhammad Jalandhry"),
        TranslationEdition("ur.ahmedali", "Urdu", "ur", "Ahmed Ali"),
        TranslationEdition("ur.jawadi", "Urdu", "ur", "Syed Zeeshan Haider Jawadi"),
        TranslationEdition("hi.hindi", "Hindi", "hi", "Suhel Farooq Khan & Naik"),
        TranslationEdition("bn.bengali", "Bengali", "bn", "Muhiuddin Khan"),
        TranslationEdition("id.indonesian", "Indonesian", "id", "Indonesian Ministry"),
        TranslationEdition("tr.diyanet", "Turkish", "tr", "Diyanet İşleri"),
        TranslationEdition("fr.hamidullah", "French", "fr", "Muhammad Hamidullah"),
        TranslationEdition("de.aburida", "German", "de", "Abu Rida"),
        TranslationEdition("es.cortes", "Spanish", "es", "Julio Cortes"),
        TranslationEdition("ru.kuliev", "Russian", "ru", "Elmir Kuliev"),
        TranslationEdition("bs.mlivo", "Bosnian", "bs", "Besim Korkut"),
        TranslationEdition("nl.siregar", "Dutch", "nl", "Sofian S. Siregar"),
        TranslationEdition("pt.elhayek", "Portuguese", "pt", "Samir El-Hayek"),
        TranslationEdition("fa.makarem", "Persian", "fa", "Naser Makarem Shirazi"),
        TranslationEdition("zh.jian", "Chinese", "zh", "Ma Jian")
    )

    /** Verified verse-by-verse translation audio on everyayah.com only. */
    val audioEditions = listOf(
        TranslationEdition(
            id = "en.walk",
            language = "English",
            langCode = "en",
            name = "Sahih Intl · Ibrahim Walk",
            hasAudio = true,
            everyAyahFolder = "English/Sahih_Intnl_Ibrahim_Walk_192kbps"
        ),
        TranslationEdition(
            id = "ur.khan",
            language = "Urdu",
            langCode = "ur",
            name = "Shamshad Ali Khan",
            hasAudio = true,
            everyAyahFolder = "translations/urdu_shamshad_ali_khan_46kbps"
        )
    )

    fun textById(id: String): TranslationEdition? = textEditions.find { it.id == id }
    fun audioById(id: String): TranslationEdition? = audioEditions.find { it.id == id }

    fun langCodeOfText(textEditionId: String): String =
        textById(textEditionId)?.langCode
            ?: textEditionId.substringBefore('.').lowercase().ifBlank { "en" }

    /** Audio edition matching the text language, or null if none available. */
    fun audioForTextEdition(textEditionId: String): TranslationEdition? {
        val code = langCodeOfText(textEditionId)
        return audioEditions.find { it.langCode == code }
    }

    fun hasAudioForText(textEditionId: String): Boolean = audioForTextEdition(textEditionId) != null

    fun searchText(query: String): List<TranslationEdition> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return textEditions
        return textEditions.filter {
            it.language.lowercase().contains(q) ||
                it.name.lowercase().contains(q) ||
                it.id.lowercase().contains(q) ||
                it.langCode.contains(q)
        }
    }

    fun everyAyahFileName(surah: Int, ayahInSurah: Int): String =
        "%03d%03d.mp3".format(surah.coerceIn(1, 114), ayahInSurah.coerceAtLeast(1))

    fun everyAyahUrl(folder: String, surah: Int, ayahInSurah: Int): String =
        "https://everyayah.com/data/$folder/${everyAyahFileName(surah, ayahInSurah)}"
}

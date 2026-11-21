package com.suhood.core.quran

/**
 * Text editions: AlQuran.Cloud identifiers only (verified public API).
 * Audio: everyayah.com verse files only where a public folder is confirmed.
 *
 * Roman Hindi / Roman Urdu meaning-based Latin-script translations are NOT
 * available on AlQuran.Cloud; they are listed as unavailable rather than fabricated.
 */
data class TranslationEdition(
    val id: String,
    val language: String,
    val langCode: String,
    val name: String,
    val hasAudio: Boolean = false,
    val everyAyahFolder: String? = null,
    /** When false, do not attempt to load (e.g. no verified source). */
    val available: Boolean = true
)

object TranslationEditions {
    val textEditions = listOf(
        // English
        TranslationEdition("en.sahih", "English", "en", "Sahih International"),
        TranslationEdition("en.pickthall", "English", "en", "Pickthall"),
        TranslationEdition("en.yusufali", "English", "en", "Yusuf Ali"),
        TranslationEdition("en.asad", "English", "en", "Muhammad Asad"),
        // Urdu
        TranslationEdition("ur.jalandhry", "Urdu", "ur", "Fateh Muhammad Jalandhry"),
        TranslationEdition("ur.ahmedali", "Urdu", "ur", "Ahmed Ali"),
        TranslationEdition("ur.jawadi", "Urdu", "ur", "Syed Zeeshan Haider Jawadi"),
        // Hindi (Devanagari)
        TranslationEdition("hi.hindi", "Hindi", "hi", "Suhel Farooq Khan & Saifur Rahman Nadwi"),
        TranslationEdition("hi.farooq", "Hindi", "hi", "Muhammad Farooq Khan"),
        // Bengali
        TranslationEdition("bn.bengali", "Bengali", "bn", "Muhiuddin Khan"),
        TranslationEdition("bn.hoque", "Bengali", "bn", "Zohurul Hoque"),
        // Turkish
        TranslationEdition("tr.diyanet", "Turkish", "tr", "Diyanet İşleri"),
        TranslationEdition("tr.yazir", "Turkish", "tr", "Elmalılı Hamdi Yazır"),
        // French
        TranslationEdition("fr.hamidullah", "French", "fr", "Muhammad Hamidullah"),
        // German
        TranslationEdition("de.aburida", "German", "de", "Abu Rida"),
        TranslationEdition("de.bubenheim", "German", "de", "Bubenheim & Elyas"),
        // Spanish
        TranslationEdition("es.cortes", "Spanish", "es", "Julio Cortes"),
        TranslationEdition("es.garcia", "Spanish", "es", "Muhammad Isa García"),
        // Indonesian / Malay
        TranslationEdition("id.indonesian", "Indonesian", "id", "Indonesian Ministry of Religious Affairs"),
        TranslationEdition("id.jalalayn", "Indonesian", "id", "Jalalayn"),
        TranslationEdition("ms.basmeih", "Malay", "ms", "Abdullah Muhammad Basmeih"),
        // Persian
        TranslationEdition("fa.makarem", "Persian", "fa", "Naser Makarem Shirazi"),
        TranslationEdition("fa.fooladvand", "Persian", "fa", "Mohammad Mahdi Fooladvand"),
        // Russian
        TranslationEdition("ru.kuliev", "Russian", "ru", "Elmir Kuliev"),
        TranslationEdition("ru.osmanov", "Russian", "ru", "Magomed-Nuri Osmanov"),
        // Chinese
        TranslationEdition("zh.jian", "Chinese", "zh", "Ma Jian"),
        TranslationEdition("zh.majian", "Chinese", "zh", "Ma Jian (alt)"),
        // Tamil / Malayalam
        TranslationEdition("ta.tamil", "Tamil", "ta", "Jan Trust Foundation"),
        TranslationEdition("ml.abdulhameed", "Malayalam", "ml", "Abdul Hameed & Kunhi"),
        TranslationEdition("ml.karakunnu", "Malayalam", "ml", "Cheriyamundam Abdul Hameed & Kunhi Mohammed"),
        // Placeholder entries — no verified AlQuran.Cloud Latin-script meaning translation
        TranslationEdition(
            id = "roman.hi.unavailable",
            language = "Roman Hindi",
            langCode = "hi-Latn",
            name = "Not available from verified API sources",
            available = false
        ),
        TranslationEdition(
            id = "roman.ur.unavailable",
            language = "Roman Urdu",
            langCode = "ur-Latn",
            name = "Not available from verified API sources",
            available = false
        )
    )

    val availableTextEditions: List<TranslationEdition>
        get() = textEditions.filter { it.available }

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

    fun audioForTextEdition(textEditionId: String): TranslationEdition? {
        val code = langCodeOfText(textEditionId).substringBefore('-')
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

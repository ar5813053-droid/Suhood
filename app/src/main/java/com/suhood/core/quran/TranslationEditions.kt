package com.suhood.core.quran

/**
 * Verified AlQuran.Cloud text editions only.
 * Each language appears exactly once with one preferred translator.
 *
 * Audio: everyayah.com verse files only where a public folder is confirmed.
 *
 * Attribution: AlQuran.Cloud / Islamic Network — do not claim ownership of texts.
 */
data class TranslationEdition(
    val id: String,
    val language: String,
    val langCode: String,
    val name: String,
    val hasAudio: Boolean = false,
    val everyAyahFolder: String? = null,
    val available: Boolean = true
)

object TranslationEditions {
    /**
     * One verified working edition per language.
     * Verified against AlQuran.Cloud for Surah 1 and Surah 36 (non-empty ayahs, matching verse numbers).
     */
    val textEditions = listOf(
        TranslationEdition("en.sahih", "English", "en", "Sahih International"),
        TranslationEdition("ur.jalandhry", "Urdu", "ur", "Fateh Muhammad Jalandhry"),
        TranslationEdition("hi.hindi", "Hindi", "hi", "Suhel Farooq Khan & Saifur Rahman Nadwi"),
        TranslationEdition("bn.bengali", "Bengali", "bn", "Muhiuddin Khan"),
        TranslationEdition("tr.diyanet", "Turkish", "tr", "Diyanet İşleri"),
        TranslationEdition("fr.hamidullah", "French", "fr", "Muhammad Hamidullah"),
        TranslationEdition("de.bubenheim", "German", "de", "Bubenheim & Elyas"),
        TranslationEdition("es.garcia", "Spanish", "es", "Muhammad Isa García"),
        TranslationEdition("id.indonesian", "Indonesian", "id", "Indonesian Ministry of Religious Affairs"),
        TranslationEdition("ms.basmeih", "Malay", "ms", "Abdullah Muhammad Basmeih"),
        TranslationEdition("fa.makarem", "Persian", "fa", "Naser Makarem Shirazi"),
        TranslationEdition("ru.kuliev", "Russian", "ru", "Elmir Kuliev"),
        TranslationEdition("zh.jian", "Chinese", "zh", "Ma Jian"),
        TranslationEdition("ta.tamil", "Tamil", "ta", "Jan Trust Foundation"),
        TranslationEdition("ml.abdulhameed", "Malayalam", "ml", "Abdul Hameed & Kunhi")
    )

    val availableTextEditions: List<TranslationEdition>
        get() = textEditions.filter { it.available }

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
        if (q.isEmpty()) return availableTextEditions
        return availableTextEditions.filter {
            it.language.lowercase().contains(q) ||
                it.name.lowercase().contains(q)
        }
    }

    fun preferredLanguages(): List<LanguageTranslationOption> =
        availableTextEditions.map {
            LanguageTranslationOption(
                language = it.language,
                translatorName = it.name,
                edition = it
            )
        }

    fun searchLanguages(query: String): List<LanguageTranslationOption> {
        val q = query.trim().lowercase()
        val all = preferredLanguages()
        if (q.isEmpty()) return all
        return all.filter {
            it.language.lowercase().contains(q) ||
                it.translatorName.lowercase().contains(q)
        }
    }

    fun everyAyahFileName(surah: Int, ayahInSurah: Int): String =
        "%03d%03d.mp3".format(surah.coerceIn(1, 114), ayahInSurah.coerceAtLeast(1))

    fun everyAyahUrl(folder: String, surah: Int, ayahInSurah: Int): String =
        "https://everyayah.com/data/$folder/${everyAyahFileName(surah, ayahInSurah)}"
}

data class LanguageTranslationOption(
    val language: String,
    val translatorName: String,
    val edition: TranslationEdition
) {
    val displayLabel: String
        get() = if (translatorName.isBlank()) language else "$language — $translatorName"
}

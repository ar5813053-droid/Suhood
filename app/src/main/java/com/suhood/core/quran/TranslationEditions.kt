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

    /**
     * Preferred single edition id per language (verified AlQuran.Cloud sources only).
     * Architecture stays flexible — change an id here to switch the default later.
     */
    private val preferredIdsInOrder = listOf(
        "en.sahih",        // English
        "ur.jalandhry",    // Urdu
        "hi.hindi",        // Hindi
        "bn.bengali",      // Bengali
        "tr.diyanet",      // Turkish
        "fr.hamidullah",   // French
        "de.bubenheim",    // German
        "es.garcia",       // Spanish
        "id.indonesian",   // Indonesian
        "ms.basmeih",      // Malay
        "fa.makarem",      // Persian
        "ru.kuliev",       // Russian
        "zh.jian",         // Chinese
        "ta.tamil",        // Tamil
        "ml.abdulhameed"   // Malayalam
    )

    private val nativeNames = mapOf(
        "en" to "English",
        "ur" to "اردو",
        "hi" to "हिन्दी",
        "bn" to "বাংলা",
        "tr" to "Türkçe",
        "fr" to "Français",
        "de" to "Deutsch",
        "es" to "Español",
        "id" to "Bahasa Indonesia",
        "ms" to "Bahasa Melayu",
        "fa" to "فارسی",
        "ru" to "Русский",
        "zh" to "中文",
        "ta" to "தமிழ்",
        "ml" to "മലയാളം"
    )

    private val popularLangCodes = listOf("en", "ur", "hi", "bn", "tr", "fr", "es", "id")

    /**
     * One preferred, verified translation per language — never multiple for the same language.
     */
    fun preferredLanguages(): List<LanguageTranslationOption> {
        val byLang = linkedMapOf<String, LanguageTranslationOption>()
        for (id in preferredIdsInOrder) {
            val ed = textById(id) ?: continue
            if (!ed.available) continue
            val key = ed.langCode.lowercase()
            if (key in byLang) continue
            byLang[key] = LanguageTranslationOption(
                language = ed.language,
                nativeName = nativeNames[key] ?: ed.language,
                translatorName = ed.name,
                edition = ed
            )
        }
        // Any other available languages not in preferred list (first edition wins)
        for (ed in availableTextEditions) {
            val key = ed.langCode.lowercase()
            if (key in byLang) continue
            if (key.startsWith("hi-") || key.startsWith("ur-")) continue // roman placeholders
            byLang[key] = LanguageTranslationOption(
                language = ed.language,
                nativeName = nativeNames[key] ?: ed.language,
                translatorName = ed.name,
                edition = ed
            )
        }
        return byLang.values.toList()
    }

    fun popularLanguages(): List<LanguageTranslationOption> {
        val all = preferredLanguages().associateBy { it.edition.langCode.lowercase() }
        return popularLangCodes.mapNotNull { all[it] }
    }

    fun searchLanguages(query: String): List<LanguageTranslationOption> {
        val q = query.trim().lowercase()
        val all = preferredLanguages()
        if (q.isEmpty()) return all
        return all.filter {
            it.language.lowercase().contains(q) ||
                it.nativeName.lowercase().contains(q) ||
                it.translatorName.lowercase().contains(q) ||
                it.edition.langCode.contains(q)
        }
    }

    fun optionForEditionId(id: String): LanguageTranslationOption? =
        preferredLanguages().find { it.edition.id == id }
            ?: textById(id)?.takeIf { it.available }?.let {
                LanguageTranslationOption(
                    language = it.language,
                    nativeName = nativeNames[it.langCode.lowercase()] ?: it.language,
                    translatorName = it.name,
                    edition = it
                )
            }

    fun everyAyahFileName(surah: Int, ayahInSurah: Int): String =
        "%03d%03d.mp3".format(surah.coerceIn(1, 114), ayahInSurah.coerceAtLeast(1))

    fun everyAyahUrl(folder: String, surah: Int, ayahInSurah: Int): String =
        "https://everyayah.com/data/$folder/${everyAyahFileName(surah, ayahInSurah)}"
}

/**
 * User-facing row: one language → one preferred translation.
 * Never exposes raw API identifiers.
 */
data class LanguageTranslationOption(
    val language: String,
    val nativeName: String,
    val translatorName: String,
    val edition: TranslationEdition
) {
    /** Clean primary label, e.g. "English" or "اردو". */
    val title: String
        get() = if (nativeName.isNotBlank() && nativeName != language) nativeName else language

    /** Secondary line, e.g. "Sahih International". */
    val subtitle: String
        get() = when {
            translatorName.isBlank() -> language
            nativeName != language && nativeName.isNotBlank() -> "$language · $translatorName"
            else -> translatorName
        }
}

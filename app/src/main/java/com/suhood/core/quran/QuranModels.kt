package com.suhood.core.quran

data class SurahMeta(
    val number: Int,
    val nameArabic: String,
    val nameEnglish: String,
    val nameTranslated: String,
    val ayahCount: Int,
    val revelationType: String
)

data class AyahText(
    val globalNumber: Int,
    val numberInSurah: Int,
    val arabic: String,
    val translation: String = ""
)

data class SurahContent(
    val meta: SurahMeta,
    val ayahs: List<AyahText>
)

data class QariInfo(
    val id: String,
    val name: String,
    val englishName: String
)

/** Verified reciters available as verse-by-verse audio on alquran.cloud */
object VerifiedQaris {
    val list = listOf(
        QariInfo("ar.alafasy", "مشاري العفاسي", "Mishary Rashid Alafasy"),
        QariInfo("ar.abdurrahmaansudais", "عبدالرحمن السديس", "Abdurrahmaan As-Sudais"),
        QariInfo("ar.mahermuaiqly", "ماهر المعيقلي", "Maher Al-Muaiqly"),
        QariInfo("ar.abdulsamad", "عبدالباسط عبدالصمد", "Abdul Basit Abdus Samad"),
        QariInfo("ar.minshawi", "محمد صديق المنشاوي", "Muhammad Siddiq Al-Minshawi"),
        QariInfo("ar.husary", "محمود خليل الحصري", "Mahmoud Khalil Al-Husary"),
        QariInfo("ar.shaatree", "أبو بكر الشاطري", "Abu Bakr Ash-Shaatree")
    )
}


data class PageAyah(
    val globalNumber: Int,
    val numberInSurah: Int,
    val surahNumber: Int,
    val surahName: String,
    val arabic: String
)

data class PageContent(
    val pageNumber: Int,
    val ayahs: List<PageAyah>
)


/**
 * Single canonical identity for a Quran verse used by text, Arabic audio,
 * and translation audio. Never derive translation identity from list index alone.
 */
data class CanonicalAyahId(
    val surah: Int,
    val ayahInSurah: Int,
    val globalNumber: Int
) {
    init {
        require(surah in 1..114) { "Invalid surah: $surah" }
        require(ayahInSurah >= 1) { "Invalid ayahInSurah: $ayahInSurah" }
        require(globalNumber >= 1) { "Invalid globalNumber: $globalNumber" }
    }

    fun arabicMediaId(): String = "quran:s${surah}a${ayahInSurah}:g$globalNumber"
    fun translationMediaId(): String = "quran-tr:s${surah}a${ayahInSurah}:g$globalNumber"

    companion object {
        private val ID_REGEX = Regex("""^quran(?:-tr)?:s(\d+)a(\d+):g(\d+)$""")
        /** Legacy media ids: "quran:123" or "quran-tr:123" (global only). */
        private val LEGACY_REGEX = Regex("""^quran(?:-tr)?:(\d+)$""")

        fun parseMediaId(mediaId: String): CanonicalAyahId? {
            ID_REGEX.matchEntire(mediaId)?.let { m ->
                return runCatching {
                    CanonicalAyahId(
                        surah = m.groupValues[1].toInt(),
                        ayahInSurah = m.groupValues[2].toInt(),
                        globalNumber = m.groupValues[3].toInt()
                    )
                }.getOrNull()
            }
            return null
        }

        /** Global ayah number from new or legacy media ids. */
        fun globalNumberFromMediaId(mediaId: String): Int? {
            parseMediaId(mediaId)?.let { return it.globalNumber }
            LEGACY_REGEX.matchEntire(mediaId)?.let { m ->
                return m.groupValues[1].toIntOrNull()?.takeIf { it >= 1 }
            }
            return null
        }

        fun isTranslationMediaId(mediaId: String): Boolean = mediaId.startsWith("quran-tr:")
    }
}


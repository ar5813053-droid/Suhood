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

package com.suhood.core.quran

/** Standard 30 Juz start points (surah number, ayah in surah). */
object JuzIndex {
    data class Juz(val number: Int, val startSurah: Int, val startAyah: Int, val labelAr: String)

    val list: List<Juz> = listOf(
        Juz(1, 1, 1, "الم"),
        Juz(2, 2, 142, "سيقول"),
        Juz(3, 2, 253, "تلك الرسل"),
        Juz(4, 3, 93, "لن تنالوا"),
        Juz(5, 4, 24, "والمحصنات"),
        Juz(6, 4, 148, "لا يحب الله"),
        Juz(7, 5, 82, "وإذا سمعوا"),
        Juz(8, 6, 111, "ولو أننا"),
        Juz(9, 7, 88, "قال الملأ"),
        Juz(10, 8, 41, "واعلموا"),
        Juz(11, 9, 93, "يعتذرون"),
        Juz(12, 11, 6, "وما من دابة"),
        Juz(13, 12, 53, "وما أبرئ"),
        Juz(14, 15, 1, "ربما"),
        Juz(15, 17, 1, "سبحان"),
        Juz(16, 18, 75, "قال ألم"),
        Juz(17, 21, 1, "اقترب"),
        Juz(18, 23, 1, "قد أفلح"),
        Juz(19, 25, 21, "وقال الذين"),
        Juz(20, 27, 56, "فما كان"),
        Juz(21, 29, 46, "ولا تجادلوا"),
        Juz(22, 33, 31, "ومن يقنت"),
        Juz(23, 36, 28, "وما أنزلنا"),
        Juz(24, 39, 32, "فمن أظلم"),
        Juz(25, 41, 47, "إليه يرد"),
        Juz(26, 46, 1, "حم"),
        Juz(27, 51, 31, "قال فما"),
        Juz(28, 58, 1, "قد سمع"),
        Juz(29, 67, 1, "تبارك"),
        Juz(30, 78, 1, "عم")
    )

    /** Surah numbers that appear in a juz (from this juz start until next). */
    fun surahNumbersInJuz(juzNumber: Int): List<Int> {
        val juz = list.find { it.number == juzNumber } ?: return emptyList()
        val next = list.find { it.number == juzNumber + 1 }
        val endSurah = next?.startSurah ?: 114
        // If next starts mid-surah, include that surah
        val last = if (next != null && next.startAyah > 1) next.startSurah else endSurah - 1
        return (juz.startSurah..last.coerceAtLeast(juz.startSurah)).toList()
    }
}

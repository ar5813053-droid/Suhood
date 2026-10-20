package com.suhood.core.dua

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.suhood.core.model.DuaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Loads Hisn al-Muslim style adhkar online (Arabic + source).
 * Source: public JSON — https://github.com/asellam/HisnElMuslim (MIT).
 * Falls back to caller-provided offline list when offline or on error.
 */
object OnlineDuaRepository {

    
    private val categoryEn = mapOf(
        "أَذْكَارُ الِاسْتِيقَاظِ مِنَ النَّوْمِ" to "Upon waking up",
        "دعاء لبس الثوب" to "When wearing clothes",
        "دعاء لبس الثوب الجديد" to "When wearing new clothes",
        "الدعاء لمن لبس ثوبا جديدا" to "For someone wearing new clothes",
        "ما يقول إذا وضع ثوبه" to "When removing clothes",
        "دعاء دخول الخلاء" to "Entering the toilet",
        "دعاء الخروج من الخلاء" to "Leaving the toilet",
        "الذكر قبل الوضوء" to "Before wudu",
        "الذكر بعد الفراغ من الوضوء" to "After wudu",
        "الذكر عند الخروج من المنزل" to "Leaving home",
        "الذكر عند دخول المنزل" to "Entering home",
        "دعاء الذهاب إلى المسجد" to "Going to the mosque",
        "دعاء دخول المسجد" to "Entering the mosque",
        "دعاء الخروج من المسجد" to "Leaving the mosque",
        "أذكار الأذان" to "During the adhan",
        "دعاء الاستفتاح" to "Opening dua of salah",
        "دعاء الركوع" to "In ruku",
        "دعاء الرفع من الركوع" to "Rising from ruku",
        "دعاء السجود" to "In sujood",
        "دعاء الجلسة بين السجدتين" to "Between the two sajdahs",
        "دعاء سجود التلاوة" to "Sujood of tilawah",
        "التشهد" to "Tashahhud",
        "الصلاة على النبي بعد التشهد" to "Salawat after tashahhud",
        "الدعاء بعد التشهد الأخير قبل السلام" to "After final tashahhud",
        "الأذكار بعد السلام من الصلاة" to "After salah",
        "دعاء الاستخارة" to "Istikhara",
        "أذكار الصباح والمساء" to "Morning and evening adhkar",
        "أذكار النوم" to "Before sleep",
        "الدعاء إذا تقلب ليلا" to "When turning at night",
        "دعاء الفزع في النوم" to "When frightened in sleep",
        "ما يفعل من رأى في منامه ما يحب" to "Good dream",
        "ما يفعل من رأى في منامه ما يكره" to "Bad dream",
        "دعاء قنوت الوتر" to "Qunut of Witr",
        "الذكر عقب السلام من الوتر" to "After Witr",
        "دعاء الهم والحزن" to "Anxiety and sorrow",
        "دعاء الكرب" to "Distress",
        "دعاء لقاء العدو وذي السلطان" to "Meeting an enemy or authority",
        "دعاء من خاف ظلم السلطان" to "Fear of oppression",
        "الدعاء على العدو" to "Against an enemy",
        "دعاء من أصيب بمصيبة" to "When afflicted",
        "دعاء الدين" to "For debt",
        "دعاء الوسوسة في الصلاة والقراءة" to "Waswas in prayer",
        "دعاء من استصعب عليه أمر" to "When a matter is difficult",
        "ما يقول ويفعل من أذنب ذنبا" to "After committing a sin",
        "دعاء طرد الشيطان ووساوسه" to "Against Shaytan",
        "الدعاء حينما يقع ما يكره" to "When disliked thing happens",
        "دعاء من أصابته مصيبة" to "When struck by calamity",
        "التهنئة بالمولود" to "Congratulating on a newborn",
        "ما يقال عند سماع نهيق الحمار" to "Hearing a donkey",
        "الدعاء للمريض في عيادته" to "Visiting the sick",
        "فضل عيادة المريض" to "Virtue of visiting the sick",
        "دعاء المريض الذي أيس من حياته" to "When death approaches",
        "تلقين المحتضر" to "Prompting the dying",
        "دعاء من أصيب بمصيبة" to "When afflicted by calamity",
        "الدعاء بعد دفن الميت" to "After burial",
        "دعاء زيارة القبور" to "Visiting graves",
        "دعاء الريح" to "When the wind blows",
        "دعاء الرعد" to "When hearing thunder",
        "من أدعية الاستسقاء" to "Rain prayer",
        "الدعاء إذا نزل المطر" to "When rain falls",
        "الذكر بعد نزول المطر" to "After rain",
        "دعاء رؤية الهلال" to "Sighting the new moon",
        "الدعاء بعد الأكل" to "After eating",
        "دعاء الضيف لصاحب الطعام" to "Guest for host",
        "الدعاء لمن سقاه أو أسقاه" to "For one who gives drink",
        "دعاء الإفطار" to "Breaking fast",
        "الدعاء قبل الطعام" to "Before eating",
        "دعاء من أفطر عند أحد" to "Iftar at someone's home",
        "دعاء المسافر للصقيم" to "Traveler for the resident",
        "دعاء المقيم للمسافر" to "Resident for the traveler",
        "التكبير والدعا في المسير" to "During travel",
        "دعاء ركوب الدابة" to "Mounting a vehicle",
        "دعاء السفر" to "Travel dua",
        "دعاء دخول القرية أو البلدة" to "Entering a town",
        "دعاء السوق" to "In the marketplace",
        "إذا تعسرت دابة" to "When a vehicle stalls"
    )

    private fun englishCategory(ar: String): String {
        categoryEn[ar]?.let { return it }
        // fuzzy contains
        for ((k, v) in categoryEn) {
            if (ar.contains(k.take(12)) || k.contains(ar.take(12))) return v
        }
        return "Authentic adhkar"
    }

    private const val HISN_URL =
        "https://raw.githubusercontent.com/asellam/HisnElMuslim/main/hisn.json"

    @Volatile
    private var cached: List<DuaItem> = emptyList()

    fun cachedOrEmpty(): List<DuaItem> = cached

    fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    suspend fun fetchHisnAlMuslim(): Result<List<DuaItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val conn = (URL(HISN_URL).openConnection() as HttpURLConnection).apply {
                connectTimeout = 12_000
                readTimeout = 20_000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
            }
            conn.inputStream.bufferedReader().use { reader ->
                val root = JSONObject(reader.readText())
                val out = mutableListOf<DuaItem>()
                var index = 0
                val keys = root.keys()
                while (keys.hasNext()) {
                    val categoryAr = keys.next()
                    val chapter = root.optJSONObject(categoryAr) ?: continue
                    val adhkar = chapter.optJSONArray("Adhkar") ?: continue
                    for (i in 0 until adhkar.length()) {
                        val item = adhkar.optJSONObject(i) ?: continue
                        val text = item.optString("Text").trim()
                        if (text.isBlank()) continue
                        val ref = item.optString("Reference").trim()
                        val count = item.optInt("Count", 1)
                        index++
                        val titleEn = englishCategory(categoryAr)
                        val meaningHint = if (ref.isNotBlank()) "Source: $ref" else "From Hisn al-Muslim"
                        out.add(
                            DuaItem(
                                id = "online-$index",
                                title = titleEn,
                                arabic = text,
                                transliteration = if (count > 1) "Repeat $count times" else "",
                                translation = meaningHint,
                                category = titleEn,
                                keywords = listOf(
                                    categoryAr,
                                    text.take(40),
                                    "hisn",
                                    "adhkar",
                                    "online"
                                ),
                                reference = ref.ifBlank { "Hisn al-Muslim" }
                            )
                        )
                    }
                }
                if (out.isEmpty()) error("Empty online dua catalogue")
                cached = out
                out
            }
        }
    }
}

package com.suhood.widgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.suhood.app.SuhoodApplication
import com.suhood.core.database.HabitCompletionEntity
import com.suhood.core.database.SalahLogEntity
import com.suhood.core.datastore.PreferencesRepository
import com.suhood.core.model.DuaItem
import com.suhood.core.prayer.PrayerTimesCalculator
import com.suhood.core.prayer.SmartNextSalah
import com.suhood.core.prayer.SmartSalahBarState
import com.suhood.feature.salah.SalahStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

object WidgetBridge {

    data class SalahSnap(
        val smart: SmartSalahBarState,
        val marked: Map<String, String>,
        val completedCount: Int,
        val locationLabel: String
    )

    data class QuranSnap(
        val dailyMinutes: Int,
        val targetPages: Int,
        val pagesDone: Int,
        val lastPage: Int,
        val lastSurah: Int?,
        val lastAyah: Int?
    )

    data class HabitSnap(
        val id: String,
        val title: String,
        val target: Int,
        val doneToday: Boolean
    )

    data class AudioSnap(
        val title: String?,
        val artist: String?,
        val isPlaying: Boolean
    )

    fun app(context: Context): SuhoodApplication =
        context.applicationContext as SuhoodApplication

    fun dao(context: Context) = app(context).database.dao()

    suspend fun loadSalah(context: Context): SalahSnap = withContext(Dispatchers.IO) {
        val prefs = PreferencesRepository(context)
        val lat = prefs.latitude.first()
        val lng = prefs.longitude.first()
        val method = prefs.prayerMethod.first()
        val label = prefs.locationLabel.first()
        val cal = Calendar.getInstance()
        val result = PrayerTimesCalculator.calculate(lat, lng, method, cal)
        val smart = SmartNextSalah.resolve(result, cal)
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val logs = dao(context).getSalahForDate(today)
        val marked = logs.associate { it.prayerName to it.status }
        val completed = logs.count { it.status != SalahStatus.NOT_PRAYED.key }
        SalahSnap(smart, marked, completed, label)
    }

    suspend fun loadQuran(context: Context): QuranSnap = withContext(Dispatchers.IO) {
        val prefs = PreferencesRepository(context)
        val minutes = prefs.quranDailyMinutes.first()
        val target = when (minutes) { 5 -> 2; 20 -> 8; else -> 4 }
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date())
        val date = prefs.quranTodayDate.first()
        val pages = if (date == today) prefs.quranTodayPages.first() else 0
        val lastPage = prefs.quranLastPage.first()
        val last = dao(context).getLastRead()
        QuranSnap(minutes, target, pages, lastPage, last?.surah, last?.ayah)
    }

    suspend fun loadHabits(context: Context): List<HabitSnap> = withContext(Dispatchers.IO) {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val habits = dao(context).observeHabits().first().take(5)
        val completions = dao(context).habitCompletionsInRange(today, today).map { it.habitId }.toSet()
        habits.map { HabitSnap(it.id, it.name, it.targetCount, it.id in completions) }
    }

    fun loadAudio(context: Context): AudioSnap = try {
        val s = app(context).playbackController.state.value
        AudioSnap(s.currentTrack?.title, s.currentTrack?.creator, s.isPlaying)
    } catch (_: Exception) {
        AudioSnap(null, null, false)
    }

    fun duaOfDay(): DuaItem {
        val list = VERIFIED_SHORT_DUAS
        val idx = Calendar.getInstance().get(Calendar.DAY_OF_YEAR) % list.size
        return list[idx]
    }

    fun hijriGregorianLabels(): Pair<String, String> {
        val cal = Calendar.getInstance()
        val greg = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.US).format(cal.time)
        return approximateHijri(cal) to greg
    }

    private fun approximateHijri(cal: Calendar): String {
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH) + 1
        val d = cal.get(Calendar.DAY_OF_MONTH)
        var jd = (1461 * (y + 4800 + (m - 14) / 12)) / 4 +
            (367 * (m - 2 - 12 * ((m - 14) / 12))) / 12 -
            (3 * ((y + 4900 + (m - 14) / 12) / 100)) / 4 + d - 32075
        val days = jd - 1948439 - 227014
        val year = ((30 * days + 10646) / 10631)
        val month = minOf(12, maxOf(1, ((days - (29 + ((year - 1) * 10631L) / 30)) / 29.5).toInt() + 1))
        val day = minOf(30, maxOf(1, (days - ((year - 1) * 10631.0) / 30 - (month - 1) * 29.5).toInt() + 1))
        val months = arrayOf(
            "Muharram", "Safar", "Rabiʿ I", "Rabiʿ II", "Jumada I", "Jumada II",
            "Rajab", "Shaʿban", "Ramadan", "Shawwal", "Dhul-Qaʿdah", "Dhul-Hijjah"
        )
        return "$day ${months[month - 1]} $year AH (approx.)"
    }

    fun refreshAll(context: Context) {
        val mgr = AppWidgetManager.getInstance(context)
        listOf(
            NextSalahWidgetReceiver::class.java,
            DuaWidgetReceiver::class.java,
            IslamicDateWidgetReceiver::class.java,
            PrayerTrackerWidgetReceiver::class.java,
            QuranCompanionWidgetReceiver::class.java,
            NasheedifyWidgetReceiver::class.java,
            HabitWidgetReceiver::class.java,
            MushafWidgetReceiver::class.java,
            PrayerMarkerWidgetReceiver::class.java
        ).forEach { cls ->
            val ids = mgr.getAppWidgetIds(ComponentName(context, cls))
            if (ids.isNotEmpty()) {
                context.sendBroadcast(
                    Intent(context, cls).apply {
                        action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                    }
                )
            }
        }
    }

    suspend fun markPrayer(context: Context, prayer: String, statusKey: String) {
        withContext(Dispatchers.IO) {
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            dao(context).upsertSalahLog(
                SalahLogEntity(
                    id = "$today-$prayer",
                    dateIso = today,
                    prayerName = prayer,
                    status = statusKey
                )
            )
        }
        refreshAll(context)
    }

    suspend fun toggleHabit(context: Context, habitId: String) {
        withContext(Dispatchers.IO) {
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val id = "$habitId|$today"
            val existing = dao(context).habitCompletionsInRange(today, today).any { it.habitId == habitId }
            if (existing) dao(context).deleteHabitCompletion(id)
            else dao(context).upsertHabitCompletion(
                HabitCompletionEntity(id = id, habitId = habitId, dateIso = today, count = 1)
            )
        }
        refreshAll(context)
    }

    private val VERIFIED_SHORT_DUAS = listOf(
        DuaItem(
            id = "w1",
            title = "Opening of prayer",
            arabic = "سُبْحَانَكَ اللَّهُمَّ وَبِحَمْدِكَ وَتَبَارَكَ اسْمُكَ وَتَعَالَى جَدُّكَ وَلَا إِلَٰهَ غَيْرُكَ",
            transliteration = "Subhanaka Allahumma wa bihamdika...",
            translation = "Glory is to You, O Allah, and praise; blessed is Your Name; exalted is Your majesty; there is no god but You.",
            category = "Prayer",
            keywords = listOf("salah", "prayer"),
            reference = "Abu Dawud, Tirmidhi"
        ),
        DuaItem(
            id = "w2",
            title = "Seeking guidance",
            arabic = "رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ",
            transliteration = "Rabbana atina fid-dunya hasanah...",
            translation = "Our Lord, give us good in this world and the Hereafter, and protect us from the Fire.",
            category = "Quran",
            keywords = listOf("quran"),
            reference = "Qur'an 2:201"
        ),
        DuaItem(
            id = "w3",
            title = "Before sleep",
            arabic = "بِاسْمِكَ اللَّهُمَّ أَمُوتُ وَأَحْيَا",
            transliteration = "Bismika Allahumma amutu wa ahya",
            translation = "In Your name, O Allah, I die and I live.",
            category = "Evening",
            keywords = listOf("sleep"),
            reference = "Bukhari"
        ),
        DuaItem(
            id = "w4",
            title = "Leaving home",
            arabic = "بِسْمِ اللَّهِ تَوَكَّلْتُ عَلَى اللَّهِ وَلَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ",
            transliteration = "Bismillahi tawakkaltu 'ala Allah...",
            translation = "In the name of Allah; I place my trust in Allah; there is no power except with Allah.",
            category = "Daily",
            keywords = listOf("home"),
            reference = "Abu Dawud, Tirmidhi"
        ),
        DuaItem(
            id = "w5",
            title = "After eating",
            arabic = "الْحَمْدُ لِلَّهِ الَّذِي أَطْعَمَنِي هَٰذَا وَرَزَقَنِيهِ مِنْ غَيْرِ حَوْلٍ مِنِّي وَلَا قُوَّةٍ",
            transliteration = "Alhamdu lillahil-ladhi at'amani hadha...",
            translation = "Praise be to Allah who fed me this and provided it without any power from me.",
            category = "Daily",
            keywords = listOf("food"),
            reference = "Abu Dawud, Tirmidhi"
        )
    )
}

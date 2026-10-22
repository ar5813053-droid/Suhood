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
import com.suhood.core.prayer.PrayerTimesCache
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

    fun app(context: Context): SuhoodApplication {
        val appCtx = context.applicationContext
        return appCtx as? SuhoodApplication
            ?: error("SuhoodApplication not ready (got ${appCtx.javaClass.name})")
    }

    fun dao(context: Context) = app(context).database.dao()

    
    suspend fun loadSalah(context: Context): SalahSnap = withContext(Dispatchers.IO) {
        loadSalahSync(context)
    }

    /** Safe for AppWidgetProvider.onUpdate (main thread) — no DataStore. */
    fun loadSalahSync(context: Context): SalahSnap {
        val (result, label) = PrayerTimesCache.loadOrCalculate(context)
        val cal = Calendar.getInstance()
        // Recompute for *today* if cache is stale or always refresh calculate with cached lat/lng
        val refreshed = runCatching {
            PrayerTimesCalculator.calculate(
                latitude = result.latitude,
                longitude = result.longitude,
                method = result.method,
                calendar = cal
            )
        }.getOrDefault(result)
        val smart = SmartNextSalah.resolve(refreshed, cal)
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val logs = runCatching { kotlinx.coroutines.runBlocking(Dispatchers.IO) {
            dao(context).getSalahForDate(today)
        } }.getOrElse { emptyList() }
        val marked = logs.associate { it.prayerName to it.status }
        val completed = logs.count { it.status != SalahStatus.NOT_PRAYED.key }
        return SalahSnap(smart, marked, completed, label)
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

    data class DateLabels(
        val gregorianDay: Int,
        val gregorianMonthEn: String,
        val gregorianFull: String,
        val weekdayAr: String,
        val weekdayEn: String,
        val hijriDay: Int,
        val hijriMonthAr: String,
        val hijriMonthEn: String,
        val hijriYear: Int,
        val hijriLineAr: String,
        val hijriLineEn: String
    )

    fun dateLabels(): DateLabels {
        val cal = Calendar.getInstance()
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val monthEn = SimpleDateFormat("MMMM", Locale.US).format(cal.time)
        val full = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.US).format(cal.time)
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        val weekdayAr = arrayOf(
            "", "الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت"
        )[dow]
        val weekdayEn = SimpleDateFormat("EEEE", Locale.US).format(cal.time)
        val (hDay, hMonth, hYear) = approximateHijriParts(cal)
        val monthsAr = arrayOf(
            "محرم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى", "جمادى الآخرة",
            "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
        )
        val monthsEn = arrayOf(
            "Muharram", "Safar", "Rabiʿ I", "Rabiʿ II", "Jumada I", "Jumada II",
            "Rajab", "Shaʿban", "Ramadan", "Shawwal", "Dhul-Qaʿdah", "Dhul-Hijjah"
        )
        val mi = (hMonth - 1).coerceIn(0, 11)
        return DateLabels(
            gregorianDay = day,
            gregorianMonthEn = monthEn.uppercase(Locale.US),
            gregorianFull = full,
            weekdayAr = weekdayAr,
            weekdayEn = weekdayEn,
            hijriDay = hDay,
            hijriMonthAr = monthsAr[mi],
            hijriMonthEn = monthsEn[mi],
            hijriYear = hYear,
            hijriLineAr = "$hDay ${monthsAr[mi]} $hYear هـ",
            hijriLineEn = "$hDay ${monthsEn[mi]} $hYear AH (approx.)"
        )
    }

    private fun approximateHijriParts(cal: Calendar): Triple<Int, Int, Int> {
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH) + 1
        val d = cal.get(Calendar.DAY_OF_MONTH)
        val jd = (1461 * (y + 4800 + (m - 14) / 12)) / 4 +
            (367 * (m - 2 - 12 * ((m - 14) / 12))) / 12 -
            (3 * ((y + 4900 + (m - 14) / 12) / 100)) / 4 + d - 32075
        val days = jd - 1948439 - 227014
        val year = ((30 * days + 10646) / 10631)
        val month = minOf(12, maxOf(1, ((days - (29 + ((year - 1) * 10631L) / 30)) / 29.5).toInt() + 1))
        val dayH = minOf(30, maxOf(1, (days - ((year - 1) * 10631.0) / 30 - (month - 1) * 29.5).toInt() + 1))
        return Triple(dayH, month, year)
    }

    /** One-shot alarm at next prayer boundary — no constant polling. */
    fun scheduleNextPrayerRefresh(context: Context) {
        runCatching {
            val snap = kotlinx.coroutines.runBlocking { loadSalah(context) }
            val delaySec = when {
                snap.smart.isActiveHighlight -> (10 * 60).coerceAtMost(snap.smart.countdownSeconds.coerceAtLeast(60))
                else -> snap.smart.countdownSeconds.coerceIn(60, 15 * 60)
            }
            val am = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            val pi = android.app.PendingIntent.getBroadcast(
                context, 72001,
                android.content.Intent(context, WidgetRefreshReceiver::class.java),
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            val fireAt = System.currentTimeMillis() + delaySec * 1000L
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    am.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC, fireAt, pi)
                } else {
                    am.setExact(android.app.AlarmManager.RTC, fireAt, pi)
                }
            } catch (_: SecurityException) {
                am.set(android.app.AlarmManager.RTC, fireAt, pi)
            }
        }
    }

    fun refreshAll(context: Context) {
        // Direct RemoteViews updates (reliable) + broadcast for system
        runCatching { updateProvider(context, NextSalahWidgetReceiver::class.java, ::buildNextSalah) }
        runCatching { updateProvider(context, DuaWidgetReceiver::class.java, ::buildDua) }
        runCatching { updateProvider(context, IslamicDateWidgetReceiver::class.java, ::buildIslamicDate) }
        runCatching { updateProvider(context, PrayerTrackerWidgetReceiver::class.java, ::buildPrayerTracker) }
        runCatching { updateProvider(context, QuranCompanionWidgetReceiver::class.java, ::buildQuranCompanion) }
        runCatching { updateProvider(context, NasheedifyWidgetReceiver::class.java, ::buildNasheedify) }
        runCatching { updateProvider(context, HabitWidgetReceiver::class.java, ::buildHabits) }
        runCatching { updateProvider(context, MushafWidgetReceiver::class.java, ::buildMushaf) }
        runCatching { updateProvider(context, PrayerMarkerWidgetReceiver::class.java, ::buildPrayerMarker) }
        runCatching { updateProvider(context, CalendarWidgetReceiver::class.java, ::buildCalendar) }
        runCatching { updateProvider(context, RankWidgetReceiver::class.java, ::buildRank) }
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

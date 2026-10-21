package com.suhood.core.prayer

import android.content.Context
import com.suhood.core.model.PrayerTime
import java.util.Calendar

/**
 * Lightweight SharedPreferences cache so home-screen widgets can read prayer times
 * without touching DataStore (runBlocking + DataStore on the main thread deadlocks).
 */
object PrayerTimesCache {
    private const val PREFS = "suhood_prayer_cache"
    private const val KEY_LAT = "lat"
    private const val KEY_LNG = "lng"
    private const val KEY_METHOD = "method"
    private const val KEY_LABEL = "label"
    private const val KEY_DATE = "date" // yyyy-MM-dd
    private const val KEY_TIMES = "times" // Fajr=05:12;Dhuhr=12:30;...

    fun save(
        context: Context,
        lat: Double,
        lng: Double,
        method: PrayerTimesCalculator.Method,
        label: String,
        result: PrayerTimesCalculator.Result
    ) {
        val cal = Calendar.getInstance()
        val date = "%04d-%02d-%02d".format(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
        val times = result.times.joinToString(";") { "${it.name}=${it.time}=${it.minutesSinceMidnight}" }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putFloat(KEY_LAT, lat.toFloat())
            .putFloat(KEY_LNG, lng.toFloat())
            .putString(KEY_METHOD, method.name)
            .putString(KEY_LABEL, label)
            .putString(KEY_DATE, date)
            .putString(KEY_TIMES, times)
            .apply()
    }

    fun loadResult(context: Context): Pair<PrayerTimesCalculator.Result, String>? {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val timesStr = p.getString(KEY_TIMES, null) ?: return null
        val date = p.getString(KEY_DATE, null) ?: return null
        val cal = Calendar.getInstance()
        val today = "%04d-%02d-%02d".format(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
        // Still usable across day boundary for post-Isha → Fajr until Home refreshes
        val methodName = p.getString(KEY_METHOD, PrayerTimesCalculator.Method.MWL.name)
            ?: PrayerTimesCalculator.Method.MWL.name
        val method = runCatching { PrayerTimesCalculator.Method.valueOf(methodName) }
            .getOrDefault(PrayerTimesCalculator.Method.MWL)
        val lat = p.getFloat(KEY_LAT, 21.4225f).toDouble()
        val lng = p.getFloat(KEY_LNG, 39.8262f).toDouble()
        val label = p.getString(KEY_LABEL, "Saved location") ?: "Saved location"
        val times = timesStr.split(";").mapNotNull { part ->
            val bits = part.split("=")
            if (bits.size < 2) return@mapNotNull null
            val name = bits[0]
            val time = bits[1]
            val mins = bits.getOrNull(2)?.toIntOrNull()
                ?: run {
                    val hm = time.split(":")
                    if (hm.size == 2) hm[0].toInt() * 60 + hm[1].toInt() else 0
                }
            PrayerTime(name = name, time = time, minutesSinceMidnight = mins)
        }
        if (times.size < 5) return null
        val result = PrayerTimesCalculator.Result(
            dateLabel = if (date == today) "Today" else date,
            times = times,
            method = method,
            latitude = lat,
            longitude = lng
        )
        return result to label
    }

    fun loadOrCalculate(context: Context): Pair<PrayerTimesCalculator.Result, String> {
        loadResult(context)?.let { return it }
        // Fallback: Makkah defaults — never block on DataStore
        val lat = 21.4225
        val lng = 39.8262
        val result = PrayerTimesCalculator.calculate(lat, lng, PrayerTimesCalculator.Method.MWL)
        return result to "Makkah (default)"
    }
}

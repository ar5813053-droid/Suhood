package com.suhood.core.prayer

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.prayerSettings by preferencesDataStore("prayer_settings_v1")

enum class PrayerTimeMode { AUTO, MASJID, ADJUSTED }

data class PrayerSettings(
    val mode: PrayerTimeMode = PrayerTimeMode.AUTO,
    val methodName: String = PrayerTimesCalculator.Method.MWL.name,
    val locationLabel: String = "Makkah (fallback)",
    val latitude: Double = 21.4225,
    val longitude: Double = 39.8262,
    /** Prayer-start HH:mm for masjid mode */
    val masjidFajr: String = "05:00",
    val masjidDhuhr: String = "12:30",
    val masjidAsr: String = "15:45",
    val masjidMaghrib: String = "18:10",
    val masjidIsha: String = "19:30",
    /** Optional jamaat HH:mm (not used for Fajr auto alarm start) */
    val jamaatFajr: String = "",
    val jamaatDhuhr: String = "",
    val jamaatAsr: String = "",
    val jamaatMaghrib: String = "",
    val jamaatIsha: String = "",
    /** Minute offsets applied in ADJUSTED mode (and optionally shown in AUTO) */
    val offsetFajr: Int = 0,
    val offsetDhuhr: Int = 0,
    val offsetAsr: Int = 0,
    val offsetMaghrib: Int = 0,
    val offsetIsha: Int = 0,
    val autoFajrEnabled: Boolean = true,
    val fajrLeadMinutes: Int = 15,
    /** Local date yyyy-MM-dd when today's auto Fajr was skipped */
    val fajrSkipDate: String = ""
)

class PrayerSettingsStore(private val context: Context) {
    private val KEY_MODE = stringPreferencesKey("mode")
    private val KEY_METHOD = stringPreferencesKey("method")
    private val KEY_LABEL = stringPreferencesKey("label")
    private val KEY_LAT = stringPreferencesKey("lat")
    private val KEY_LNG = stringPreferencesKey("lng")
    private val KEY_AUTO_FAJR = booleanPreferencesKey("auto_fajr")
    private val KEY_FAJR_LEAD = intPreferencesKey("fajr_lead")
    private val KEY_SKIP = stringPreferencesKey("fajr_skip")

    private fun mKey(name: String) = stringPreferencesKey("masjid_$name")
    private fun jKey(name: String) = stringPreferencesKey("jamaat_$name")
    private fun oKey(name: String) = intPreferencesKey("offset_$name")

    val settings: Flow<PrayerSettings> = context.prayerSettings.data.map { p ->
        PrayerSettings(
            mode = runCatching { PrayerTimeMode.valueOf(p[KEY_MODE] ?: "AUTO") }.getOrDefault(PrayerTimeMode.AUTO),
            methodName = p[KEY_METHOD] ?: PrayerTimesCalculator.Method.MWL.name,
            locationLabel = p[KEY_LABEL] ?: "Makkah (fallback)",
            latitude = p[KEY_LAT]?.toDoubleOrNull() ?: 21.4225,
            longitude = p[KEY_LNG]?.toDoubleOrNull() ?: 39.8262,
            masjidFajr = p[mKey("fajr")] ?: "05:00",
            masjidDhuhr = p[mKey("dhuhr")] ?: "12:30",
            masjidAsr = p[mKey("asr")] ?: "15:45",
            masjidMaghrib = p[mKey("maghrib")] ?: "18:10",
            masjidIsha = p[mKey("isha")] ?: "19:30",
            jamaatFajr = p[jKey("fajr")] ?: "",
            jamaatDhuhr = p[jKey("dhuhr")] ?: "",
            jamaatAsr = p[jKey("asr")] ?: "",
            jamaatMaghrib = p[jKey("maghrib")] ?: "",
            jamaatIsha = p[jKey("isha")] ?: "",
            offsetFajr = p[oKey("fajr")] ?: 0,
            offsetDhuhr = p[oKey("dhuhr")] ?: 0,
            offsetAsr = p[oKey("asr")] ?: 0,
            offsetMaghrib = p[oKey("maghrib")] ?: 0,
            offsetIsha = p[oKey("isha")] ?: 0,
            autoFajrEnabled = p[KEY_AUTO_FAJR] ?: true,
            fajrLeadMinutes = p[KEY_FAJR_LEAD] ?: 15,
            fajrSkipDate = p[KEY_SKIP] ?: ""
        )
    }

    suspend fun current(): PrayerSettings = settings.first()

    suspend fun update(block: (PrayerSettings) -> PrayerSettings) {
        val cur = current()
        val n = block(cur)
        context.prayerSettings.edit { p ->
            p[KEY_MODE] = n.mode.name
            p[KEY_METHOD] = n.methodName
            p[KEY_LABEL] = n.locationLabel
            p[KEY_LAT] = n.latitude.toString()
            p[KEY_LNG] = n.longitude.toString()
            p[mKey("fajr")] = n.masjidFajr
            p[mKey("dhuhr")] = n.masjidDhuhr
            p[mKey("asr")] = n.masjidAsr
            p[mKey("maghrib")] = n.masjidMaghrib
            p[mKey("isha")] = n.masjidIsha
            p[jKey("fajr")] = n.jamaatFajr
            p[jKey("dhuhr")] = n.jamaatDhuhr
            p[jKey("asr")] = n.jamaatAsr
            p[jKey("maghrib")] = n.jamaatMaghrib
            p[jKey("isha")] = n.jamaatIsha
            p[oKey("fajr")] = n.offsetFajr
            p[oKey("dhuhr")] = n.offsetDhuhr
            p[oKey("asr")] = n.offsetAsr
            p[oKey("maghrib")] = n.offsetMaghrib
            p[oKey("isha")] = n.offsetIsha
            p[KEY_AUTO_FAJR] = n.autoFajrEnabled
            p[KEY_FAJR_LEAD] = n.fajrLeadMinutes
            p[KEY_SKIP] = n.fajrSkipDate
        }
    }
}

object EffectivePrayerTimes {
    data class Entry(val name: String, val startHhmm: String, val source: String)

    fun compute(settings: PrayerSettings): List<Entry> {
        return when (settings.mode) {
            PrayerTimeMode.MASJID -> listOf(
                Entry("Fajr", settings.masjidFajr, "Masjid timetable"),
                Entry("Dhuhr", settings.masjidDhuhr, "Masjid timetable"),
                Entry("Asr", settings.masjidAsr, "Masjid timetable"),
                Entry("Maghrib", settings.masjidMaghrib, "Masjid timetable"),
                Entry("Isha", settings.masjidIsha, "Masjid timetable")
            )
            PrayerTimeMode.AUTO, PrayerTimeMode.ADJUSTED -> {
                val method = runCatching {
                    PrayerTimesCalculator.Method.valueOf(settings.methodName)
                }.getOrDefault(PrayerTimesCalculator.Method.MWL)
                val calc = PrayerTimesCalculator.calculate(
                    settings.latitude, settings.longitude, method
                )
                calc.times.map { pt ->
                    val offset = when (pt.name) {
                        "Fajr" -> settings.offsetFajr
                        "Dhuhr" -> settings.offsetDhuhr
                        "Asr" -> settings.offsetAsr
                        "Maghrib" -> settings.offsetMaghrib
                        "Isha" -> settings.offsetIsha
                        else -> 0
                    }
                    val apply = settings.mode == PrayerTimeMode.ADJUSTED || offset != 0
                    val hhmm = if (apply && offset != 0) addMinutes(pt.time, offset) else pt.time
                    val src = if (offset != 0 && settings.mode == PrayerTimeMode.ADJUSTED)
                        "Calculated + ${offset}m" else "Calculated · ${method.name}"
                    Entry(pt.name, hhmm, src)
                }
            }
        }
    }

    fun fajrStart(settings: PrayerSettings): String? =
        compute(settings).find { it.name.equals("Fajr", true) }?.startHhmm

    fun addMinutes(hhmm: String, delta: Int): String {
        val parts = hhmm.split(":")
        if (parts.size != 2) return hhmm
        val h = parts[0].toIntOrNull() ?: return hhmm
        val m = parts[1].toIntOrNull() ?: return hhmm
        var total = h * 60 + m + delta
        total = ((total % (24 * 60)) + (24 * 60)) % (24 * 60)
        return "%02d:%02d".format(total / 60, total % 60)
    }
}

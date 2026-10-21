package com.suhood.feature.salah

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.suhood.core.database.SalahLogEntity
import com.suhood.core.database.SuhoodDatabase
import com.suhood.core.prayer.PrayerTimesCache
import com.suhood.feature.mosque.OverpassMosques
import com.suhood.feature.quest.QuestRules
import com.suhood.feature.quest.QuestStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import kotlin.math.abs

/**
 * Location-assisted prayer logging helpers.
 * Mosque presence never proves a prayer was performed.
 * Never auto-marks a prayer as missed without reliable location evidence.
 */
object AutoMarkPrefs {
    private const val P = "suhood_automark"

    fun consented(ctx: Context) =
        ctx.getSharedPreferences(P, Context.MODE_PRIVATE).getBoolean("consented", false)

    fun setConsented(ctx: Context, v: Boolean) =
        ctx.getSharedPreferences(P, Context.MODE_PRIVATE).edit().putBoolean("consented", v).apply()

    fun enabled(ctx: Context) =
        ctx.getSharedPreferences(P, Context.MODE_PRIVATE).getBoolean("enabled", false)

    fun setEnabled(ctx: Context, v: Boolean) =
        ctx.getSharedPreferences(P, Context.MODE_PRIVATE).edit().putBoolean("enabled", v).apply()

    fun nearestMosqueName(ctx: Context): String? =
        ctx.getSharedPreferences(P, Context.MODE_PRIVATE).getString("mosque_name", null)

    fun nearestMosqueDist(ctx: Context): Float =
        ctx.getSharedPreferences(P, Context.MODE_PRIVATE).getFloat("mosque_dist", -1f)

    fun setNearest(ctx: Context, name: String?, distM: Float) {
        ctx.getSharedPreferences(P, Context.MODE_PRIVATE).edit()
            .putString("mosque_name", name)
            .putFloat("mosque_dist", distM)
            .apply()
    }

    fun presenceTicks(ctx: Context): List<Long> {
        val raw = ctx.getSharedPreferences(P, Context.MODE_PRIVATE).getString("presence", "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split(",").mapNotNull { it.toLongOrNull() }
    }

    fun addPresence(ctx: Context, now: Long = System.currentTimeMillis()) {
        val cut = now - 15 * 60_000L
        val next = (presenceTicks(ctx) + now).filter { it >= cut }.takeLast(40)
        ctx.getSharedPreferences(P, Context.MODE_PRIVATE).edit()
            .putString("presence", next.joinToString(","))
            .apply()
    }

    fun clearPresence(ctx: Context) =
        ctx.getSharedPreferences(P, Context.MODE_PRIVATE).edit().remove("presence").apply()
}

object AutoMarkEngine {
    const val MAX_RADIUS_M = 2000f
    const val STABLE_MINUTES = 5
    const val ON_TIME_WINDOW_MIN = 5
    const val LATE_START_MIN = 10
    const val LATE_END_MIN = 15
    const val MISSED_WINDOW_MIN = 30

    fun hasLocationPermission(ctx: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    fun lastKnownLocation(ctx: Context): Location? {
        if (!hasLocationPermission(ctx)) return null
        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        var best: Location? = null
        for (p in listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)) {
            runCatching {
                val loc = lm.getLastKnownLocation(p) ?: return@runCatching
                if (best == null || loc.time > best!!.time) best = loc
            }
        }
        return best
    }

    suspend fun findEligibleMosque(ctx: Context, lat: Double, lon: Double): Pair<String, Float>? =
        withContext(Dispatchers.IO) {
            runCatching {
                val list = OverpassMosques.nearby(lat, lon, MAX_RADIUS_M.toInt())
                val near = list.minByOrNull { it.distanceMeters } ?: return@runCatching null
                if (near.distanceMeters > MAX_RADIUS_M) return@runCatching null
                near.name to near.distanceMeters.toFloat()
            }.getOrNull()
        }

    /**
     * If conditions are met, records one prayer and awards XP once.
     * Returns "Prayer:status" or null.
     */
    suspend fun tryAutoMark(ctx: Context): String? {
        if (!AutoMarkPrefs.consented(ctx) || !AutoMarkPrefs.enabled(ctx)) return null
        if (!hasLocationPermission(ctx)) return null
        val loc = lastKnownLocation(ctx) ?: return null
        if (loc.hasAccuracy() && loc.accuracy > 150f) return null

        val mosque = findEligibleMosque(ctx, loc.latitude, loc.longitude)
        if (mosque == null) {
            AutoMarkPrefs.setNearest(ctx, null, -1f)
            AutoMarkPrefs.clearPresence(ctx)
            return null
        }
        val (name, dist) = mosque
        AutoMarkPrefs.setNearest(ctx, name, dist)
        if (dist > MAX_RADIUS_M) return null

        AutoMarkPrefs.addPresence(ctx)
        val ticks = AutoMarkPrefs.presenceTicks(ctx)
        if (ticks.size < 2) return null
        val spanMin = (ticks.last() - ticks.first()) / 60_000f
        if (spanMin < STABLE_MINUTES) return null

        val (result, _) = PrayerTimesCache.loadOrCalculate(ctx)
        val cal = Calendar.getInstance()
        val nowMin = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val dao = SuhoodDatabase.get(ctx).dao()

        for (pt in result.times) {
            val start = if (pt.minutesSinceMidnight > 0) {
                pt.minutesSinceMidnight
            } else {
                val parts = pt.time.split(":")
                if (parts.size != 2) continue
                parts[0].toInt() * 60 + parts[1].toInt()
            }
            val delta = nowMin - start
            if (delta < 0 || delta > MISSED_WINDOW_MIN) continue

            val existing = dao.getSalahForDate(today).firstOrNull { it.prayerName == pt.name }
            if (existing != null && existing.status != SalahStatus.NOT_PRAYED.key) continue

            val status = when {
                delta <= ON_TIME_WINDOW_MIN -> SalahStatus.ON_TIME
                delta in LATE_START_MIN..LATE_END_MIN -> SalahStatus.LATE
                delta in (ON_TIME_WINDOW_MIN + 1) until LATE_START_MIN -> SalahStatus.LATE
                else -> null // do not auto-mark missed
            } ?: continue

            val id = "$today-${pt.name}"
            dao.upsertSalahLog(
                SalahLogEntity(
                    id = id,
                    dateIso = today,
                    prayerName = pt.name,
                    status = status.key
                )
            )
            QuestStore(ctx).tryAward(
                eventId = "salah-$id",
                reason = "Salah ${pt.name} (${status.label}, location-assisted)",
                amount = QuestRules.XP_SALAH
            )
            return "${pt.name}:${status.key}"
        }
        return null
    }
}

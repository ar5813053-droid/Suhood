package com.suhood.core.prayer

// PrayerName not used
import com.suhood.core.model.PrayerTime
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

/**
 * Offline Islamic prayer times using standard solar position formulas.
 *
 * Default method: Muslim World League (Fajr 18°, Isha 17°).
 * Times are calculated for the device timezone unless [timeZoneId] is set.
 *
 * These are **calculated** times, not verified mosque schedules.
 * Users should confirm with their local mosque when precision matters.
 *
 * Algorithm reference: common astronomical prayer-time formulation
 * (similar to widely documented methods used by open-source prayer apps).
 */
object PrayerTimesCalculator {

    enum class Method(
        val fajrAngle: Double,
        val ishaAngle: Double,
        val label: String
    ) {
        MWL(18.0, 17.0, "Muslim World League"),
        ISNA(15.0, 15.0, "ISNA"),
        EGYPT(19.5, 17.5, "Egyptian General Authority"),
        UMM_AL_QURA(18.5, 0.0, "Umm al-Qura (Isha = Maghrib + 90m)")
    }

    data class Result(
        val dateLabel: String,
        val times: List<PrayerTime>,
        val method: Method,
        val latitude: Double,
        val longitude: Double,
        val isCalculated: Boolean = true
    )

    fun calculate(
        latitude: Double,
        longitude: Double,
        method: Method = Method.MWL,
        calendar: Calendar = Calendar.getInstance(),
        timeZoneId: String? = null
    ): Result {
        val tz = timeZoneId?.let { TimeZone.getTimeZone(it) } ?: TimeZone.getDefault()
        val cal = calendar.clone() as Calendar
        cal.timeZone = tz

        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)

        val julian = julianDate(year, month, day)
        val tzOffsetHours = tz.getOffset(cal.timeInMillis) / 3_600_000.0

        // Solar coordinates
        val d = julian - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val L = fixAngle(q + 1.915 * sin(rad(g)) + 0.020 * sin(rad(2 * g)))
        val e = 23.439 - 0.00000036 * d
        val ra = deg(atan(cos(rad(e)) * tan(rad(L)))) / 15.0
        val eqt = q / 15.0 - fixHour(ra)
        val decl = deg(asin(sin(rad(e)) * sin(rad(L))))

        fun timeFor(angle: Double, afterNoon: Boolean): Double {
            val noon = fixHour(12 + tzOffsetHours - longitude / 15.0 - eqt)
            val t = (1.0 / 15.0) * deg(
                acos(
                    (-sin(rad(angle)) - sin(rad(decl)) * sin(rad(latitude))) /
                        (cos(rad(decl)) * cos(rad(latitude)))
                )
            )
            return if (afterNoon) noon + t else noon - t
        }

        val noon = fixHour(12 + tzOffsetHours - longitude / 15.0 - eqt)
        val fajr = timeFor(method.fajrAngle, afterNoon = false)
        val sunrise = timeFor(0.833, afterNoon = false)
        val dhuhr = noon
        val asr = asrTime(latitude, decl, noon, shadowFactor = 1)
        val maghrib = timeFor(0.833, afterNoon = true)
        val isha = if (method == Method.UMM_AL_QURA) {
            maghrib + 1.5
        } else {
            timeFor(method.ishaAngle, afterNoon = true)
        }

        val names = listOf(
            "Fajr" to fajr,
            "Dhuhr" to dhuhr,
            "Asr" to asr,
            "Maghrib" to maghrib,
            "Isha" to isha
        )

        val nowMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val formatted = names.map { (name, t) ->
            val mins = (fixHour(t) * 60).toInt()
            PrayerTime(
                name = name,
                time = formatHm(mins),
                isNext = false,
                isPassed = mins < nowMinutes
            )
        }

        // Mark next prayer
        val nextIdx = formatted.indexOfFirst { !it.isPassed }.let { if (it < 0) 0 else it }
        val times = formatted.mapIndexed { i, p -> p.copy(isNext = i == nextIdx) }

        val dateLabel = String.format(
            Locale.US,
            "%1\$tA, %1\$td %1\$tb %1\$tY",
            cal
        )

        return Result(
            dateLabel = dateLabel,
            times = times,
            method = method,
            latitude = latitude,
            longitude = longitude
        )
    }

    fun countdownSecondsToNext(result: Result, now: Calendar = Calendar.getInstance()): Int {
        val next = result.times.firstOrNull { it.isNext } ?: return 0
        val parts = next.time.split(":")
        if (parts.size != 2) return 0
        val target = now.clone() as Calendar
        target.set(Calendar.HOUR_OF_DAY, parts[0].toInt())
        target.set(Calendar.MINUTE, parts[1].toInt())
        target.set(Calendar.SECOND, 0)
        if (target.before(now)) target.add(Calendar.DAY_OF_MONTH, 1)
        return ((target.timeInMillis - now.timeInMillis) / 1000).toInt().coerceAtLeast(0)
    }

    private fun asrTime(lat: Double, decl: Double, noon: Double, shadowFactor: Int): Double {
        val a = -deg(atan(1.0 / (shadowFactor + tan(rad(abs(lat - decl))))))
        val t = (1.0 / 15.0) * deg(
            acos(
                (-sin(rad(a)) - sin(rad(decl)) * sin(rad(lat))) /
                    (cos(rad(decl)) * cos(rad(lat)))
            )
        )
        return noon + t
    }

    private fun julianDate(y: Int, m: Int, d: Int): Double {
        var year = y
        var month = m
        if (month <= 2) {
            year -= 1
            month += 12
        }
        val A = floor(year / 100.0)
        val B = 2 - A + floor(A / 4.0)
        return floor(365.25 * (year + 4716)) + floor(30.6001 * (month + 1)) + d + B - 1524.5
    }

    private fun rad(d: Double) = d * PI / 180.0
    private fun deg(r: Double) = r * 180.0 / PI
    private fun fixAngle(a: Double): Double {
        var x = a % 360.0
        if (x < 0) x += 360.0
        return x
    }
    private fun fixHour(h: Double): Double {
        var x = h % 24.0
        if (x < 0) x += 24.0
        return x
    }
    private fun formatHm(totalMinutes: Int): String {
        val m = ((totalMinutes % (24 * 60)) + (24 * 60)) % (24 * 60)
        return "%02d:%02d".format(m / 60, m % 60)
    }
}

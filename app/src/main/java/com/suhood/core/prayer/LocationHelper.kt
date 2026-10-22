package com.suhood.core.prayer

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.suhood.core.datastore.PreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

data class DeviceLocation(
    val latitude: Double,
    val longitude: Double,
    val label: String
)

object LocationHelper {

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    fun lastKnownLocation(context: Context): Location? {
        if (!hasLocationPermission(context)) return null
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )
        var best: Location? = null
        for (p in providers) {
            runCatching {
                val loc = lm.getLastKnownLocation(p) ?: return@runCatching
                if (best == null || loc.accuracy < best!!.accuracy) best = loc
            }
        }
        return best
    }

    suspend fun resolveLabel(context: Context, lat: Double, lng: Double): String =
        withContext(Dispatchers.IO) {
            runCatching {
                if (!Geocoder.isPresent()) return@runCatching "Lat %.2f, Lng %.2f".format(lat, lng)
                val geo = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val list = geo.getFromLocation(lat, lng, 1)
                val a = list?.firstOrNull()
                when {
                    a == null -> "Lat %.2f, Lng %.2f".format(lat, lng)
                    !a.locality.isNullOrBlank() && !a.countryName.isNullOrBlank() ->
                        "${a.locality}, ${a.countryName}"
                    !a.subAdminArea.isNullOrBlank() -> a.subAdminArea
                    !a.adminArea.isNullOrBlank() -> a.adminArea
                    else -> "Lat %.2f, Lng %.2f".format(lat, lng)
                }
            }.getOrDefault("Lat %.2f, Lng %.2f".format(lat, lng))
        }

    /**
     * Read device location (if permitted) and persist to DataStore + prayer cache.
     * Returns null when permission missing or no fix available.
     */
    suspend fun refreshAndPersist(context: Context, prefs: PreferencesRepository): DeviceLocation? {
        val loc = lastKnownLocation(context) ?: return null
        val lat = loc.latitude
        val lng = loc.longitude
        val label = resolveLabel(context, lat, lng)
        prefs.setLocation(lat, lng, label)
        val result = PrayerTimesCalculator.calculate(lat, lng)
        PrayerTimesCache.save(
            context,
            lat,
            lng,
            PrayerTimesCalculator.Method.MWL,
            label,
            result
        )
        return DeviceLocation(lat, lng, label)
    }
}

package com.suhood.feature.mosque

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.suhood.core.designsystem.components.GlassCard
import com.suhood.core.designsystem.theme.SuhoodTypography
import com.suhood.core.designsystem.theme.suhoodColors
import com.suhood.core.network.HttpJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URLEncoder
import kotlin.math.*

data class MosquePlace(
    val id: Long,
    val name: String,
    val lat: Double,
    val lon: Double,
    val distanceMeters: Double,
    val address: String
)

object OverpassMosques {
    /** Overpass API — OpenStreetMap amenity=place_of_worship + religion=muslim (or mosque). */
    fun nearby(lat: Double, lon: Double, radiusM: Int): List<MosquePlace> {
        val query = """
            [out:json][timeout:25];
            (
              node["amenity"="place_of_worship"]["religion"="muslim"](around:$radiusM,$lat,$lon);
              way["amenity"="place_of_worship"]["religion"="muslim"](around:$radiusM,$lat,$lon);
              node["amenity"="mosque"](around:$radiusM,$lat,$lon);
              way["amenity"="mosque"](around:$radiusM,$lat,$lon);
            );
            out center tags;
        """.trimIndent()
        val url = "https://overpass-api.de/api/interpreter?data=" + URLEncoder.encode(query, "UTF-8")
        val root = JSONObject(HttpJson.get(url, timeoutMs = 30_000))
        val elements = root.optJSONArray("elements") ?: return emptyList()
        val list = mutableListOf<MosquePlace>()
        for (i in 0 until elements.length()) {
            val e = elements.getJSONObject(i)
            val tags = e.optJSONObject("tags") ?: JSONObject()
            val name = tags.optString("name").ifBlank { tags.optString("name:en").ifBlank { "Mosque" } }
            val elat = when {
                e.has("lat") -> e.getDouble("lat")
                e.optJSONObject("center") != null -> e.getJSONObject("center").getDouble("lat")
                else -> continue
            }
            val elon = when {
                e.has("lon") -> e.getDouble("lon")
                e.optJSONObject("center") != null -> e.getJSONObject("center").getDouble("lon")
                else -> continue
            }
            val dist = haversine(lat, lon, elat, elon)
            val addr = listOfNotNull(
                tags.optString("addr:street").ifBlank { null },
                tags.optString("addr:city").ifBlank { null }
            ).joinToString(", ")
            list += MosquePlace(e.optLong("id"), name, elat, elon, dist, addr)
        }
        return list.sortedBy { it.distanceMeters }
    }

    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 2 * r * asin(sqrt(a))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindMosqueScreen(onBack: () -> Unit) {
    val colors = suhoodColors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var radiusKm by remember { mutableIntStateOf(3) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var mosques by remember { mutableStateOf<List<MosquePlace>>(emptyList()) }
    var lat by remember { mutableDoubleStateOf(21.4225) }
    var lon by remember { mutableDoubleStateOf(39.8262) }
    var hasLocation by remember { mutableStateOf(false) }

    fun searchMosques() {
        scope.launch {
            loading = true
            error = null
            try {
                mosques = withContext(Dispatchers.IO) {
                    OverpassMosques.nearby(lat, lon, radiusKm * 1000)
                }
                if (mosques.isEmpty()) error = "No mosques found in OpenStreetMap data for this radius. Try a larger radius."
            } catch (e: Exception) {
                error = e.message ?: "Search failed"
                mosques = emptyList()
            } finally {
                loading = false
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val ok = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (ok) {
            fetchDeviceLocation(context) { la, lo ->
                lat = la; lon = lo; hasLocation = true
                searchMosques()
            }
        } else {
            error = "Location permission denied. Showing results near Makkah default. You can still open maps manually."
            searchMosques()
        }
    }

    LaunchedEffect(Unit) {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (fine || coarse) {
            fetchDeviceLocation(context) { la, lo ->
                lat = la; lon = lo; hasLocation = true
                searchMosques()
            }
        } else {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = { Text("Find Mosque", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = colors.textPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { searchMosques() }) {
                        Icon(Icons.Filled.Refresh, null, tint = colors.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background)
            )
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(horizontal = 16.dp)) {
            Text(
                "Data from OpenStreetMap via Overpass. Coverage varies by region. Not every mosque may be listed.",
                style = SuhoodTypography.labelSmall,
                color = colors.textMuted
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1, 3, 5, 10).forEach { km ->
                    FilterChip(
                        selected = radiusKm == km,
                        onClick = { radiusKm = km; searchMosques() },
                        label = { Text("${km} km") }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                if (hasLocation) "Using device location" else "Using fallback coordinates until location is available",
                style = SuhoodTypography.labelSmall,
                color = colors.textSecondary
            )
            Spacer(Modifier.height(12.dp))
            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colors.accentLavender)
                }
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (error != null) {
                        item {
                            Text(error ?: "", color = colors.textSecondary)
                        }
                    }
                    items(mosques, key = { it.id }) { m ->
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                val uri = Uri.parse("geo:${m.lat},${m.lon}?q=${m.lat},${m.lon}(${Uri.encode(m.name)})")
                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            },
                            contentPadding = PaddingValues(14.dp)
                        ) {
                            Text(m.name, style = SuhoodTypography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = colors.textPrimary)
                            Text(
                                formatDistance(m.distanceMeters) + if (m.address.isNotBlank()) " · ${m.address}" else "",
                                style = SuhoodTypography.labelSmall,
                                color = colors.textMuted
                            )
                            Text("Tap for directions", style = SuhoodTypography.labelSmall, color = colors.accentLavender)
                        }
                    }
                    item {
                        Text("© OpenStreetMap contributors", style = SuhoodTypography.labelSmall, color = colors.textMuted)
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

private fun formatDistance(m: Double): String =
    if (m < 1000) "${m.toInt()} m" else String.format("%.1f km", m / 1000.0)

@SuppressLint("MissingPermission")
private fun fetchDeviceLocation(context: android.content.Context, onResult: (Double, Double) -> Unit) {
    try {
        val lm = context.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
        val providers = listOf(
            android.location.LocationManager.GPS_PROVIDER,
            android.location.LocationManager.NETWORK_PROVIDER
        )
        var best: Location? = null
        for (p in providers) {
            val loc = runCatching { lm.getLastKnownLocation(p) }.getOrNull() ?: continue
            if (best == null || loc.accuracy < best.accuracy) best = loc
        }
        if (best != null) onResult(best.latitude, best.longitude)
        else onResult(21.4225, 39.8262)
    } catch (_: Exception) {
        onResult(21.4225, 39.8262)
    }
}

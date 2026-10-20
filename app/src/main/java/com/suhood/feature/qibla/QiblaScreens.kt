package com.suhood.feature.qibla

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Surface
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.suhood.core.designsystem.components.GlassCard
import com.suhood.core.designsystem.theme.SuhoodTypography
import com.suhood.core.designsystem.theme.suhoodColors
import kotlin.math.*

object QiblaScreens {

    private const val KAABA_LAT = 21.422487
    private const val KAABA_LNG = 39.826206

    @Composable
    fun Finder(onBack: () -> Unit) {
        val colors = suhoodColors
        val context = LocalContext.current
        val mainHandler = remember { Handler(Looper.getMainLooper()) }

        var hasPermission by remember {
            mutableStateOf(
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
            )
        }
        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->
            hasPermission = result.values.any { it }
        }

        var latitude by remember { mutableStateOf<Double?>(null) }
        var longitude by remember { mutableStateOf<Double?>(null) }
        var placeLabel by remember { mutableStateOf("Getting location…") }
        var accuracyM by remember { mutableStateOf<Float?>(null) }
        var azimuthDeg by remember { mutableFloatStateOf(0f) }
        var sensorOk by remember { mutableStateOf(true) }
        var sensorLabel by remember { mutableStateOf("Compass starting…") }

        LaunchedEffect(hasPermission) {
            if (!hasPermission) {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }

        // GPS / network location
        DisposableEffect(hasPermission) {
            if (!hasPermission) {
                placeLabel = "Allow location permission"
                return@DisposableEffect onDispose { }
            }
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            fun applyLocation(location: Location) {
                latitude = location.latitude
                longitude = location.longitude
                accuracyM = location.accuracy
                placeLabel = reverseGeocode(context, location.latitude, location.longitude)
                    ?: String.format("%.4f°, %.4f°", location.latitude, location.longitude)
            }
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    mainHandler.post { applyLocation(location) }
                }
                @Deprecated("Deprecated")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }
            try {
                val last = listOf(
                    LocationManager.GPS_PROVIDER,
                    LocationManager.NETWORK_PROVIDER,
                    LocationManager.PASSIVE_PROVIDER
                ).mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
                    .maxByOrNull { it.time }
                if (last != null) applyLocation(last)

                if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 1f, listener)
                }
                if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1500L, 2f, listener)
                }
            } catch (_: SecurityException) {
                placeLabel = "Location permission needed"
            }
            onDispose { runCatching { lm.removeUpdates(listener) } }
        }

        // Compass — rotation vector + accel/mag, main-thread updates, display remap
        DisposableEffect(Unit) {
            val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            val rotationVector = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            val gameRotation = sm.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
            val accelerometer = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            val magnetometer = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
            val gravity = FloatArray(3)
            val geomagnetic = FloatArray(3)
            var hasG = false
            var hasM = false
            var smoothAzimuth = 0f
            var initialized = false

            fun displayRotation(): Int {
                val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                @Suppress("DEPRECATION")
                return wm.defaultDisplay.rotation
            }

            fun publishAzimuth(rawDeg: Float) {
                var deg = rawDeg
                while (deg < 0f) deg += 360f
                while (deg >= 360f) deg -= 360f
                if (!initialized) {
                    smoothAzimuth = deg
                    initialized = true
                } else {
                    // shortest-path low-pass
                    var delta = deg - smoothAzimuth
                    if (delta > 180f) delta -= 360f
                    if (delta < -180f) delta += 360f
                    smoothAzimuth = (smoothAzimuth + delta * 0.18f + 360f) % 360f
                }
                val out = smoothAzimuth
                mainHandler.post {
                    azimuthDeg = out
                    sensorOk = true
                    sensorLabel = "Compass active"
                }
            }

            fun fromRotationMatrix(Rin: FloatArray) {
                val rotation = displayRotation()
                val outR = FloatArray(9)
                val axisX: Int
                val axisY: Int
                when (rotation) {
                    Surface.ROTATION_90 -> {
                        axisX = SensorManager.AXIS_Y
                        axisY = SensorManager.AXIS_MINUS_X
                    }
                    Surface.ROTATION_180 -> {
                        axisX = SensorManager.AXIS_MINUS_X
                        axisY = SensorManager.AXIS_MINUS_Y
                    }
                    Surface.ROTATION_270 -> {
                        axisX = SensorManager.AXIS_MINUS_Y
                        axisY = SensorManager.AXIS_X
                    }
                    else -> {
                        axisX = SensorManager.AXIS_X
                        axisY = SensorManager.AXIS_Y
                    }
                }
                SensorManager.remapCoordinateSystem(Rin, axisX, axisY, outR)
                val orient = FloatArray(3)
                SensorManager.getOrientation(outR, orient)
                var deg = Math.toDegrees(orient[0].toDouble()).toFloat()
                if (deg < 0) deg += 360f
                publishAzimuth(deg)
            }

            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    when (event.sensor.type) {
                        Sensor.TYPE_ROTATION_VECTOR, Sensor.TYPE_GAME_ROTATION_VECTOR -> {
                            val R = FloatArray(9)
                            SensorManager.getRotationMatrixFromVector(R, event.values)
                            fromRotationMatrix(R)
                        }
                        Sensor.TYPE_ACCELEROMETER -> {
                            // slight smoothing
                            for (i in 0..2) gravity[i] = gravity[i] * 0.8f + event.values[i] * 0.2f
                            hasG = true
                        }
                        Sensor.TYPE_MAGNETIC_FIELD -> {
                            for (i in 0..2) geomagnetic[i] = geomagnetic[i] * 0.8f + event.values[i] * 0.2f
                            hasM = true
                        }
                    }
                    // Always try accel+mag path as continuous source (works when RV is noisy)
                    if (hasG && hasM) {
                        val R = FloatArray(9)
                        val I = FloatArray(9)
                        if (SensorManager.getRotationMatrix(R, I, gravity, geomagnetic)) {
                            // Prefer RV if available, but still update from mag when only mag works
                            if (rotationVector == null && gameRotation == null) {
                                fromRotationMatrix(R)
                            } else {
                                // blend: still publish mag-based if RV not yet fired
                                fromRotationMatrix(R)
                            }
                        }
                    }
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                    if (accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
                        mainHandler.post { sensorLabel = "Move phone in a figure-8 to calibrate" }
                    }
                }
            }

            val delay = SensorManager.SENSOR_DELAY_GAME
            var registered = false
            rotationVector?.let {
                sm.registerListener(listener, it, delay)
                registered = true
            }
            gameRotation?.let {
                sm.registerListener(listener, it, delay)
                registered = true
            }
            accelerometer?.let {
                sm.registerListener(listener, it, delay)
                registered = true
            }
            magnetometer?.let {
                sm.registerListener(listener, it, delay)
                registered = true
            }
            if (!registered) {
                sensorOk = false
                sensorLabel = "No compass sensor on this device"
            }
            onDispose { sm.unregisterListener(listener) }
        }

        val qiblaBearing = remember(latitude, longitude) {
            val lat = latitude
            val lng = longitude
            if (lat == null || lng == null) null else computeQiblaBearing(lat, lng)
        }

        val relative = if (qiblaBearing != null) {
            var d = (qiblaBearing - azimuthDeg + 360.0) % 360.0
            if (d > 180.0) d -= 360.0
            d
        } else null
        val aligned = relative != null && abs(relative) < 8.0

        Column(
            Modifier
                .fillMaxSize()
                .background(colors.background)
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.textPrimary)
                }
                Text(
                    "Qibla",
                    style = SuhoodTypography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary
                )
            }

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                InfoChip(placeLabel.take(28), colors)
                InfoChip(
                    when {
                        accuracyM == null -> "GPS…"
                        accuracyM!! > 80 -> "Low accuracy"
                        else -> "±${accuracyM!!.toInt()} m"
                    },
                    colors
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(sensorLabel, style = SuhoodTypography.labelSmall, color = colors.textMuted)

            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                val ringColor = colors.borderStrong
                val accent = colors.accentLavender
                val q = (qiblaBearing ?: 0.0).toFloat()
                Canvas(Modifier.size(300.dp)) {
                    val cx = size.width / 2
                    val cy = size.height / 2
                    val r = size.minDimension / 2 - 10.dp.toPx()

                    // Dial rotates opposite to heading so north stays geographic on dial
                    rotate(-azimuthDeg, Offset(cx, cy)) {
                        drawCircle(color = ringColor, radius = r, style = Stroke(width = 2.5.dp.toPx()))
                        for (i in 0 until 72) {
                            val a = Math.toRadians(i * 5.0)
                            val outer = r - 2.dp.toPx()
                            val inner = if (i % 18 == 0) r - 22.dp.toPx() else if (i % 6 == 0) r - 14.dp.toPx() else r - 8.dp.toPx()
                            drawLine(
                                ringColor,
                                Offset(cx + cos(a - PI / 2).toFloat() * outer, cy + sin(a - PI / 2).toFloat() * outer),
                                Offset(cx + cos(a - PI / 2).toFloat() * inner, cy + sin(a - PI / 2).toFloat() * inner),
                                strokeWidth = if (i % 18 == 0) 3f else 1.5f
                            )
                        }
                        // Cardinal labels as ticks only (N at 0)
                        val nY = cy - (r - 36.dp.toPx())
                        drawCircle(Color(0xFFE74C3C), radius = 4.dp.toPx(), center = Offset(cx, nY))

                        // Kaaba on true qibla bearing
                        val qa = Math.toRadians(q.toDouble())
                        val kx = cx + cos(qa - PI / 2).toFloat() * (r - 40.dp.toPx())
                        val ky = cy + sin(qa - PI / 2).toFloat() * (r - 40.dp.toPx())
                        val s = 16.dp.toPx()
                        drawRoundRect(
                            color = Color(0xFF111111),
                            topLeft = Offset(kx - s / 2, ky - s / 2),
                            size = Size(s, s),
                            cornerRadius = CornerRadius(2f, 2f)
                        )
                        drawRoundRect(
                            color = Color(0xFFD4AF37),
                            topLeft = Offset(kx - s / 2, ky - s / 2),
                            size = Size(s, s * 0.25f)
                        )
                    }

                    // Phone forward needle (fixed up)
                    drawLine(
                        color = if (aligned) Color(0xFF4ADE80) else accent,
                        start = Offset(cx, cy + 28.dp.toPx()),
                        end = Offset(cx, cy - r + 30.dp.toPx()),
                        strokeWidth = 6.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawCircle(color = colors.surfaceElevated, radius = 12.dp.toPx())
                    drawCircle(
                        color = if (aligned) Color(0xFF4ADE80) else accent,
                        radius = 6.dp.toPx()
                    )
                }
            }

            Text(
                when {
                    !hasPermission -> "Allow location to find Qibla"
                    qiblaBearing == null -> "Getting location…"
                    aligned -> "✓ Facing the Qibla"
                    relative != null && relative > 0 -> "Turn right ${relative.toInt()}°"
                    relative != null -> "Turn left ${abs(relative).toInt()}°"
                    else -> "Rotate phone slowly"
                },
                style = SuhoodTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = if (aligned) Color(0xFF4ADE80) else colors.textPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            if (qiblaBearing != null) {
                Text(
                    "Qibla ${qiblaBearing.toInt()}° · Heading ${azimuthDeg.toInt()}°",
                    style = SuhoodTypography.bodySmall,
                    color = colors.textMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                )
            }

            Spacer(Modifier.height(12.dp))
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("How to use", style = SuhoodTypography.labelMedium, color = colors.textMuted)
                Text(
                    "1. Allow location · 2. Hold phone flat · 3. Rotate until the needle meets the Kaaba · Away from metal for accuracy.",
                    style = SuhoodTypography.bodySmall,
                    color = colors.textSecondary
                )
            }
            Spacer(Modifier.height(20.dp))
        }
    }

    @Composable
    private fun InfoChip(text: String, colors: com.suhood.core.designsystem.theme.SuhoodColorScheme) {
        Box(
            Modifier
                .background(colors.surfaceElevated, RoundedCornerShape(999.dp))
                .border(1.dp, colors.borderSubtle, RoundedCornerShape(999.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(text, style = SuhoodTypography.labelSmall, color = colors.textSecondary)
        }
    }

    private fun computeQiblaBearing(lat: Double, lng: Double): Double {
        val lat1 = Math.toRadians(lat)
        val lat2 = Math.toRadians(KAABA_LAT)
        val dLng = Math.toRadians(KAABA_LNG - lng)
        val y = sin(dLng) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLng)
        return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
    }

    private fun reverseGeocode(context: Context, lat: Double, lng: Double): String? {
        return runCatching {
            if (!Geocoder.isPresent()) return null
            @Suppress("DEPRECATION")
            val list = Geocoder(context).getFromLocation(lat, lng, 1) ?: return null
            val a = list.firstOrNull() ?: return null
            listOfNotNull(a.locality, a.subAdminArea, a.adminArea, a.countryName)
                .distinct()
                .take(2)
                .joinToString(", ")
                .ifBlank { null }
        }.getOrNull()
    }
}

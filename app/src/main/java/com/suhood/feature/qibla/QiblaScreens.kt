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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        var placeLabel by remember { mutableStateOf("Locating…") }
        var accuracyM by remember { mutableStateOf<Float?>(null) }
        var azimuthDeg by remember { mutableFloatStateOf(0f) }
        var sensorOk by remember { mutableStateOf(true) }

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

        // GPS
        DisposableEffect(hasPermission) {
            if (!hasPermission) return@DisposableEffect onDispose { }
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    latitude = location.latitude
                    longitude = location.longitude
                    accuracyM = location.accuracy
                    placeLabel = reverseGeocode(context, location.latitude, location.longitude)
                        ?: String.format("%.4f, %.4f", location.latitude, location.longitude)
                }
                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }
            try {
                val last = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    ?: lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (last != null) {
                    latitude = last.latitude
                    longitude = last.longitude
                    accuracyM = last.accuracy
                    placeLabel = reverseGeocode(context, last.latitude, last.longitude)
                        ?: String.format("%.4f, %.4f", last.latitude, last.longitude)
                }
                if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000L, 2f, listener)
                }
                if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 3000L, 5f, listener)
                }
            } catch (_: SecurityException) {
                placeLabel = "Location permission needed"
            }
            onDispose {
                runCatching { lm.removeUpdates(listener) }
            }
        }

        // Compass sensors
        DisposableEffect(Unit) {
            val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            val rotation = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
                ?: sm.getDefaultSensor(Sensor.TYPE_ORIENTATION)
            val accelerometer = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            val magnetometer = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
            val gravity = FloatArray(3)
            val geomagnetic = FloatArray(3)
            var hasG = false
            var hasM = false
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    when (event.sensor.type) {
                        Sensor.TYPE_ROTATION_VECTOR -> {
                            val rot = FloatArray(9)
                            SensorManager.getRotationMatrixFromVector(rot, event.values)
                            val orient = FloatArray(3)
                            SensorManager.getOrientation(rot, orient)
                            // azimuth: radians → degrees, 0 = North
                            var deg = Math.toDegrees(orient[0].toDouble()).toFloat()
                            if (deg < 0) deg += 360f
                            azimuthDeg = deg
                            sensorOk = true
                        }
                        Sensor.TYPE_ACCELEROMETER -> {
                            System.arraycopy(event.values, 0, gravity, 0, 3)
                            hasG = true
                        }
                        Sensor.TYPE_MAGNETIC_FIELD -> {
                            System.arraycopy(event.values, 0, geomagnetic, 0, 3)
                            hasM = true
                        }
                    }
                    if (hasG && hasM && rotation == null) {
                        val R = FloatArray(9)
                        val I = FloatArray(9)
                        if (SensorManager.getRotationMatrix(R, I, gravity, geomagnetic)) {
                            val orient = FloatArray(3)
                            SensorManager.getOrientation(R, orient)
                            var deg = Math.toDegrees(orient[0].toDouble()).toFloat()
                            if (deg < 0) deg += 360f
                            azimuthDeg = deg
                            sensorOk = true
                        }
                    }
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }
            if (rotation != null) {
                sm.registerListener(listener, rotation, SensorManager.SENSOR_DELAY_UI)
            } else {
                accelerometer?.let { sm.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
                magnetometer?.let { sm.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
                if (accelerometer == null && magnetometer == null) sensorOk = false
            }
            onDispose { sm.unregisterListener(listener) }
        }

        val qiblaBearing = remember(latitude, longitude) {
            val lat = latitude
            val lng = longitude
            if (lat == null || lng == null) null
            else computeQiblaBearing(lat, lng)
        }

        // Angle to rotate compass so Kaaba marker aligns when phone points correctly
        val needleRotation = remember(azimuthDeg, qiblaBearing) {
            val q = qiblaBearing ?: 0.0
            // Device heading clockwise from north; we rotate dial opposite so north stays up relative to heading
            -(azimuthDeg)
        }
        val kaabaAngleOnDial = (qiblaBearing ?: 0.0).toFloat()
        val aligned = qiblaBearing != null &&
            abs((((qiblaBearing - azimuthDeg) + 540) % 360) - 180) < 8.0

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
                Text("Qibla", style = SuhoodTypography.titleLarge.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary)
            }

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(placeLabel.take(22), colors)
                Chip(
                    when {
                        accuracyM == null -> "Waiting GPS"
                        accuracyM!! > 100 -> "Low accuracy"
                        else -> "±${accuracyM!!.toInt()} m"
                    },
                    colors
                )
            }

            Spacer(Modifier.height(24.dp))

            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                val ringColor = colors.borderStrong
                val accent = colors.accentLavender
                Canvas(Modifier.size(280.dp)) {
                    val cx = size.width / 2
                    val cy = size.height / 2
                    val r = size.minDimension / 2 - 8.dp.toPx()
                    // Outer ring
                    drawCircle(color = ringColor, radius = r, style = Stroke(width = 2.dp.toPx()))
                    // Tick marks + rotate with device
                    rotate(needleRotation, Offset(cx, cy)) {
                        for (i in 0 until 72) {
                            val a = Math.toRadians(i * 5.0)
                            val outer = r - 4.dp.toPx()
                            val inner = if (i % 6 == 0) r - 18.dp.toPx() else r - 10.dp.toPx()
                            val x1 = cx + cos(a - PI / 2).toFloat() * outer
                            val y1 = cy + sin(a - PI / 2).toFloat() * outer
                            val x2 = cx + cos(a - PI / 2).toFloat() * inner
                            val y2 = cy + sin(a - PI / 2).toFloat() * inner
                            drawLine(ringColor, Offset(x1, y1), Offset(x2, y2), strokeWidth = 2f)
                        }
                        // Kaaba marker on dial at qibla bearing
                        val qa = Math.toRadians(kaabaAngleOnDial.toDouble())
                        val kx = cx + cos(qa - PI / 2).toFloat() * (r - 36.dp.toPx())
                        val ky = cy + sin(qa - PI / 2).toFloat() * (r - 36.dp.toPx())
                        // Simple Kaaba cube
                        val s = 14.dp.toPx()
                        drawRoundRect(
                            color = Color(0xFF1A1A1A),
                            topLeft = Offset(kx - s / 2, ky - s / 2),
                            size = androidx.compose.ui.geometry.Size(s, s),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f)
                        )
                        drawRoundRect(
                            color = Color(0xFFD4AF37),
                            topLeft = Offset(kx - s / 2, ky - s / 2),
                            size = androidx.compose.ui.geometry.Size(s, s * 0.22f)
                        )
                    }
                    // Fixed phone needle (points up = direction phone faces)
                    drawLine(
                        color = if (aligned) Color(0xFF4ADE80) else accent,
                        start = Offset(cx, cy + 20.dp.toPx()),
                        end = Offset(cx, cy - r + 28.dp.toPx()),
                        strokeWidth = 5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawCircle(color = colors.surfaceElevated, radius = 10.dp.toPx())
                    drawCircle(color = if (aligned) Color(0xFF4ADE80) else accent, radius = 5.dp.toPx())
                }
            }

            Text(
                when {
                    !hasPermission -> "Allow location to find Qibla"
                    qiblaBearing == null -> "Getting location…"
                    aligned -> "Facing the Qibla"
                    else -> "Rotate phone until needle meets Kaaba"
                },
                style = SuhoodTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            if (qiblaBearing != null) {
                Text(
                    "Qibla bearing ${qiblaBearing.toInt()}° from North",
                    style = SuhoodTypography.bodySmall,
                    color = colors.textMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                )
            }
            if (!sensorOk) {
                Text(
                    "Compass sensor unavailable on this device",
                    style = SuhoodTypography.bodySmall,
                    color = colors.error,
                    modifier = Modifier.padding(8.dp)
                )
            }

            Spacer(Modifier.height(16.dp))
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Tip", style = SuhoodTypography.labelMedium, color = colors.textMuted)
                Text(
                    "Hold the phone flat. Move away from metal and magnets for better accuracy.",
                    style = SuhoodTypography.bodySmall,
                    color = colors.textSecondary
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    @Composable
    private fun Chip(text: String, colors: com.suhood.core.designsystem.theme.SuhoodColorScheme) {
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
        var brng = Math.toDegrees(atan2(y, x))
        brng = (brng + 360.0) % 360.0
        return brng
    }

    private fun reverseGeocode(context: Context, lat: Double, lng: Double): String? {
        return runCatching {
            if (!Geocoder.isPresent()) return null
            @Suppress("DEPRECATION")
            val list = Geocoder(context).getFromLocation(lat, lng, 1)
            val a = list?.firstOrNull() ?: return null
            a.locality ?: a.subAdminArea ?: a.adminArea
        }.getOrNull()
    }
}

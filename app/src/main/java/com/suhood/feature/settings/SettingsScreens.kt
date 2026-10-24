package com.suhood.feature.settings
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.ExperimentalFoundationApi
import com.suhood.app.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.material3.Switch
import androidx.compose.material3.Icon
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.background

import androidx.compose.foundation.Image

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.suhood.core.designsystem.components.*
import com.suhood.core.designsystem.theme.SuhoodSpacing
import com.suhood.core.designsystem.theme.SuhoodTypography
import com.suhood.core.designsystem.theme.ThemeMode
import com.suhood.core.designsystem.theme.suhoodColors
import com.suhood.core.navigation.Routes
import com.suhood.core.prayer.PrayerTimesCalculator
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

object SettingsScreens {

    @Composable
    fun Home(onBack: () -> Unit, onNavigate: (String) -> Unit) {
        val colors = suhoodColors
        data class Row(val label: String, val route: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
        val more = listOf(
            Row("Appearance", Routes.SETTINGS_APPEARANCE, Icons.Filled.DarkMode),
            Row("Language", Routes.SETTINGS_LANGUAGE, Icons.Filled.Language),
            Row("Prayer preferences", Routes.SETTINGS_PRAYER, Icons.Filled.Mosque),
            Row("Customize Salah times", Routes.SETTINGS_SALAH_TIMES, Icons.Filled.Timer),
            Row("Notifications", Routes.SETTINGS_NOTIFICATIONS, Icons.Filled.Notifications),
            Row("Focus", Routes.SETTINGS_FOCUS, Icons.Filled.Timer),
            Row("Audio", Routes.SETTINGS_AUDIO, Icons.Filled.MusicNote),
            Row("Accessibility", Routes.SETTINGS_ACCESSIBILITY, Icons.Filled.Accessibility),
            Row("Suhood Phone", Routes.SETTINGS_PHONE, Icons.Filled.Call),
            Row("Privacy", Routes.SETTINGS_PRIVACY, Icons.Filled.PrivacyTip),
            Row("Permissions", Routes.SETTINGS_PERMISSIONS, Icons.Filled.Security),
            Row("Tip the developer", Routes.SETTINGS_TIP, Icons.Filled.VolunteerActivism),
            Row("About Suhood", Routes.SETTINGS_ABOUT, Icons.Filled.Info)
        )
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            ScreenHeader(title = "Settings", onBack = onBack)
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = SuhoodSpacing.screenHorizontal)
                    .padding(bottom = 24.dp)
            ) {
                // Developer banner (Syed Furqan)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.dp, colors.borderSubtle, RoundedCornerShape(18.dp))
                        .clickable { onNavigate(Routes.SETTINGS_ABOUT) }
                ) {
                    Image(
                        painter = painterResource(R.drawable.developer_banner),
                        contentDescription = "Syed Furqan — Suhood developer",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        Modifier
                            .matchParentSize()
                            .background(Color.Black.copy(alpha = 0.35f))
                    )
                    Column(
                        Modifier
                            .align(Alignment.BottomStart)
                            .padding(14.dp)
                    ) {
                        Text(
                            "Syed Furqan",
                            style = SuhoodTypography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            "Technology Creator · App Developer",
                            style = SuhoodTypography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Text(
                            "“Building ideas into reality, one project at a time.”",
                            style = SuhoodTypography.labelSmall,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
                Text("Preferences", style = SuhoodTypography.labelLarge, color = colors.textMuted)
                Spacer(Modifier.height(8.dp))
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    more.forEachIndexed { i, row ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onNavigate(row.route) }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(row.icon, null, tint = colors.accentLavender, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(14.dp))
                            Text(
                                row.label,
                                style = SuhoodTypography.titleMedium,
                                color = colors.textPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Filled.ChevronRight, null, tint = colors.textMuted, modifier = Modifier.size(20.dp))
                        }
                        if (i < more.lastIndex) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(colors.borderSubtle.copy(alpha = 0.5f))
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun Appearance(settingsViewModel: SettingsViewModel, onBack: () -> Unit) {
        val colors = suhoodColors
        val mode by settingsViewModel.themeMode.collectAsStateWithLifecycle()
        val selected = when (mode) {
            ThemeMode.DARK -> 0
            ThemeMode.LIGHT -> 1
            ThemeMode.SYSTEM -> 2
        }
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = SuhoodSpacing.screenHorizontal)
        ) {
            ScreenHeader(title = "Appearance", onBack = onBack)
            Spacer(Modifier.height(16.dp))
            Text("Theme", style = SuhoodTypography.titleSmall, color = colors.textMuted)
            Spacer(Modifier.height(8.dp))
            GlassSegmentedControl(
                options = listOf("Dark", "Light", "System"),
                selectedIndex = selected,
                onSelect = {
                    settingsViewModel.setThemeMode(
                        when (it) {
                            1 -> ThemeMode.LIGHT
                            2 -> ThemeMode.SYSTEM
                            else -> ThemeMode.DARK
                        }
                    )
                }
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Changes apply immediately across the entire app.",
                style = SuhoodTypography.bodySmall,
                color = colors.textSecondary
            )
        }
    }

    @Composable
    fun Language(onBack: () -> Unit) {
        val colors = suhoodColors
        var selected by remember { mutableIntStateOf(0) }
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = SuhoodSpacing.screenHorizontal)) {
            ScreenHeader(title = "Language", onBack = onBack)
            Spacer(Modifier.height(16.dp))
            GlassSegmentedControl(
                options = listOf("English", "العربية"),
                selectedIndex = selected,
                onSelect = { selected = it }
            )
            Spacer(Modifier.height(12.dp))
            Text("Arabic RTL support is prepared for religious content.", style = SuhoodTypography.bodySmall, color = colors.textMuted)
        }
    }

    @Composable
    fun About(onBack: () -> Unit) {
        val colors = suhoodColors
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = SuhoodSpacing.screenHorizontal)) {
            ScreenHeader(title = "About Suhood", onBack = onBack)
            Spacer(Modifier.height(16.dp))
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Suhood", style = SuhoodTypography.headlineSmall, color = colors.textPrimary)
                Text("Version 1.1.0", style = SuhoodTypography.bodySmall, color = colors.textMuted)
                Spacer(Modifier.height(12.dp))
                Text(
                    "Help Muslims spend less time distracted and more time reconnecting with Allah.\n\nFree. Non-commercial. No ads. No subscriptions.",
                    style = SuhoodTypography.bodyMedium,
                    color = colors.textSecondary
                )
            }
        }
    }

    @Composable
    fun Privacy(onBack: () -> Unit) {
        val colors = suhoodColors
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = SuhoodSpacing.screenHorizontal)) {
            ScreenHeader(title = "Privacy", onBack = onBack)
            Spacer(Modifier.height(16.dp))
            GlassCard {
                Text(
                    "Suhood stores preferences, alarms, bookmarks, and focus history only on this device. No account is required. No ads, analytics SDKs, or cloud sync are included.",
                    style = SuhoodTypography.bodyMedium,
                    color = colors.textSecondary
                )
            }
        }
    }

    @Composable
    fun PrayerPrefs(settingsViewModel: SettingsViewModel, onBack: () -> Unit) {
        val colors = suhoodColors
        val method by settingsViewModel.prayerMethod.collectAsStateWithLifecycle()
        val label by settingsViewModel.locationLabel.collectAsStateWithLifecycle()
        val lat by settingsViewModel.latitude.collectAsStateWithLifecycle()
        val lng by settingsViewModel.longitude.collectAsStateWithLifecycle()
        val methods = PrayerTimesCalculator.Method.entries
        val methodIndex = methods.indexOf(method).coerceAtLeast(0)
        var latText by remember { mutableStateOf(lat.toString()) }
        var lngText by remember { mutableStateOf(lng.toString()) }
        var nameText by remember { mutableStateOf(label) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SuhoodSpacing.screenHorizontal)
                .padding(bottom = 24.dp)
        ) {
            ScreenHeader(title = "Prayer preferences", onBack = onBack)
            Spacer(Modifier.height(8.dp))
            DemoStatusLabel("Calculated times · not a mosque timetable")
            Spacer(Modifier.height(16.dp))
            Text("Calculation method", style = SuhoodTypography.titleSmall, color = colors.textMuted)
            Spacer(Modifier.height(8.dp))
            methods.forEachIndexed { i, m ->
                GlassCard(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    selected = i == methodIndex,
                    onClick = { settingsViewModel.setPrayerMethod(m) }
                ) {
                    Text(m.label, style = SuhoodTypography.titleSmall, color = colors.textPrimary)
                    Text(m.name, style = SuhoodTypography.labelSmall, color = colors.textMuted)
                }
            }
            Spacer(Modifier.height(20.dp))
            Text("Manual location", style = SuhoodTypography.titleSmall, color = colors.textMuted)
            Spacer(Modifier.height(4.dp))
            Text(
                "Used when location permission is not granted. Default is a Makkah fallback — change this to your city.",
                style = SuhoodTypography.bodySmall,
                color = colors.textSecondary
            )
            Spacer(Modifier.height(12.dp))
            GlassSearchField(query = nameText, onQueryChange = { nameText = it }, placeholder = "City label")
            Spacer(Modifier.height(8.dp))
            GlassSearchField(query = latText, onQueryChange = { latText = it }, placeholder = "Latitude")
            Spacer(Modifier.height(8.dp))
            GlassSearchField(query = lngText, onQueryChange = { lngText = it }, placeholder = "Longitude")
            Spacer(Modifier.height(12.dp))
            GlassButton(
                text = "Save location",
                onClick = {
                    val la = latText.toDoubleOrNull()
                    val ln = lngText.toDoubleOrNull()
                    if (la != null && ln != null) {
                        settingsViewModel.setManualLocation(la, ln, nameText.ifBlank { "Custom" })
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            GlassButton(
                text = "Use Makkah fallback",
                onClick = {
                    settingsViewModel.setManualLocation(
                        21.4225, 39.8262,
                        "Fallback: Makkah (not your local mosque)"
                    )
                    latText = "21.4225"
                    lngText = "39.8262"
                    nameText = "Fallback: Makkah (not your local mosque)"
                },
                primary = false,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    @Composable
    fun NotificationPrefs(onBack: () -> Unit) {
        val colors = suhoodColors
        val context = androidx.compose.ui.platform.LocalContext.current
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = SuhoodSpacing.screenHorizontal)) {
            ScreenHeader(title = "Notifications", onBack = onBack)
            Spacer(Modifier.height(12.dp))
            Text(
                "Prayer reminders use calculated local times and Android alarms. Grant notification permission so reminders can appear.",
                style = SuhoodTypography.bodyMedium,
                color = colors.textSecondary
            )
            Spacer(Modifier.height(16.dp))
            GlassButton(
                text = "Open notification settings",
                onClick = {
                    runCatching {
                        context.startActivity(
                            android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            GlassButton(
                text = "Reschedule prayer reminders",
                onClick = { com.suhood.core.prayer.SalahReminderScheduler.rescheduleAll(context) },
                primary = false,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    @Composable
    fun FocusPrefs(onBack: () -> Unit) {
        val colors = suhoodColors
        val context = androidx.compose.ui.platform.LocalContext.current
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = SuhoodSpacing.screenHorizontal)) {
            ScreenHeader(title = "Focus preferences", onBack = onBack)
            Spacer(Modifier.height(12.dp))
            Text(
                "Focus uses a timer plus optional Usage Access and Accessibility (same family of APIs as AppBlock) to detect apps you selected. Android does not allow force-closing other apps without special privileges.",
                style = SuhoodTypography.bodyMedium,
                color = colors.textSecondary
            )
            Spacer(Modifier.height(16.dp))
            GlassButton(
                text = "Open Usage Access",
                onClick = { com.suhood.feature.focus.AppBlockHelper.openUsageAccessSettings(context) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            GlassButton(
                text = "Open Accessibility (Suhood Focus)",
                onClick = { com.suhood.feature.focus.AppBlockHelper.openAccessibilitySettings(context) },
                primary = false,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    @Composable
    fun AudioPrefs(onBack: () -> Unit) {
        val colors = suhoodColors
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = SuhoodSpacing.screenHorizontal)) {
            ScreenHeader(title = "Audio preferences", onBack = onBack)
            Spacer(Modifier.height(16.dp))
            Text(
                "Playback uses Media3 ExoPlayer with local sample tones (test audio only — not commercial nasheeds). A licensed catalog can plug into AudioCatalogProvider later.",
                style = SuhoodTypography.bodyMedium,
                color = colors.textSecondary
            )
        }
    }

    @Composable
    fun Accessibility(onBack: () -> Unit) {
        val colors = suhoodColors
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = SuhoodSpacing.screenHorizontal)) {
            ScreenHeader(title = "Accessibility", onBack = onBack)
            Spacer(Modifier.height(16.dp))
            Text(
                "Content descriptions, minimum touch targets (~48 dp), and contrast are considered throughout. Reduce motion preference is supported where the platform allows.",
                style = SuhoodTypography.bodyMedium,
                color = colors.textSecondary
            )
        }
    }

    @Composable
    fun PermissionsGuide(onBack: () -> Unit) {
        val colors = suhoodColors
        val context = androidx.compose.ui.platform.LocalContext.current
        Column(
            Modifier.fillMaxSize().statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SuhoodSpacing.screenHorizontal)
                .padding(bottom = 32.dp)
        ) {
            ScreenHeader(title = "Permissions", onBack = onBack)
            Spacer(Modifier.height(8.dp))
            Text(
                "Suhood only asks for access when a feature needs it. You can change these anytime in system settings.",
                style = SuhoodTypography.bodyMedium,
                color = colors.textSecondary
            )
            Spacer(Modifier.height(16.dp))
            GlassCard(Modifier.fillMaxWidth()) {
                Text("Notifications", style = SuhoodTypography.titleSmall, color = colors.textPrimary)
                Text("Prayer reminders and alarms", style = SuhoodTypography.bodySmall, color = colors.textMuted)
                Spacer(Modifier.height(8.dp))
                GlassButton(text = "Open", onClick = {
                    runCatching {
                        context.startActivity(
                            android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                        )
                    }
                }, primary = false, modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(8.dp))
            GlassCard(Modifier.fillMaxWidth()) {
                Text("Alarms & reminders", style = SuhoodTypography.titleSmall, color = colors.textPrimary)
                Text("Exact prayer and custom alarm times", style = SuhoodTypography.bodySmall, color = colors.textMuted)
                Spacer(Modifier.height(8.dp))
                GlassButton(text = "Open", onClick = {
                    runCatching {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                            context.startActivity(
                                android.content.Intent(
                                    android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                                )
                            )
                        }
                    }
                }, primary = false, modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(8.dp))
            GlassCard(Modifier.fillMaxWidth()) {
                Text("Usage Access", style = SuhoodTypography.titleSmall, color = colors.textPrimary)
                Text("Detect which app is open during Focus", style = SuhoodTypography.bodySmall, color = colors.textMuted)
                Spacer(Modifier.height(8.dp))
                GlassButton(text = "Open", onClick = {
                    com.suhood.feature.focus.AppBlockHelper.openUsageAccessSettings(context)
                }, primary = false, modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(8.dp))
            GlassCard(Modifier.fillMaxWidth()) {
                Text("Accessibility (Suhood Focus)", style = SuhoodTypography.titleSmall, color = colors.textPrimary)
                Text("Show return-to-focus when a limited app opens. No password reading.", style = SuhoodTypography.bodySmall, color = colors.textMuted)
                Spacer(Modifier.height(8.dp))
                GlassButton(text = "Open", onClick = {
                    com.suhood.feature.focus.AppBlockHelper.openAccessibilitySettings(context)
                }, primary = false, modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(8.dp))
            GlassCard(Modifier.fillMaxWidth()) {
                Text("Music and audio", style = SuhoodTypography.titleSmall, color = colors.textPrimary)
                Text("Play nasheeds from your library", style = SuhoodTypography.bodySmall, color = colors.textMuted)
            }
            Spacer(Modifier.height(8.dp))
            GlassCard(Modifier.fillMaxWidth()) {
                Text("Internet", style = SuhoodTypography.titleSmall, color = colors.textPrimary)
                Text("Quran text/audio and Archive search", style = SuhoodTypography.bodySmall, color = colors.textMuted)
            }
        }
    }

    @Composable
    fun TipSupport(onBack: () -> Unit) {
        val colors = suhoodColors
        val context = androidx.compose.ui.platform.LocalContext.current
        val upiId = "syedfurqan95@fam"
        val upiUri = "upi://pay?pa=$upiId&pn=Suhood%20Developer&cu=INR&tn=Tip%20for%20Suhood"
        val qrBitmap = remember {
            runCatching {
                val matrix = com.google.zxing.qrcode.QRCodeWriter().encode(
                    upiUri,
                    com.google.zxing.BarcodeFormat.QR_CODE,
                    512,
                    512,
                    mapOf(com.google.zxing.EncodeHintType.MARGIN to 1)
                )
                val bmp = android.graphics.Bitmap.createBitmap(512, 512, android.graphics.Bitmap.Config.ARGB_8888)
                for (x in 0 until 512) for (y in 0 until 512) {
                    bmp.setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
                bmp
            }.getOrNull()
        }
        Column(
            Modifier.fillMaxSize().statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SuhoodSpacing.screenHorizontal)
                .padding(bottom = 32.dp)
        ) {
            ScreenHeader(title = "Tip the developer", onBack = onBack)
            Spacer(Modifier.height(8.dp))
            Text(
                "Suhood is free and non-commercial. Tipping is optional and never unlocks paid features.",
                style = SuhoodTypography.bodyMedium,
                color = colors.textSecondary
            )
            Spacer(Modifier.height(16.dp))
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "SYED FURQAN",
                    style = SuhoodTypography.headlineMedium,
                    color = colors.textPrimary
                )
                Spacer(Modifier.height(12.dp))
                if (qrBitmap != null) {
                    androidx.compose.foundation.Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "Payment QR for SYED FURQAN",
                        modifier = Modifier.fillMaxWidth().height(240.dp).padding(8.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
                GlassButton(
                    text = "Open payment app",
                    onClick = {
                        runCatching {
                            context.startActivity(
                                android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse(upiUri)
                                )
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(24.dp))
            Text("Purpose of Suhood", style = SuhoodTypography.titleLarge, color = colors.textPrimary)
            Spacer(Modifier.height(8.dp))
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    """Suhood helps Muslims reduce distractions and return to Salah, Quran, Dhikr, and calm focus.

It is a personal tool—not a product for sale:

• Calculated prayer times and local reminders on your device
• Focus sessions that can gently interrupt apps you choose to limit
• Quran reading and beneficial Islamic audio
• Alarms that respect your schedule

No ads. No subscriptions. No account. Your data stays on your phone.

If this project helps you, a tip via UPI is optional gratitude. May Allah accept our efforts.

— Suhood developer""",
                    style = SuhoodTypography.bodyMedium,
                    color = colors.textSecondary
                )
            }
        }
    }


    @Composable
    fun SalahTimes(onBack: () -> Unit) {
        val context = androidx.compose.ui.platform.LocalContext.current
        val store = remember { com.suhood.core.prayer.PrayerSettingsStore(context) }
        val settings by store.settings.collectAsStateWithLifecycle(
            initialValue = com.suhood.core.prayer.PrayerSettings()
        )
        val scope = rememberCoroutineScope()
        val colors = suhoodColors
        val prayers = listOf(
            "Fajr" to settings.masjidFajr,
            "Dhuhr" to settings.masjidDhuhr,
            "Asr" to settings.masjidAsr,
            "Maghrib" to settings.masjidMaghrib,
            "Isha" to settings.masjidIsha
        )
        var editing by remember { mutableStateOf<String?>(null) }
        var wheelHour by remember { mutableIntStateOf(5) }
        var wheelMinute by remember { mutableIntStateOf(0) }

        fun openEditor(name: String, time: String) {
            val parts = time.split(":")
            wheelHour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 5
            wheelMinute = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
            editing = name
        }

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SuhoodSpacing.screenHorizontal)
                .padding(bottom = 32.dp)
        ) {
            ScreenHeader(title = "Customize Salah times", onBack = onBack)
            Text(
                "Tap a prayer to set time with the wheel (same as Alarms). Saved times apply to the Home Salah bar, reminders, and Focus block when Prayer mode is Masjid.",
                style = SuhoodTypography.bodySmall,
                color = colors.textMuted
            )
            Spacer(Modifier.height(12.dp))
            GlassButton(
                text = "Use masjid timetable",
                onClick = {
                    scope.launch {
                        store.update {
                            it.copy(mode = com.suhood.core.prayer.PrayerTimeMode.MASJID)
                        }
                        com.suhood.core.prayer.SalahReminderScheduler.rescheduleAll(context)
                        com.suhood.core.prayer.FajrAutoScheduler.reschedule(context)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            prayers.forEach { (name, time) ->
                GlassCard(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    onClick = { openEditor(name, time) }
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(name, style = SuhoodTypography.titleMedium, color = colors.textPrimary)
                        Text(time, style = SuhoodTypography.headlineSmall, color = colors.silverBright)
                    }
                }
            }
        }

        // Alarm-style hour / minute wheel dialog
        if (editing != null) {
            val prayer = editing!!
            AlertDialog(
                onDismissRequest = { editing = null },
                title = {
                    Text(
                        "Set $prayer time",
                        color = colors.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                text = {
                    Row(
                        Modifier.fillMaxWidth().height(220.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SalahSettingsTimeWheel(
                            items = (0..23).map { "%02d".format(it) },
                            selectedIndex = wheelHour,
                            onSelected = { wheelHour = it },
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            ":",
                            color = colors.textPrimary,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                        SalahSettingsTimeWheel(
                            items = (0..59).map { "%02d".format(it) },
                            selectedIndex = wheelMinute,
                            onSelected = { wheelMinute = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val hhmm = "%02d:%02d".format(wheelHour, wheelMinute)
                            scope.launch {
                                // Seeds other slots + switches to MASJID so times apply app-wide
                                store.setCustomSalahTime(prayer, hhmm)
                                com.suhood.core.prayer.SalahReminderScheduler.rescheduleAll(context)
                                com.suhood.core.prayer.FajrAutoScheduler.reschedule(context)
                                editing = null
                            }
                        }
                    ) {
                        Text("Done", color = colors.accentLavender, fontWeight = FontWeight.SemiBold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { editing = null }) {
                        Text("Cancel", color = colors.textMuted)
                    }
                },
                containerColor = colors.surfaceElevated
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SalahSettingsTimeWheel(
    items: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val itemH = 44.dp
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = selectedIndex.coerceIn(0, items.lastIndex.coerceAtLeast(0))
    )
    val fling = rememberSnapFlingBehavior(lazyListState = listState)
    var lastTickIndex by remember { mutableIntStateOf(selectedIndex) }

    LaunchedEffect(listState.firstVisibleItemIndex, listState.isScrollInProgress) {
        val idx = listState.firstVisibleItemIndex
        if (listState.isScrollInProgress && idx != lastTickIndex) {
            lastTickIndex = idx
            runCatching {
                val am = context.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
                am.playSoundEffect(android.media.AudioManager.FX_KEY_CLICK, 0.55f)
            }
        }
    }
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val idx = listState.firstVisibleItemIndex.coerceIn(0, items.lastIndex.coerceAtLeast(0))
            if (idx != selectedIndex) onSelected(idx)
        }
    }
    LaunchedEffect(selectedIndex) {
        if (!listState.isScrollInProgress && listState.firstVisibleItemIndex != selectedIndex) {
            listState.animateScrollToItem(selectedIndex.coerceIn(0, items.lastIndex.coerceAtLeast(0)))
        }
    }

    LazyColumn(
        state = listState,
        flingBehavior = fling,
        modifier = modifier.height(220.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(vertical = 88.dp)
    ) {
        items(items.size) { i ->
            val selected = i == selectedIndex
            Box(
                Modifier.height(itemH).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    items[i],
                    color = if (selected) Color.White else Color.White.copy(alpha = 0.28f),
                    fontSize = 28.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

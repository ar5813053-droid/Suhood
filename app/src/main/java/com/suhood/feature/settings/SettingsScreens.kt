package com.suhood.feature.settings

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.DarkMode
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
import androidx.compose.ui.platform.LocalContext
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
        val context = androidx.compose.ui.platform.LocalContext.current

        val colors = suhoodColors
        data class Row(val label: String, val route: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
        val more = listOf(
            Row("Appearance", Routes.SETTINGS_APPEARANCE, Icons.Filled.DarkMode),
            Row("Language", Routes.SETTINGS_LANGUAGE, Icons.Filled.Language),
            Row("Edit personalization", Routes.SETTINGS_PERSONALIZATION, Icons.Filled.AccountCircle),
            Row("Prayer preferences", Routes.SETTINGS_PRAYER, Icons.Filled.Mosque),
            Row("Customize Salah times", Routes.SETTINGS_SALAH_TIMES, Icons.Filled.Timer),
            Row("Notifications", Routes.SETTINGS_NOTIFICATIONS, Icons.Filled.Notifications),
            Row("Focus", Routes.SETTINGS_FOCUS, Icons.Filled.Timer),
            Row("Audio", Routes.SETTINGS_AUDIO, Icons.Filled.MusicNote),
            Row("Accessibility", Routes.SETTINGS_ACCESSIBILITY, Icons.Filled.Accessibility),
            Row("Privacy", Routes.SETTINGS_PRIVACY, Icons.Filled.PrivacyTip),
            Row("Permissions", Routes.SETTINGS_PERMISSIONS, Icons.Filled.Security),
            Row("Tip the developer", Routes.SETTINGS_TIP, Icons.Filled.VolunteerActivism),
            Row("Terms & Conditions", Routes.SETTINGS_TERMS, Icons.Filled.Info),
            Row("Support", "settings/support", Icons.Filled.Info),
            Row("Instagram", "external:instagram", Icons.Filled.Info),
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
                                .clickable {
                                    when (row.route) {
                                        "external:instagram" -> {
                                            runCatching {
                                                context.startActivity(
                                                    Intent(
                                                        Intent.ACTION_VIEW,
                                                        Uri.parse("https://www.instagram.com/suhood.app?xtok=MTQzdzdwbWQ2bTRm")
                                                    )
                                                )
                                            }
                                        }
                                        "settings/support" -> {
                                            runCatching {
                                                context.startActivity(
                                                    Intent(Intent.ACTION_SENDTO).apply {
                                                        data = Uri.parse("mailto:support.suhood@gmail.com")
                                                        putExtra(Intent.EXTRA_SUBJECT, "Suhood Support")
                                                    }
                                                )
                                            }
                                        }
                                        else -> onNavigate(row.route)
                                    }
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (row.route == "external:instagram") {
                                Icon(
                                    painter = painterResource(R.drawable.ic_instagram),
                                    contentDescription = "Instagram",
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(22.dp)
                                )
                            } else {
                                Icon(row.icon, null, tint = colors.accentLavender, modifier = Modifier.size(22.dp))
                            }
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
            Spacer(Modifier.height(12.dp))
            val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
            var snap by remember {
                mutableStateOf(com.suhood.core.permissions.PermissionStatusManager.snapshot(context))
            }
            DisposableEffect(lifecycleOwner) {
                val obs = androidx.lifecycle.LifecycleEventObserver { _, e ->
                    if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                        snap = com.suhood.core.permissions.PermissionStatusManager.snapshot(context)
                        runCatching {
                            com.suhood.core.permissions.PermissionReminderScheduler.reschedule(context)
                        }
                    }
                }
                lifecycleOwner.lifecycle.addObserver(obs)
                onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
            }
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Focus blocking status", style = SuhoodTypography.titleSmall, color = colors.textPrimary)
                Text(snap.blockingCapability.statusMessage, style = SuhoodTypography.bodySmall, color = colors.textMuted)
                if (!snap.blockingCapability.isReliableBlocking) {
                    Text(
                        "Blocking is not marked active until a supported mechanism works.",
                        style = SuhoodTypography.labelSmall,
                        color = Color(0xFFE0C36A)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            snap.items.filter { it.applicability != com.suhood.core.permissions.PermissionApplicability.NOT_APPLICABLE }.forEach { item ->
                GlassCard(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(item.title, style = SuhoodTypography.titleSmall, color = colors.textPrimary)
                            Text(item.reason, style = SuhoodTypography.bodySmall, color = colors.textMuted)
                            Text("Feature: " + item.feature, style = SuhoodTypography.labelSmall, color = colors.textMuted)
                            Text(
                                if (item.granted) "Enabled" else "Not enabled",
                                style = SuhoodTypography.labelMedium,
                                color = if (item.granted) Color(0xFF6FCF97) else Color(0xFFE0C36A)
                            )
                            if (item.mayBeRestricted && !item.granted) {
                                Text(
                                    "Some devices restrict Accessibility for sideloaded apps. Enable restricted settings for Suhood if Android shows that option.",
                                    style = SuhoodTypography.labelSmall,
                                    color = colors.textMuted
                                )
                            }
                        }
                    }
                    if (!item.granted) {
                        Spacer(Modifier.height(8.dp))
                        GlassButton(
                            text = "Open settings",
                            onClick = {
                                runCatching {
                                    com.suhood.core.permissions.PermissionStatusManager.openSettingsFor(context, item.kind)
                                }
                            },
                            primary = false,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
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
        // 12-hour wheel: hour 1..12 index maps to labels; amPm 0=AM 1=PM
        var wheelHour12 by remember { mutableIntStateOf(5) }   // 1..12
        var wheelMinute by remember { mutableIntStateOf(0) }
        var wheelAmPm by remember { mutableIntStateOf(0) }     // 0=AM, 1=PM

        fun openEditor(name: String, time: String) {
            val parts = time.split(":")
            val h24 = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 5
            val m = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
            // Convert 24h -> 12h + AM/PM
            when {
                h24 == 0 -> { wheelHour12 = 12; wheelAmPm = 0 }
                h24 < 12 -> { wheelHour12 = h24; wheelAmPm = 0 }
                h24 == 12 -> { wheelHour12 = 12; wheelAmPm = 1 }
                else -> { wheelHour12 = h24 - 12; wheelAmPm = 1 }
            }
            wheelMinute = m
            editing = name
        }

        fun toHhmm24(hour12: Int, minute: Int, amPm: Int): String {
            val h24 = when {
                amPm == 0 && hour12 == 12 -> 0
                amPm == 0 -> hour12
                amPm == 1 && hour12 == 12 -> 12
                else -> hour12 + 12
            }
            return "%02d:%02d".format(h24.coerceIn(0, 23), minute.coerceIn(0, 59))
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
                        Text(
                            formatSalahTime12h(time),
                            style = SuhoodTypography.headlineSmall,
                            color = colors.silverBright
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(
                "Friday · Jumu'ah",
                style = SuhoodTypography.titleSmall,
                color = colors.textPrimary
            )
            Text(
                "Independent of regular Dhuhr. On Friday the Home bar shows Jumu'ah using this time when enabled; otherwise calculated Dhuhr is used as a labelled fallback (not your mosque's confirmed congregation).",
                style = SuhoodTypography.bodySmall,
                color = colors.textMuted
            )
            Spacer(Modifier.height(8.dp))
            GlassCard(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Custom Jumu'ah time", style = SuhoodTypography.titleMedium, color = colors.textPrimary)
                        Text(
                            if (settings.customJumuahEnabled && settings.customJumuahTime.isNotBlank())
                                "Manual · ${formatSalahTime12h(settings.customJumuahTime)}"
                            else
                                "Using calculated Dhuhr fallback",
                            style = SuhoodTypography.bodySmall,
                            color = colors.textMuted
                        )
                    }
                    androidx.compose.material3.Switch(
                        checked = settings.customJumuahEnabled,
                        onCheckedChange = { on ->
                            scope.launch {
                                store.update {
                                    it.copy(
                                        customJumuahEnabled = on,
                                        customJumuahTime = if (on && it.customJumuahTime.isBlank())
                                            it.masjidDhuhr else it.customJumuahTime
                                    )
                                }
                            }
                        }
                    )
                }
            }
            if (settings.customJumuahEnabled) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    onClick = {
                        openEditor(
                            "Jumu'ah",
                            settings.customJumuahTime.ifBlank { settings.masjidDhuhr }
                        )
                    }
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Jumu'ah", style = SuhoodTypography.titleMedium, color = colors.textPrimary)
                        Text(
                            formatSalahTime12h(
                                settings.customJumuahTime.ifBlank { settings.masjidDhuhr }
                            ),
                            style = SuhoodTypography.headlineSmall,
                            color = Color(0xFF6FCF97)
                        )
                    }
                }
                TextButton(
                    onClick = {
                        scope.launch {
                            store.update {
                                it.copy(customJumuahEnabled = false, customJumuahTime = "")
                            }
                        }
                    }
                ) {
                    Text("Reset to calculated Dhuhr", color = colors.textMuted)
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
                    Column(Modifier.fillMaxWidth()) {
                        // Preview 12h label so user sees AM/PM clearly
                        val preview = run {
                            val label = "%d:%02d %s".format(
                                wheelHour12,
                                wheelMinute,
                                if (wheelAmPm == 0) "AM" else "PM"
                            )
                            val hhmm24 = toHhmm24(wheelHour12, wheelMinute, wheelAmPm)
                            "$label  ·  $hhmm24"
                        }
                        Text(
                            preview,
                            color = colors.textMuted,
                            style = SuhoodTypography.labelMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(
                            Modifier.fillMaxWidth().height(220.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Hours 1–12 (index 0 = 1, …, index 11 = 12)
                            SalahSettingsTimeWheel(
                                items = (1..12).map { "%02d".format(it) },
                                selectedIndex = (wheelHour12 - 1).coerceIn(0, 11),
                                onSelected = { wheelHour12 = it + 1 },
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
                            SalahSettingsTimeWheel(
                                items = listOf("AM", "PM"),
                                selectedIndex = wheelAmPm,
                                onSelected = { wheelAmPm = it },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val hhmm = toHhmm24(wheelHour12, wheelMinute, wheelAmPm)
                            scope.launch {
                                if (prayer == "Jumu'ah") {
                                    store.update {
                                        it.copy(
                                            customJumuahEnabled = true,
                                            customJumuahTime = hhmm
                                        )
                                    }
                                } else {
                                    store.setCustomSalahTime(prayer, hhmm)
                                    com.suhood.core.prayer.SalahReminderScheduler.rescheduleAll(context)
                                    com.suhood.core.prayer.FajrAutoScheduler.reschedule(context)
                                }
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

private fun formatSalahTime12h(hhmm: String): String {
    val parts = hhmm.split(":")
    val h = parts.getOrNull(0)?.toIntOrNull() ?: return hhmm
    val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
    val amPm = if (h < 12) "AM" else "PM"
    val h12 = when {
        h == 0 -> 12
        h > 12 -> h - 12
        else -> h
    }
    return "%d:%02d %s".format(h12, m, amPm)
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


@Composable
fun SettingsTermsScreen(onBack: () -> Unit) {
    val colors = suhoodColors
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        ScreenHeader(title = "Terms & Conditions", onBack = onBack)
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SuhoodSpacing.screenHorizontal)
                .padding(bottom = 32.dp)
        ) {
            Text(
                TERMS_SETTINGS_TEXT,
                color = colors.textSecondary,
                fontSize = 14.sp,
                lineHeight = 22.sp
            )
        }
    }
}

private val TERMS_SETTINGS_TEXT = """
SUHOOD — TERMS & CONDITIONS

Last updated: October 2026

1. Acceptance
By using Suhood you agree to these Terms. If you do not agree, do not use the app.

2. Purpose
Suhood is a personal Islamic lifestyle tool for Salah reminders, focus protection, Quran, and Dhikr. It is not a substitute for scholarly guidance or medical advice.

3. Permissions
Features such as Salah app blocking require Accessibility, Usage Access, Notifications, and optionally Display over other apps. You may revoke these at any time in system Settings. Blocking depends on Android and OEM limits.

4. Privacy
Profile answers, plans, and preferences are stored on your device. Suhood does not sell personal spiritual data. Network is used only for optional content when those features are used.

5. Accuracy of prayer times
Prayer times are calculated or customized by you. Always verify with your local mosque when needed.

6. Acceptable use
Do not misuse Accessibility or other permissions to harm devices, other apps, or people.

7. No warranty
Suhood is provided as-is. We do not guarantee uninterrupted blocking, alarms, or notifications on every OEM.

8. Limitation of liability
To the fullest extent permitted by law, Suhood and its developers are not liable for indirect damages arising from use of the app.

9. Changes
We may update these Terms. Continued use after updates means acceptance of the revised Terms.

10. Contact
Support: support.suhood@gmail.com
Instagram: https://www.instagram.com/suhood.app
""".trimIndent()

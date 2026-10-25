package com.suhood.feature.settings

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suhood.core.designsystem.components.GlassButton
import com.suhood.core.designsystem.components.ScreenHeader
import com.suhood.core.designsystem.theme.SuhoodSpacing
import com.suhood.core.designsystem.theme.SuhoodTypography
import com.suhood.core.designsystem.theme.suhoodColors
import com.suhood.feature.focus.FocusChallengeStore
import com.suhood.feature.focus.FocusSessionController
import com.suhood.feature.focus.InstalledAppsRepository
import com.suhood.feature.onboarding.OnboardingProfileStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PersonalizationEditScreen(
    onBack: () -> Unit,
    settingsViewModel: SettingsViewModel
) {
    val context = LocalContext.current
    val colors = suhoodColors
    val store = remember { OnboardingProfileStore.get(context) }
    val focus = remember { FocusSessionController.get(context) }
    val scope = rememberCoroutineScope()

    // Draft state loaded once from store
    var age by remember { mutableStateOf(store.ageGroup) }
    var gender by remember { mutableStateOf(store.gender) }
    var life by remember { mutableStateOf(store.dailyLife) }
    var lang by remember { mutableStateOf(store.language) }
    var goals by remember { mutableStateOf(store.goals) }
    var phone by remember { mutableStateOf(store.phoneUsage) }
    var social by remember { mutableStateOf(store.socialUsage) }
    var late by remember { mutableStateOf(store.lateNight) }
    var checkFreq by remember { mutableStateOf(store.checkFrequency) }
    var notifInt by remember { mutableStateOf(store.notifInterrupt) }
    var hardest by remember { mutableStateOf(store.hardestPrayer) }
    var reminder by remember { mutableStateOf(store.reminderStyle) }
    var fajr by remember { mutableStateOf(store.prayerStatus("Fajr")) }
    var dhuhr by remember { mutableStateOf(store.prayerStatus("Dhuhr")) }
    var asr by remember { mutableStateOf(store.prayerStatus("Asr")) }
    var maghrib by remember { mutableStateOf(store.prayerStatus("Maghrib")) }
    var isha by remember { mutableStateOf(store.prayerStatus("Isha")) }
    var qMin by remember { mutableIntStateOf(store.quranMinutes) }
    var dMin by remember { mutableIntStateOf(store.dhikrMinutes) }
    var qRem by remember { mutableStateOf(store.quranReminder) }
    var selectedApps by remember { mutableStateOf(focus.getBlockedApps()) }
    var appQuery by remember { mutableStateOf("") }
    var installed by remember { mutableStateOf(emptyList<com.suhood.feature.focus.InstalledAppInfo>()) }
    var dirty by remember { mutableStateOf(false) }
    var showDiscard by remember { mutableStateOf(false) }
    var savedFlash by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    fun markDirty() { dirty = true; savedFlash = false }

    LaunchedEffect(Unit) {
        installed = withContext(Dispatchers.IO) {
            InstalledAppsRepository.loadLauncherApps(context)
        }
    }

    fun saveAll() {
        saving = true
        store.ageGroup = age
        store.gender = gender
        store.dailyLife = life
        store.language = lang
        store.goals = goals
        store.phoneUsage = phone
        store.socialUsage = social
        store.lateNight = late
        store.checkFrequency = checkFreq
        store.notifInterrupt = notifInt
        store.hardestPrayer = hardest
        store.reminderStyle = reminder
        store.setPrayerStatus("Fajr", fajr)
        store.setPrayerStatus("Dhuhr", dhuhr)
        store.setPrayerStatus("Asr", asr)
        store.setPrayerStatus("Maghrib", maghrib)
        store.setPrayerStatus("Isha", isha)
        store.quranMinutes = qMin
        store.dhikrMinutes = dMin
        store.quranReminder = qRem
        focus.setBlockedApps(selectedApps)
        FocusChallengeStore.get(context).setFeatureEnabled(selectedApps.isNotEmpty())
        settingsViewModel.setQuranCompanion(qMin, 9, 0, qRem)
        // language key for repo
        scope.launch {
            runCatching {
                val code = when (lang) {
                    "Hindi" -> "hi"
                    "Urdu" -> "ur"
                    else -> "en"
                }
                // PreferencesRepository via VM if available — optional
            }
            dirty = false
            saving = false
            savedFlash = true
        }
    }

    fun tryBack() {
        if (dirty) showDiscard = true else onBack()
    }

    BackHandler { tryBack() }

    if (showDiscard) {
        AlertDialog(
            onDismissRequest = { showDiscard = false },
            title = { Text("Discard changes?") },
            text = { Text("You have unsaved personalization edits.") },
            confirmButton = {
                TextButton(onClick = { showDiscard = false; onBack() }) {
                    Text("Discard", color = Color(0xFFE8A0A0))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscard = false }) {
                    Text("Keep editing")
                }
            },
            containerColor = Color(0xFF141519)
        )
    }

    val filtered = remember(installed, appQuery) {
        if (appQuery.isBlank()) installed
        else installed.filter {
            it.label.contains(appQuery, true) || it.packageName.contains(appQuery, true)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(colors.background)
    ) {
        ScreenHeader(title = "Edit personalization", onBack = { tryBack() })
        if (savedFlash) {
            Text(
                "Saved",
                color = Color(0xFF9FE8C8),
                modifier = Modifier.padding(horizontal = SuhoodSpacing.screenHorizontal),
                style = SuhoodTypography.labelMedium
            )
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SuhoodSpacing.screenHorizontal)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionTitle("About you")
            Label("Age group")
            ChipRow(
                listOf("Under 18", "18–24", "25–34", "35–44", "45+", "Prefer not to say"),
                age
            ) { age = it; markDirty() }
            Label("Gender")
            ChipRow(listOf("Male", "Female", "Prefer not to say"), gender) { gender = it; markDirty() }
            Label("Daily life")
            ChipRow(
                listOf("Student", "Employed", "Business/Self-employed", "Freelancer/Remote", "Homemaker", "Looking for work", "Other"),
                life
            ) { life = it; markDirty() }
            Label("Language")
            ChipRow(listOf("English", "Hindi", "Hinglish", "Urdu"), lang) { lang = it; markDirty() }

            SectionTitle("Goals")
            val goalOpts = listOf(
                "Salah consistency", "Quran reading", "Dhikr and Tasbeeh",
                "Reducing phone distractions", "Improving daily routine"
            )
            goalOpts.forEach { g ->
                val on = g in goals
                SelectRow(g, on) {
                    goals = if (on) goals - g else goals + g
                    markDirty()
                }
            }

            SectionTitle("Phone habits")
            val times = listOf("< 1 hour", "1–2 hours", "2–4 hours", "4–6 hours", "6+ hours", "Prefer not to say")
            Label("Daily phone use")
            ChipRow(times, phone) { phone = it; markDirty() }
            Label("Social media")
            ChipRow(times, social) { social = it; markDirty() }
            Label("Late-night scrolling")
            ChipRow(listOf("Rarely", "Sometimes", "Often", "Prefer not to say"), late) { late = it; markDirty() }
            Label("Check frequency")
            ChipRow(listOf("A few times/day", "Hourly", "Constantly", "Prefer not to say"), checkFreq) { checkFreq = it; markDirty() }
            Label("Notifications interrupt Salah?")
            ChipRow(listOf("Yes", "Sometimes", "No", "Prefer not to say"), notifInt) { notifInt = it; markDirty() }

            SectionTitle("Distracting apps")
            Text(
                "Synced with Focus block list (package names).",
                color = colors.textMuted,
                style = SuhoodTypography.labelSmall
            )
            OutlinedTextField(
                value = appQuery,
                onValueChange = { appQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search") },
                singleLine = true
            )
            Text("${selectedApps.size} selected", color = colors.textMuted, style = SuhoodTypography.labelSmall)
            // Cap list height via limited items display
            filtered.take(40).forEach { app ->
                val on = app.packageName in selectedApps
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (on) Color(0xFF1A2E2A) else colors.surface)
                        .clickable {
                            selectedApps =
                                if (on) selectedApps - app.packageName else selectedApps + app.packageName
                            markDirty()
                        }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val bmp = remember(app.packageName) {
                        app.icon?.let { drawableToBitmap(it) }
                    }
                    if (bmp != null) {
                        Image(bmp.asImageBitmap(), null, Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)))
                    } else {
                        Box(Modifier.size(32.dp).background(Color(0xFF2A2D33), RoundedCornerShape(8.dp)))
                    }
                    Spacer(Modifier.size(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(app.label, color = colors.textPrimary, style = SuhoodTypography.bodyMedium)
                        Text(app.packageName, color = colors.textMuted, style = SuhoodTypography.labelSmall, maxLines = 1)
                    }
                    if (on) androidx.compose.material3.Icon(Icons.Filled.Check, null, tint = Color(0xFF9FE8C8))
                }
            }
            if (filtered.size > 40) {
                Text("Showing first 40 — refine search", color = colors.textMuted, style = SuhoodTypography.labelSmall)
            }

            SectionTitle("Salah routine")
            val statuses = listOf("Usually on time", "Sometimes delayed", "I want to improve", "Prefer not to answer")
            Label("Fajr"); ChipRow(statuses, fajr) { fajr = it; markDirty() }
            Label("Dhuhr"); ChipRow(statuses, dhuhr) { dhuhr = it; markDirty() }
            Label("Asr"); ChipRow(statuses, asr) { asr = it; markDirty() }
            Label("Maghrib"); ChipRow(statuses, maghrib) { maghrib = it; markDirty() }
            Label("Isha"); ChipRow(statuses, isha) { isha = it; markDirty() }
            Label("Hardest prayer")
            ChipRow(listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha", "Prefer not to say"), hardest) { hardest = it; markDirty() }
            Label("Reminder style")
            ChipRow(listOf("Gentle", "Balanced", "Firm"), reminder) { reminder = it; markDirty() }

            SectionTitle("Quran & Dhikr")
            Label("Daily Quran (minutes)")
            ChipRow(listOf("5", "10", "20", "30"), qMin.toString()) { qMin = it.toInt(); markDirty() }
            Label("Daily Dhikr (minutes)")
            ChipRow(listOf("5", "10", "15", "20"), dMin.toString()) { dMin = it.toInt(); markDirty() }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Quran reminder", color = colors.textPrimary)
                Switch(checked = qRem, onCheckedChange = { qRem = it; markDirty() })
            }

            Text(
                "Data stays on this device. Optional fields can stay blank.",
                color = colors.textMuted,
                style = SuhoodTypography.labelSmall
            )
        }
        GlassButton(
            text = if (saving) "Saving…" else "Save changes",
            onClick = { if (!saving) saveAll() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SuhoodSpacing.screenHorizontal)
                .padding(bottom = 20.dp)
        )
    }
}

@Composable
private fun SectionTitle(t: String) {
    Text(
        t,
        color = Color(0xFFC7CBD1),
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun Label(t: String) {
    Text(t, color = Color(0xFF9AA3AE), fontSize = 12.sp)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipRow(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { opt ->
            val on = selected == opt
            Box(
                Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (on) Color(0xFF1A2E2A) else Color(0xFF141519))
                    .border(1.dp, if (on) Color(0x669FE8C8) else Color(0x33FFFFFF), RoundedCornerShape(20.dp))
                    .clickable { onSelect(opt) }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(opt, color = if (on) Color(0xFF9FE8C8) else Color(0xFFF5F6F7), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun SelectRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Color(0xFF1A2E2A) else Color(0xFF141519))
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color(0xFFF5F6F7), fontSize = 14.sp)
        if (selected) androidx.compose.material3.Icon(Icons.Filled.Check, null, tint = Color(0xFF9FE8C8))
    }
}

private fun drawableToBitmap(drawable: Drawable): Bitmap? {
    return try {
        if (drawable is BitmapDrawable && drawable.bitmap != null && !drawable.bitmap.isRecycled) {
            return drawable.bitmap
        }
        val w = drawable.intrinsicWidth.coerceIn(1, 192)
        val h = drawable.intrinsicHeight.coerceIn(1, 192)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        bmp
    } catch (_: Exception) {
        null
    }
}

package com.suhood.feature.onboarding

import androidx.compose.ui.Modifier

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.suhood.app.SuhoodApplication
import com.suhood.core.designsystem.components.GlassButton
import com.suhood.core.designsystem.theme.SuhoodSpacing
import com.suhood.core.designsystem.theme.SuhoodTypography
import com.suhood.core.designsystem.theme.suhoodColors
import com.suhood.core.datastore.PreferencesRepository
import com.suhood.feature.focus.AppBlockHelper
import com.suhood.feature.focus.FocusAccessibilityService
import com.suhood.feature.focus.FocusChallengeStore
import com.suhood.feature.focus.FocusSessionController
import com.suhood.feature.focus.InstalledAppsRepository
import com.suhood.feature.settings.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TOTAL_STEPS = 12

@Composable
fun OnboardingFlow(
    onComplete: () -> Unit,
    settingsViewModel: SettingsViewModel
) {
    val context = LocalContext.current
    val store = remember { OnboardingProfileStore.get(context) }
    var step by remember { mutableIntStateOf(store.step) }
    val colors = suhoodColors
    val scope = rememberCoroutineScope()

    fun go(to: Int) {
        step = to.coerceIn(0, TOTAL_STEPS - 1)
        store.step = step
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF08090B))
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF111316), Color(0xFF08090B), Color(0xFF08090B))
                    )
                )
        )
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = SuhoodSpacing.xl)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OnboardingProgress(step, TOTAL_STEPS, Modifier.weight(1f))
                if (step in 1..10) {
                    TextButton(onClick = {
                        if (step < 9) go(9) else if (step < 11) go(11)
                    }) {
                        Text("Skip", color = Color(0xFF9AA3AE))
                    }
                }
            }

            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    (slideInHorizontally { it / 4 } + fadeIn(tween(220)))
                        .togetherWith(slideOutHorizontally { -it / 4 } + fadeOut(tween(180)))
                },
                modifier = Modifier.weight(1f),
                label = "onboarding"
            ) { page ->
                when (page) {
                    0 -> WelcomeStep(onContinue = { go(1) })
                    1 -> AboutYouStep(store, onContinue = { go(2) }, onBack = { go(0) })
                    2 -> GoalsStep(store, onContinue = { go(3) }, onBack = { go(1) })
                    3 -> HabitsStep(store, onContinue = { go(4) }, onBack = { go(2) })
                    4 -> EducationStep(onContinue = { go(5) }, onBack = { go(3) })
                    5 -> SalahRoutineStep(store, onContinue = { go(6) }, onBack = { go(4) })
                    6 -> QuranDhikrStep(store, settingsViewModel, onContinue = { go(7) }, onBack = { go(5) })
                    7 -> PlanStep(store, onContinue = { go(8) }, onBack = { go(6) })
                    8 -> PermissionsStep(onContinue = { go(9) }, onBack = { go(7) })
                    9 -> TestSetupStep(onContinue = { go(10) }, onBack = { go(8) })
                    10 -> AppsStep(onContinue = { go(11) }, onBack = { go(9) })
                    else -> CompleteStep(
                        store = store,
                        onEnter = {
                            scope.launch {
                                FocusChallengeStore.get(context).setFeatureEnabled(
                                    FocusSessionController.get(context).getBlockedApps().isNotEmpty()
                                )
                                store.step = 0
                                onComplete()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingProgress(step: Int, total: Int, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(total) { i ->
            Box(
                Modifier
                    .weight(1f)
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(if (i <= step) Color(0xFFC7CBD1) else Color(0xFF2A2D33))
            )
        }
    }
}

@Composable
private fun StepScaffold(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    onContinue: (() -> Unit)? = null,
    continueLabel: String = "Continue",
    content: @Composable () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        if (onBack != null) {
            TextButton(onClick = onBack) {
                Text("Back", color = Color(0xFF9AA3AE))
            }
        }
        Text(
            title,
            color = Color(0xFFF5F6F7),
            fontSize = 26.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 32.sp
        )
        if (subtitle != null) {
            Spacer(Modifier.height(8.dp))
            Text(subtitle, color = Color(0xFF9AA3AE), fontSize = 14.sp, lineHeight = 20.sp)
        }
        Spacer(Modifier.height(16.dp))
        Box(Modifier.weight(1f)) { content() }
        if (onContinue != null) {
            GlassButton(
                text = continueLabel,
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp, top = 8.dp)
            )
        }
    }
}

@Composable
private fun Chip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) Color(0xFF1A2E2A) else Color(0xFF141519))
            .border(1.dp, if (selected) Color(0x669FE8C8) else Color(0x33FFFFFF), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(label, color = if (selected) Color(0xFF9FE8C8) else Color(0xFFF5F6F7), fontSize = 13.sp)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipRow(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { opt ->
            Chip(opt, selected == opt) { onSelect(opt) }
        }
    }
}

@Composable
private fun GlassCardBox(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF141519))
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) { content() }
}

// —— Screens ——

@Composable
private fun WelcomeStep(onContinue: () -> Unit) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("◆", color = Color(0xFFC7CBD1), fontSize = 40.sp)
        Spacer(Modifier.height(12.dp))
        Text("Suhood", color = Color(0xFFF5F6F7), fontSize = 32.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(24.dp))
        Text(
            "Make room for what matters.",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Reconnect with Salah. Refocus your life.",
            color = Color(0xFF9AA3AE),
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(40.dp))
        GlassButton(text = "Get Started", onClick = onContinue, modifier = Modifier.fillMaxWidth())
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AboutYouStep(store: OnboardingProfileStore, onContinue: () -> Unit, onBack: () -> Unit) {
    var age by remember { mutableStateOf(store.ageGroup) }
    var gender by remember { mutableStateOf(store.gender) }
    var life by remember { mutableStateOf(store.dailyLife) }
    var lang by remember { mutableStateOf(store.language) }
    StepScaffold(
        title = "About you",
        subtitle = "Optional — used only for personalization on this device. Not judgment.",
        onBack = onBack,
        onContinue = {
            store.ageGroup = age
            store.gender = gender
            store.dailyLife = life
            store.language = lang
            onContinue()
        }
    ) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Text("Age group", color = Color(0xFFC7CBD1), fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            ChipRow(
                listOf("Under 18", "18–24", "25–34", "35–44", "45+", "Prefer not to say"),
                age
            ) { age = it }
            Spacer(Modifier.height(16.dp))
            Text("Gender", color = Color(0xFFC7CBD1), fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            ChipRow(listOf("Male", "Female", "Prefer not to say"), gender) { gender = it }
            Spacer(Modifier.height(16.dp))
            Text("Daily life", color = Color(0xFFC7CBD1), fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            ChipRow(
                listOf("Student", "Employed", "Business/Self-employed", "Freelancer/Remote", "Homemaker", "Looking for work", "Other"),
                life
            ) { life = it }
            Spacer(Modifier.height(16.dp))
            Text("Preferred language", color = Color(0xFFC7CBD1), fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            ChipRow(listOf("English", "Hindi", "Hinglish", "Urdu"), lang) { lang = it }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GoalsStep(store: OnboardingProfileStore, onContinue: () -> Unit, onBack: () -> Unit) {
    var selected by remember { mutableStateOf(store.goals) }
    val options = listOf(
        "Salah consistency",
        "Quran reading",
        "Dhikr and Tasbeeh",
        "Reducing phone distractions",
        "Improving daily routine"
    )
    StepScaffold(
        title = "What would you like to improve?",
        subtitle = "Select all that apply.",
        onBack = onBack,
        onContinue = {
            store.goals = selected
            onContinue()
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            options.forEach { opt ->
                val on = opt in selected
                GlassCardBox {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                selected = if (on) selected - opt else selected + opt
                            },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(opt, color = Color(0xFFF5F6F7), fontSize = 16.sp)
                        if (on) Icon(Icons.Filled.Check, null, tint = Color(0xFF9FE8C8))
                    }
                }
            }
        }
    }
}

@Composable
private fun HabitsStep(store: OnboardingProfileStore, onContinue: () -> Unit, onBack: () -> Unit) {
    var phone by remember { mutableStateOf(store.phoneUsage) }
    var social by remember { mutableStateOf(store.socialUsage) }
    var late by remember { mutableStateOf(store.lateNight) }
    var check by remember { mutableStateOf(store.checkFrequency) }
    var notif by remember { mutableStateOf(store.notifInterrupt) }
    val times = listOf("< 1 hour", "1–2 hours", "2–4 hours", "4–6 hours", "6+ hours", "Prefer not to say")
    StepScaffold(
        title = "Your phone habits",
        subtitle = "Estimates only — unless Usage Access is on later. You can skip any question.",
        onBack = onBack,
        onContinue = {
            store.phoneUsage = phone
            store.socialUsage = social
            store.lateNight = late
            store.checkFrequency = check
            store.notifInterrupt = notif
            onContinue()
        }
    ) {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Average daily phone use", color = Color(0xFFC7CBD1), fontSize = 13.sp)
            ChipRow(times, phone) { phone = it }
            Text("Daily social media", color = Color(0xFFC7CBD1), fontSize = 13.sp)
            ChipRow(times, social) { social = it }
            Text("Late-night scrolling", color = Color(0xFFC7CBD1), fontSize = 13.sp)
            ChipRow(listOf("Rarely", "Sometimes", "Often", "Prefer not to say"), late) { late = it }
            Text("How often you check your phone", color = Color(0xFFC7CBD1), fontSize = 13.sp)
            ChipRow(listOf("A few times/day", "Hourly", "Constantly", "Prefer not to say"), check) { check = it }
            Text("Do notifications interrupt Salah or focus?", color = Color(0xFFC7CBD1), fontSize = 13.sp)
            ChipRow(listOf("Yes", "Sometimes", "No", "Prefer not to say"), notif) { notif = it }
        }
    }
}

@Composable
private fun EducationStep(onContinue: () -> Unit, onBack: () -> Unit) {
    val cards = listOf(
        "Notifications can pull attention away at the wrong moment.",
        "Endless feeds are designed to keep you longer than you planned.",
        "Habit loops make opening apps automatic — a short pause helps.",
        "Late scrolling can crowd out sleep and Salah preparation."
    )
    StepScaffold(
        title = "How distraction works",
        subtitle = "Your phone is a tool. You decide what deserves your attention.",
        onBack = onBack,
        onContinue = onContinue
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
            cards.forEach { line ->
                GlassCardBox {
                    Text(line, color = Color(0xFFF5F6F7), fontSize = 15.sp, lineHeight = 22.sp)
                }
            }
        }
    }
}

@Composable
private fun AppsStep(onContinue: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val controller = remember { FocusSessionController.get(context) }
    var selected by remember { mutableStateOf(controller.getBlockedApps()) }
    var apps by remember { mutableStateOf(emptyList<com.suhood.feature.focus.InstalledAppInfo>()) }
    var query by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) { InstalledAppsRepository.loadLauncherApps(context) }
    }
    val filtered = remember(apps, query) {
        if (query.isBlank()) apps
        else apps.filter {
            it.label.contains(query, true) || it.packageName.contains(query, true)
        }
    }
    StepScaffold(
        title = "Apps to block at Salah",
        subtitle = "Selected apps are blocked during prayer time and can show a short Islamic pause. Choose at least the apps that distract you most.",
        onBack = onBack,
        onContinue = {
            controller.setBlockedApps(selected)
            FocusChallengeStore.get(context).setFeatureEnabled(selected.isNotEmpty())
            onContinue()
        }
    ) {
        Column {
            androidx.compose.material3.OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search apps", color = Color(0xFF6B7280)) },
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            Text("${selected.size} selected", color = Color(0xFF9AA3AE), fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(filtered, key = { it.packageName }) { app ->
                    val on = app.packageName in selected
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (on) Color(0xFF1A2E2A) else Color(0xFF141519))
                            .clickable {
                                selected = if (on) selected - app.packageName else selected + app.packageName
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val bmp = remember(app.packageName) {
                            runCatching {
                                drawableToBitmap(context.packageManager.getApplicationIcon(app.packageName))
                            }.getOrNull()
                        }
                        if (bmp != null) {
                            Image(bmp.asImageBitmap(), null, Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)))
                        } else {
                            Box(Modifier.size(36.dp).background(Color(0xFF2A2D33), RoundedCornerShape(8.dp)))
                        }
                        Spacer(Modifier.size(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(app.label, color = Color(0xFFF5F6F7), fontSize = 15.sp)
                            Text(app.packageName, color = Color(0xFF6B7280), fontSize = 11.sp, maxLines = 1)
                        }
                        if (on) Icon(Icons.Filled.Check, null, tint = Color(0xFF9FE8C8))
                    }
                }
            }
        }
    }
}

@Composable
private fun SalahRoutineStep(store: OnboardingProfileStore, onContinue: () -> Unit, onBack: () -> Unit) {
    val prayers = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")
    val statuses = listOf("Usually on time", "Sometimes delayed", "I want to improve", "Prefer not to answer")
    var local by remember {
        mutableStateOf(prayers.associateWith { store.prayerStatus(it) })
    }
    var hardest by remember { mutableStateOf(store.hardestPrayer) }
    var style by remember { mutableStateOf(store.reminderStyle) }
    StepScaffold(
        title = "Your Salah routine",
        subtitle = "Optional. There is no shame in wanting to improve.",
        onBack = onBack,
        onContinue = {
            local.forEach { (k, v) -> store.setPrayerStatus(k, v) }
            store.hardestPrayer = hardest
            store.reminderStyle = style
            onContinue()
        }
    ) {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            prayers.forEach { p ->
                Text(p, color = Color(0xFFC7CBD1), fontSize = 13.sp)
                ChipRow(statuses, local[p] ?: "") { local = local + (p to it) }
            }
            Text("Which prayer is hardest to maintain?", color = Color(0xFFC7CBD1), fontSize = 13.sp)
            ChipRow(prayers + "Prefer not to say", hardest) { hardest = it }
            Text("Reminder style", color = Color(0xFFC7CBD1), fontSize = 13.sp)
            ChipRow(listOf("Gentle", "Balanced", "Firm"), style) { style = it }
        }
    }
}

@Composable
private fun QuranDhikrStep(
    store: OnboardingProfileStore,
    settingsViewModel: SettingsViewModel,
    onContinue: () -> Unit,
    onBack: () -> Unit
) {
    var qMin by remember { mutableIntStateOf(store.quranMinutes) }
    var dMin by remember { mutableIntStateOf(store.dhikrMinutes) }
    var rem by remember { mutableStateOf(store.quranReminder) }
    StepScaffold(
        title = "Quran & Dhikr goals",
        subtitle = "Small daily targets. You can change these anytime in Settings.",
        onBack = onBack,
        onContinue = {
            store.quranMinutes = qMin
            store.dhikrMinutes = dMin
            store.quranReminder = rem
            // Wire to existing daily goal if ViewModel supports pages-ish target
            runCatching {
                settingsViewModel.setQuranCompanion(qMin, 9, 0, rem)
            }
            onContinue()
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Daily Quran time", color = Color(0xFFC7CBD1), fontSize = 13.sp)
            ChipRow(listOf("5", "10", "20", "30"), qMin.toString()) { qMin = it.toInt() }
            Text("Daily Dhikr time", color = Color(0xFFC7CBD1), fontSize = 13.sp)
            ChipRow(listOf("5", "10", "15", "20"), dMin.toString()) { dMin = it.toInt() }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Optional Quran reminder", color = Color(0xFFF5F6F7))
                Switch(checked = rem, onCheckedChange = { rem = it })
            }
        }
    }
}

@Composable
private fun PlanStep(store: OnboardingProfileStore, onContinue: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val apps = remember { FocusSessionController.get(context).getBlockedApps().size }
    StepScaffold(
        title = "Your Suhood plan",
        subtitle = "Recommendations from your answers. Editable anytime in the app.",
        onBack = onBack,
        onContinue = onContinue,
        continueLabel = "Looks good"
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PlanRow("Reminder style", store.reminderStyle)
            PlanRow("Distracting apps selected", "$apps apps")
            PlanRow("Quran daily", "${store.quranMinutes} min")
            PlanRow("Dhikr daily", "${store.dhikrMinutes} min")
            PlanRow("Goals", store.goals.joinToString().ifBlank { "—" })
            PlanRow("Hardest prayer", store.hardestPrayer.ifBlank { "—" })
            GlassCardBox {
                Text(
                    "Next: enable notifications, Usage Access, and Accessibility so Salah lock and mindful pauses can work.",
                    color = Color(0xFF9AA3AE),
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun PlanRow(label: String, value: String) {
    GlassCardBox {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color(0xFF9AA3AE), fontSize = 13.sp)
            Text(value, color = Color(0xFFF5F6F7), fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun PermissionsStep(onContinue: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    var notifOk by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var usageOk by remember { mutableStateOf(AppBlockHelper.hasUsageAccess(context)) }
    var a11yOk by remember { mutableStateOf(FocusAccessibilityService.isEnabled(context)) }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notifOk = it || Build.VERSION.SDK_INT < 33
    }
    LaunchedEffect(Unit) {
        // refresh when returning — simple loop not needed; user taps refresh via continue
    }
    StepScaffold(
        title = "Permissions & protection",
        subtitle = "Each item is explained. Blocking is only active after Accessibility is really on.",
        onBack = onBack,
        onContinue = {
            usageOk = AppBlockHelper.hasUsageAccess(context)
            a11yOk = FocusAccessibilityService.isEnabled(context)
            onContinue()
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
            PermRow(
                "Notifications",
                "Salah reminders and Adhan sound",
                notifOk,
                if (notifOk) "Enabled" else "Enable"
            ) {
                if (Build.VERSION.SDK_INT >= 33) {
                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            PermRow(
                "Usage Access",
                "Detect which app is open for lock / pause",
                usageOk,
                if (usageOk) "Enabled" else "Open settings"
            ) { AppBlockHelper.openUsageAccessSettings(context) }
            PermRow(
                "Accessibility (Suhood Focus)",
                "Required for app blocking & mindful pauses",
                a11yOk,
                if (a11yOk) "Enabled" else "Open settings"
            ) { FocusAccessibilityService.openSettings(context) }
            PermRow(
                "Exact alarms",
                "Reliable Salah-time triggers",
                true,
                "Check"
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.startActivity(
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            }
            PermRow(
                "Location",
                "Accurate prayer times for your city (or set city manually later)",
                true,
                "Open"
            ) {
                context.startActivity(
                    Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
            Text(
                "Opening a settings screen does not mean protection is on. We verify on the next step.",
                color = Color(0xFF6B7280),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun PermRow(title: String, sub: String, ok: Boolean, btn: String, onClick: () -> Unit) {
    GlassCardBox {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = Color(0xFFF5F6F7), fontWeight = FontWeight.Medium)
                Text(sub, color = Color(0xFF9AA3AE), fontSize = 12.sp)
            }
            Box(
                Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (ok) Color(0xFF1A2E2A) else Color(0xFF2A2D33))
                    .clickable(onClick = onClick)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(btn, color = if (ok) Color(0xFF9FE8C8) else Color(0xFFF5F6F7), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun TestSetupStep(onContinue: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    var usageOk by remember { mutableStateOf(AppBlockHelper.hasUsageAccess(context)) }
    var a11yOk by remember { mutableStateOf(FocusAccessibilityService.isEnabled(context)) }
    var notifMsg by remember { mutableStateOf<String?>(null) }
    val protection = usageOk && a11yOk
    StepScaffold(
        title = "Test your setup",
        subtitle = if (protection) "Protection checks passed." else "Some checks still need attention.",
        onBack = onBack,
        onContinue = onContinue,
        continueLabel = if (protection) "Continue" else "Continue anyway"
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            StatusLine("Usage Access", usageOk)
            StatusLine("Accessibility", a11yOk)
            GlassButton(
                text = "Send test notification",
                onClick = {
                    runCatching {
                        val n = NotificationCompat.Builder(context, SuhoodApplication.CHANNEL_SALAH_LOCK)
                            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                            .setContentTitle("Suhood test")
                            .setContentText("Notifications are working.")
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .build()
                        NotificationManagerCompat.from(context).notify(99101, n)
                        notifMsg = "Notification sent — check shade / sound."
                    }.onFailure { notifMsg = "Could not send: ${it.message}" }
                },
                modifier = Modifier.fillMaxWidth()
            )
            if (notifMsg != null) Text(notifMsg!!, color = Color(0xFF9AA3AE), fontSize = 13.sp)
            GlassButton(
                text = "Refresh permission status",
                onClick = {
                    usageOk = AppBlockHelper.hasUsageAccess(context)
                    a11yOk = FocusAccessibilityService.isEnabled(context)
                },
                modifier = Modifier.fillMaxWidth(),
                primary = false
            )
            Text(
                "Android limit: Force Stop or turning off Accessibility stops blocking. OEM battery settings can also delay alarms.",
                color = Color(0xFF6B7280),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun StatusLine(label: String, ok: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color(0xFFF5F6F7))
        Text(if (ok) "OK" else "Missing", color = if (ok) Color(0xFF9FE8C8) else Color(0xFFE8A0A0))
    }
}


@Composable
private fun CompleteStep(store: OnboardingProfileStore, onEnter: () -> Unit) {
    val particles = remember {
        List(42) {
            ConfettiParticle(
                x = kotlin.random.Random.nextFloat(),
                speed = 0.004f + kotlin.random.Random.nextFloat() * 0.012f,
                color = listOf(
                    Color(0xFF9FE8C8), Color(0xFFC7CBD1), Color(0xFFE8C07A),
                    Color(0xFF7EB6FF), Color(0xFFE8A0A0)
                ).random(),
                size = 4f + kotlin.random.Random.nextFloat() * 8f,
                phase = kotlin.random.Random.nextFloat()
            )
        }
    }
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(16)
            tick++
        }
    }
    Box(Modifier.fillMaxSize()) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val h = size.height
            val w = size.width
            particles.forEach { p ->
                val y = ((tick * p.speed + p.phase) % 1.2f) * h
                val x = p.x * w
                drawCircle(color = p.color, radius = p.size, center = androidx.compose.ui.geometry.Offset(x, y))
            }
        }
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("✓", color = Color(0xFF9FE8C8), fontSize = 56.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                "You're all set",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Preferences saved on this device.\nSalah lock and mindful pauses are ready when permissions stay on.",
                color = Color(0xFF9AA3AE),
                textAlign = TextAlign.Center,
                fontSize = 14.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "${store.goals.size} goals · ${store.reminderStyle} reminders",
                color = Color(0xFFC7CBD1),
                fontSize = 13.sp
            )
            Spacer(Modifier.height(32.dp))
            GlassButton(text = "Enter Suhood", onClick = onEnter, modifier = Modifier.fillMaxWidth())
        }
    }
}

private data class ConfettiParticle(
    val x: Float,
    val speed: Float,
    val color: Color,
    val size: Float,
    val phase: Float
)


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

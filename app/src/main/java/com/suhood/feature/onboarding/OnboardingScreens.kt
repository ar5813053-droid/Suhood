package com.suhood.feature.onboarding

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.suhood.feature.focus.AppBlockHelper
import com.suhood.feature.focus.FocusAccessibilityService
import com.suhood.feature.focus.FocusChallengeStore
import com.suhood.feature.focus.FocusSessionController
import com.suhood.feature.focus.InstalledAppInfo
import com.suhood.feature.focus.InstalledAppsRepository
import com.suhood.feature.settings.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Liquid Glass tokens (HTML prototype)
private val Void = Color(0xFF000408)
private val Moon = Color(0xFF8BB8D4)
private val T1 = Color(0xF5FFFFFF)
private val T2 = Color(0x9EFFFFFF)
private val T3 = Color(0x5CFFFFFF)
private val Glass1 = Color(0x0EFFFFFF)
private val GlassSel = Color(0x2491B4E6)
private val B1 = Color(0x1AFFFFFF)
private val BSel = Color(0x5991B4E6)
private val R2 = 18.dp
private val R3 = 24.dp
private val R4 = 32.dp

private const val TOTAL = 12

@Composable
fun OnboardingFlow(
    onComplete: () -> Unit,
    settingsViewModel: SettingsViewModel
) {
    val context = LocalContext.current
    val store = remember { OnboardingProfileStore.get(context) }
    var step by remember { mutableIntStateOf(store.step.coerceIn(0, TOTAL - 1)) }
    val scope = rememberCoroutineScope()

    fun go(to: Int) {
        step = to.coerceIn(0, TOTAL - 1)
        store.step = step
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Void)
    ) {
        // Soft radial glow
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x381A3A5C), Color.Transparent),
                        radius = 900f
                    )
                )
        )
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Progress
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(Color(0x0FFFFFFF))
            ) {
                val pct = if (step <= 0) 0f else (step.toFloat() / (TOTAL - 1)).coerceIn(0f, 1f)
                Box(
                    Modifier
                        .fillMaxWidth(pct)
                        .height(2.dp)
                        .background(Brush.horizontalGradient(listOf(Moon, Moon.copy(alpha = 0.7f))))
                )
            }
            // Nav
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (step > 0) {
                    Box(
                        Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Glass1)
                            .border(1.dp, B1, CircleShape)
                            .clickable { go(step - 1) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = T2, modifier = Modifier.size(18.dp))
                    }
                } else {
                    Spacer(Modifier.size(34.dp))
                }
                Spacer(Modifier.weight(1f))
                Text(
                    if (step == 0) "" else "$step of ${TOTAL - 1}",
                    color = T3,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.weight(1f))
                // Skip only on early optional screens (not required ones)
                val skipTarget = when (step) {
                    1 -> 2 // about optional parts → goals still required later
                    else -> null
                }
                if (skipTarget != null && step in 1..1) {
                    Text(
                        "Skip",
                        color = Moon,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable { go(skipTarget) }
                    )
                } else {
                    Spacer(Modifier.width(40.dp))
                }
            }

            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    (slideInHorizontally { it / 4 } + fadeIn()) togetherWith
                        (slideOutHorizontally { -it / 4 } + fadeOut())
                },
                modifier = Modifier.weight(1f),
                label = "onb"
            ) { s ->
                Box(Modifier.fillMaxSize().padding(horizontal = 22.dp)) {
                    when (s) {
                        0 -> WelcomeStep { go(1) }
                        1 -> AboutStep(store) { go(2) }
                        2 -> GoalsStep(store) { go(3) }
                        3 -> HabitsStep(store) { go(4) }
                        4 -> EducationStep { go(5) }
                        5 -> AppsStep(store) { go(6) }
                        6 -> SalahStep(store) { go(7) }
                        7 -> PracticeStep(store, settingsViewModel) { go(8) }
                        8 -> PlanStep(store) { go(9) }
                        9 -> PermissionsStep { go(10) }
                        10 -> PreviewStep(store) { go(11) }
                        else -> CompleteStep(store) {
                            scope.launch {
                                FocusChallengeStore.get(context).setFeatureEnabled(
                                    FocusSessionController.get(context).getBlockedApps().isNotEmpty()
                                )
                                // Regenerate plan once more with latest apps
                                val labels = withContext(Dispatchers.IO) {
                                    emptyList<String>()
                                }
                                val plan = PersonalizedPlanEngine.generate(store.toAnswers(labels))
                                store.savePlan(plan)
                                store.step = 0
                                onComplete()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrimaryButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    val shape = RoundedCornerShape(R4)
    Box(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                if (enabled) Brush.verticalGradient(listOf(Color(0xF5FFFFFF), Color(0xEDEBF0F8)))
                else Brush.verticalGradient(listOf(Color(0x44FFFFFF), Color(0x33FFFFFF)))
            )
            .border(1.dp, Color(0x66FFFFFF), shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (enabled) Color(0xFF050810) else Color(0x66050810),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SecondaryButton(text: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(R4)
    Box(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Glass1)
            .border(1.dp, B1, shape)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = T2, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun GlassCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(R3))
            .background(Glass1)
            .border(1.dp, B1, RoundedCornerShape(R3))
            .padding(16.dp)
    ) { content() }
}

@Composable
private fun SelectRow(
    label: String,
    selected: Boolean,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(R2))
            .background(if (selected) GlassSel else Glass1)
            .border(1.dp, if (selected) BSel else B1, RoundedCornerShape(R2))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, color = T1, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            if (subtitle != null) {
                Text(subtitle, color = T3, fontSize = 12.sp)
            }
        }
        Box(
            Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(if (selected) Moon else Color.Transparent)
                .border(1.5.dp, if (selected) Moon else Color(0x29FFFFFF), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (selected) Icon(Icons.Filled.Check, null, tint = Color(0xFF050810), modifier = Modifier.size(12.dp))
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, color = Color(0xFFD4A574), fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
}

@Composable
private fun StepChrome(
    title: String,
    subtitle: String? = null,
    canContinue: Boolean = true,
    validationHint: String? = null,
    continueLabel: String = "Continue",
    onContinue: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Text(
            title,
            color = T1,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = 32.sp,
            letterSpacing = (-0.5).sp
        )
        if (subtitle != null) {
            Spacer(Modifier.height(10.dp))
            Text(subtitle, color = T2, fontSize = 15.sp, lineHeight = 22.sp)
        }
        Spacer(Modifier.height(18.dp))
        Box(Modifier.weight(1f)) { content() }
        if (!canContinue && validationHint != null) {
            Hint(validationHint)
        }
        if (onContinue != null) {
            PrimaryButton(continueLabel, enabled = canContinue, onClick = onContinue)
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun WelcomeStep(onContinue: () -> Unit) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🌙", fontSize = 48.sp)
        Spacer(Modifier.height(20.dp))
        Text(
            "Make room for\nwhat matters.",
            color = T1,
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            lineHeight = 36.sp
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Suhood helps you protect Salah, quiet distractions, and build a gentle daily practice.",
            color = T2,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        Spacer(Modifier.height(36.dp))
        PrimaryButton("Get Started", onClick = onContinue)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun AboutStep(store: OnboardingProfileStore, onContinue: () -> Unit) {
    var age by remember { mutableStateOf(store.ageGroup) }
    var gender by remember { mutableStateOf(store.gender) }
    var life by remember { mutableStateOf(store.dailyLife) }
    var lang by remember { mutableStateOf(store.language) }
    val ages = listOf("Under 18" to "under18", "18–24" to "18-24", "25–34" to "25-34", "35–44" to "35-44", "45+" to "45+")
    val genders = listOf("Male" to "male", "Female" to "female", "Prefer not to say" to "prefer_not")
    val lives = listOf("Student" to "student", "Working" to "working", "Parent" to "parent", "Other" to "other")
    val langs = listOf("English", "Hindi", "Urdu", "Hinglish")
    val valid = OnboardingValidation.aboutValid(age, life)

    StepChrome(
        title = "A little about yourself?",
        subtitle = "Helps us pace recommendations. Gender is optional.",
        canContinue = valid,
        validationHint = "Select age group and daily life to continue.",
        onContinue = {
            store.ageGroup = age
            store.gender = gender
            store.dailyLife = life
            store.language = lang
            onContinue()
        }
    ) {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("AGE", color = T3, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
            ages.forEach { (label, id) ->
                SelectRow(label, age == id || age == label) { age = id }
            }
            Spacer(Modifier.height(8.dp))
            Text("GENDER (OPTIONAL)", color = T3, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
            genders.forEach { (label, id) ->
                SelectRow(label, gender == id || gender == label) { gender = id }
            }
            Spacer(Modifier.height(8.dp))
            Text("DAILY LIFE", color = T3, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
            lives.forEach { (label, id) ->
                SelectRow(label, life == id || life == label) { life = id }
            }
            Spacer(Modifier.height(8.dp))
            Text("LANGUAGE", color = T3, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
            langs.forEach { label ->
                SelectRow(label, lang == label) { lang = label }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun GoalsStep(store: OnboardingProfileStore, onContinue: () -> Unit) {
    val opts = listOf(
        Triple("salah", "Consistent Salah", "Show up for the five prayers more reliably"),
        Triple("quran", "More Quran", "A small daily reading habit"),
        Triple("dhikr", "Daily Dhikr", "Short remembrance throughout the day"),
        Triple("focus", "Reduce distractions", "Protect attention around Salah"),
        Triple("routine", "Daily routine", "One calm structure you can keep")
    )
    var selected by remember {
        mutableStateOf(
            store.goals.map { g ->
                when {
                    g in setOf("salah", "quran", "dhikr", "focus", "routine") -> g
                    "Salah" in g || "salah" in g.lowercase() -> "salah"
                    "Quran" in g || "quran" in g.lowercase() -> "quran"
                    "Dhikr" in g || "dhikr" in g.lowercase() -> "dhikr"
                    "distract" in g.lowercase() || "phone" in g.lowercase() || "focus" in g.lowercase() -> "focus"
                    "routine" in g.lowercase() -> "routine"
                    else -> g
                }
            }.toSet()
        )
    }
    val valid = OnboardingValidation.goalsValid(selected)

    StepChrome(
        title = "What matters most right now?",
        subtitle = "Pick at least one. This drives your personal plan.",
        canContinue = valid,
        validationHint = "Select at least one goal to continue.",
        onContinue = {
            store.goals = selected
            onContinue()
        }
    ) {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            opts.forEach { (id, title, sub) ->
                SelectRow(title, id in selected, sub) {
                    selected = if (id in selected) selected - id else selected + id
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun HabitsStep(store: OnboardingProfileStore, onContinue: () -> Unit) {
    var phone by remember { mutableStateOf(store.phoneUsage) }
    var dist by remember { mutableStateOf(store.socialUsage) }
    val phones = listOf("1–2 hours" to "1-2h", "2–4 hours" to "2-4h", "4–6 hours" to "4-6h", "6+ hours" to "6h+")
    val dists = listOf("Morning" to "morning", "Afternoon" to "afternoon", "Evening" to "evening", "Late night" to "night")
    val valid = OnboardingValidation.habitsValid(phone, dist)

    StepChrome(
        title = "How do you use your phone?",
        subtitle = "Estimates shape distraction strategy. Usage Access can refine later.",
        canContinue = valid,
        validationHint = "Select daily use and when distractions are strongest.",
        onContinue = {
            store.phoneUsage = phone
            store.socialUsage = dist
            onContinue()
        }
    ) {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("DAILY SCREEN TIME", color = T3, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
            phones.forEach { (label, id) ->
                SelectRow(label, phone == id || phone == label) { phone = id }
            }
            Spacer(Modifier.height(8.dp))
            Text("DISTRACTIONS STRONGEST", color = T3, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
            dists.forEach { (label, id) ->
                SelectRow(label, dist == id || dist == label) { dist = id }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun EducationStep(onContinue: () -> Unit) {
    StepChrome(
        title = "Your attention is valuable.",
        subtitle = "A few minutes of protection around Salah can change the whole day.",
        onContinue = onContinue,
        continueLabel = "I understand"
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            GlassCard {
                Text("Salah windows", color = T1, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text("When prayer time arrives, selected apps can pause so you return to Allah first.", color = T2, fontSize = 13.sp)
            }
            GlassCard {
                Text("Gentle, not punishing", color = T1, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text("Limits are timed. Access returns automatically. You stay in control of permissions.", color = T2, fontSize = 13.sp)
            }
            GlassCard {
                Text("Small habits compound", color = T1, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text("Your plan will suggest realistic first steps — not an overwhelming overhaul.", color = T2, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun AppsStep(store: OnboardingProfileStore, onContinue: () -> Unit) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var apps by remember { mutableStateOf<List<InstalledAppInfo>>(emptyList()) }
    var selected by remember {
        mutableStateOf(FocusSessionController.get(context).getBlockedApps().toSet())
    }
    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) {
            runCatching { InstalledAppsRepository.loadLauncherApps(context) }.getOrDefault(emptyList())
        }
    }
    val filtered = remember(apps, query) {
        if (query.isBlank()) apps
        else apps.filter {
            it.label.contains(query, true) || it.packageName.contains(query, true)
        }
    }

    StepChrome(
        title = "Which apps pull your focus?",
        subtitle = "Optional. Selected apps can be limited during Salah after permissions.",
        continueLabel = if (selected.isEmpty()) "Continue without apps" else "Continue · ${selected.size} selected",
        onContinue = {
            FocusSessionController.get(context).setBlockedApps(selected)
            onContinue()
        }
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(R2))
                    .background(Glass1)
                    .border(1.dp, B1, RoundedCornerShape(R2))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Search, null, tint = T3, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                androidx.compose.foundation.text.BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(color = T1, fontSize = 14.sp),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (query.isEmpty()) Text("Search apps", color = T3, fontSize = 14.sp)
                        inner()
                    }
                )
            }
            if (selected.isNotEmpty()) {
                Text(
                    "${selected.size} app${if (selected.size > 1) "s" else ""} selected",
                    color = Moon,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filtered.take(80).forEach { app ->
                    val on = app.packageName in selected
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(R2))
                            .background(if (on) GlassSel else Glass1)
                            .border(1.dp, if (on) BSel else B1, RoundedCornerShape(R2))
                            .clickable {
                                selected = if (on) selected - app.packageName else selected + app.packageName
                            }
                            .padding(11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val bmp = remember(app.packageName) {
                            runCatching {
                                drawableToBitmap(context.packageManager.getApplicationIcon(app.packageName))
                            }.getOrNull()
                        }
                        if (bmp != null) {
                            Image(bmp.asImageBitmap(), null, Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)))
                        } else {
                            Box(Modifier.size(38.dp).background(Color(0xFF1A2A3A), RoundedCornerShape(10.dp)))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(app.label, color = T1, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text(app.packageName, color = T3, fontSize = 11.sp, maxLines = 1)
                        }
                        Box(
                            Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(if (on) Moon else Color.Transparent)
                                .border(1.5.dp, if (on) Moon else Color(0x29FFFFFF), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (on) Icon(Icons.Filled.Check, null, tint = Color(0xFF050810), modifier = Modifier.size(12.dp))
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun SalahStep(store: OnboardingProfileStore, onContinue: () -> Unit) {
    var consistency by remember { mutableStateOf(store.consistency) }
    var prayer by remember { mutableStateOf(store.hardestPrayer) }
    var style by remember { mutableStateOf(store.reminderStyle.ifBlank { "Gentle" }) }
    val cons = listOf(
        "All five on time" to "all5",
        "Most days" to "most",
        "Working on it" to "working",
        "Just starting" to "starting"
    )
    val prayers = listOf("Fajr" to "fajr", "Dhuhr" to "dhuhr", "Asr" to "asr", "Maghrib" to "maghrib", "Isha" to "isha")
    val styles = listOf("Gentle", "Balanced", "Firm")
    val valid = OnboardingValidation.salahValid(consistency.ifBlank { "working" }, prayer)

    StepChrome(
        title = "Your prayer routine",
        subtitle = "We tailor Salah support — never invent prayer times.",
        canContinue = valid,
        validationHint = "Select consistency and your hardest prayer.",
        onContinue = {
            store.consistency = consistency
            store.hardestPrayer = prayer
            store.reminderStyle = style
            onContinue()
        }
    ) {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("CONSISTENCY", color = T3, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
            cons.forEach { (label, id) ->
                SelectRow(label, consistency == id || consistency == label) { consistency = id }
            }
            Spacer(Modifier.height(8.dp))
            Text("HARDEST PRAYER", color = T3, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
            prayers.forEach { (label, id) ->
                SelectRow(label, prayer.equals(id, true) || prayer.equals(label, true)) {
                    prayer = label
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("REMINDER STYLE", color = T3, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
            styles.forEach { label ->
                SelectRow(label, style.equals(label, true)) { style = label }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PracticeStep(
    store: OnboardingProfileStore,
    settingsViewModel: SettingsViewModel,
    onContinue: () -> Unit
) {
    var practice by remember { mutableStateOf(store.practice.ifBlank { "both" }) }
    var minutes by remember { mutableIntStateOf(store.dailyMinutes.coerceIn(5, 30)) }
    val practices = listOf(
        Triple("quran", "Quran", "Reading only"),
        Triple("dhikr", "Dhikr", "Remembrance only"),
        Triple("both", "Both", "Quran and Dhikr")
    )
    val times = listOf(5, 10, 15, 20)
    val valid = OnboardingValidation.practiceValid(practice, minutes)

    StepChrome(
        title = "Set your daily goal",
        subtitle = "Small targets you can keep. Change anytime in Settings.",
        canContinue = valid,
        validationHint = "Choose a practice and daily time.",
        onContinue = {
            store.practice = practice
            store.dailyMinutes = minutes
            if (practice == "quran" || practice == "both") store.quranMinutes = minutes
            if (practice == "dhikr" || practice == "both") store.dhikrMinutes = minOf(minutes, 15)
            runCatching { settingsViewModel.setQuranCompanion(store.quranMinutes, 9, 0, store.quranReminder) }
            onContinue()
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("PRACTICE", color = T3, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
            practices.forEach { (id, title, sub) ->
                SelectRow(title, practice == id, sub) { practice = id }
            }
            Spacer(Modifier.height(8.dp))
            Text("DAILY TIME", color = T3, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
            times.forEach { m ->
                SelectRow("$m minutes", minutes == m) { minutes = m }
            }
        }
    }
}

@Composable
private fun PlanStep(store: OnboardingProfileStore, onContinue: () -> Unit) {
    val context = LocalContext.current
    val plan = remember(
        store.goals, store.hardestPrayer, store.consistency, store.reminderStyle,
        store.practice, store.dailyMinutes, store.phoneUsage, store.socialUsage
    ) {
        val apps = FocusSessionController.get(context).getBlockedApps()
        val p = PersonalizedPlanEngine.generate(store.toAnswers(apps.toList()))
        store.savePlan(p)
        p
    }

    StepChrome(
        title = "Here's what we've built together",
        subtitle = "Generated from your answers — not a generic template.",
        continueLabel = "Looks good",
        onContinue = onContinue
    ) {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            GlassCard {
                Text(plan.mainFocus, color = T1, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Spacer(Modifier.height(6.dp))
                Text(plan.mainFocusReason, color = T2, fontSize = 12.sp)
            }
            Text("FIRST-DAY ACTIONS", color = T3, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
            plan.firstDayActions.forEach { a ->
                GlassCard {
                    Text(a.title, color = T1, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(4.dp))
                    Text(a.detail, color = T2, fontSize = 12.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(a.reason, color = T3, fontSize = 11.sp)
                }
            }
            Text("SALAH SUPPORT", color = T3, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
            plan.salahSupport.forEach { Text("• $it", color = T2, fontSize = 13.sp) }
            Text("DISTRACTION STRATEGY", color = T3, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
            plan.distractionStrategy.forEach { Text("• $it", color = T2, fontSize = 13.sp) }
            Text("WEEKLY PROGRESSION", color = T3, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
            plan.weeklyProgression.forEach { Text("• $it", color = T2, fontSize = 13.sp) }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun PermissionsStep(onContinue: () -> Unit) {
    val context = LocalContext.current
    var notifOk by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var usageOk by remember { mutableStateOf(hasUsageAccess(context)) }
    var a11yOk by remember { mutableStateOf(FocusAccessibilityService.isEnabled(context)) }
    var overlayOk by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)
        )
    }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { notifOk = it }

    // Refresh when returning
    LaunchedEffect(Unit) {
        usageOk = hasUsageAccess(context)
        a11yOk = FocusAccessibilityService.isEnabled(context)
    }

    StepChrome(
        title = "A few permissions to make it work",
        subtitle = "We only ask for what Salah lock and reminders need. Nothing is enabled silently.",
        continueLabel = "Continue",
        onContinue = onContinue
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PermCard(
                title = "Notifications",
                body = "Salah reminders and lock countdown",
                granted = notifOk,
                action = {
                    if (Build.VERSION.SDK_INT >= 33) {
                        notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else notifOk = true
                }
            )
            PermCard(
                title = "Usage Access",
                body = "Detect which app is open during Salah lock",
                granted = usageOk,
                action = {
                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            )
            PermCard(
                title = "Accessibility",
                body = "Show lock overlay and return you to Home when a blocked app opens",
                granted = a11yOk,
                action = { AppBlockHelper.openAccessibilitySettings(context) }
            )
            PermCard(
                title = "Display over other apps",
                body = "Backup for mini-games and lock UI if Accessibility is delayed",
                granted = overlayOk,
                action = {
                    runCatching {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                android.net.Uri.parse("package:${context.packageName}")
                            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                    overlayOk = Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                        Settings.canDrawOverlays(context)
                }
            )
            Text(
                "Blocking needs Accessibility + Usage Access. Overlay is optional backup. Enable anytime in Settings.",
                color = T3,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun PermCard(title: String, body: String, granted: Boolean, action: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(R2))
            .background(Glass1)
            .border(1.dp, B1, RoundedCornerShape(R2))
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = T1, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(body, color = T3, fontSize = 12.sp)
        }
        Text(
            if (granted) "On" else "Enable",
            color = if (granted) Color(0xFF9FE8C8) else Moon,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .clickable(enabled = !granted, onClick = action)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun PreviewStep(store: OnboardingProfileStore, onContinue: () -> Unit) {
    val plan = store.loadPlan()
    StepChrome(
        title = "See what to expect",
        subtitle = "A calm preview of how Suhood will support you.",
        continueLabel = "Everything looks great",
        onContinue = onContinue
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            GlassCard {
                Text("Your focus", color = T3, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(plan?.mainFocus ?: "Build a steadier daily practice", color = T1, fontSize = 14.sp)
            }
            GlassCard {
                Text("Salah lock", color = T3, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "When Salah begins, selected apps pause for about 10 minutes with a live countdown.",
                    color = T2,
                    fontSize = 13.sp
                )
            }
            GlassCard {
                Text("First day", color = T3, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                plan?.firstDayActions?.take(2)?.forEach {
                    Text("• ${it.title}", color = T2, fontSize = 13.sp)
                } ?: Text("• Pray your next Salah with intention", color = T2, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun CompleteStep(store: OnboardingProfileStore, onEnter: () -> Unit) {
    val plan = store.loadPlan()
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(16)
            tick++
        }
    }
    val particles = remember {
        List(48) { i ->
            ConfettiP(
                x = (i * 37 % 100) / 100f,
                speed = 0.004f + (i % 7) * 0.0012f,
                color = listOf(Moon, Color(0xFFC7D4E0), Color(0xFF9FE8C8), Color(0xFFE8D5A3))[i % 4],
                size = 3f + (i % 5),
                phase = (i * 13 % 100) / 100f
            )
        }
    }
    Box(Modifier.fillMaxSize()) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val h = size.height
            val w = size.width
            particles.forEach { p ->
                val y = ((tick * p.speed + p.phase) % 1.25f) * h
                val x = p.x * w
                drawCircle(color = p.color, radius = p.size, center = androidx.compose.ui.geometry.Offset(x, y))
            }
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("🌙", fontSize = 42.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                "A fresh start,",
                color = T1,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            Text(
                "one prayer at a time.",
                color = T1,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Your plan is ready. Suhood will walk with you — gently, at your pace.",
                color = T2,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            Spacer(Modifier.height(18.dp))
            GlassCard {
                Text(
                    plan?.mainFocus ?: "Your personal plan is saved on this device.",
                    color = T1,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Goals: ${store.goals.joinToString()}",
                    color = T2,
                    fontSize = 12.sp
                )
                Text(
                    "Practice: ${store.practice} · ${store.dailyMinutes} min",
                    color = T2,
                    fontSize = 12.sp
                )
            }
            Spacer(Modifier.height(28.dp))
            PrimaryButton("Enter Suhood", onClick = onEnter)
            Spacer(Modifier.height(16.dp))
        }
    }
}

private data class ConfettiP(
    val x: Float,
    val speed: Float,
    val color: Color,
    val size: Float,
    val phase: Float
)


private fun hasUsageAccess(context: Context): Boolean {
    return try {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow("android:get_usage_stats", Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow("android:get_usage_stats", Process.myUid(), context.packageName)
        }
        mode == AppOpsManager.MODE_ALLOWED
    } catch (_: Exception) {
        false
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

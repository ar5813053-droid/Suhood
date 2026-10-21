package com.suhood.feature.journey

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suhood.app.SuhoodApplication
import com.suhood.core.database.HabitCompletionEntity
import com.suhood.core.database.HabitEntity
import com.suhood.core.designsystem.components.GlassCard
import com.suhood.core.designsystem.theme.SuhoodSpacing
import com.suhood.core.designsystem.theme.SuhoodTypography
import com.suhood.core.designsystem.theme.suhoodColors
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object JourneyScreens {

    private data class Template(
        val name: String,
        val category: String,
        val target: Int,
        val icon: ImageVector
    )

    private val templates = listOf(
        Template("Morning Salah on time", "salah", 1, Icons.Filled.Mosque),
        Template("Read Quran pages", "quran", 2, Icons.Filled.MenuBook),
        Template("Hifz revision", "hifz", 1, Icons.Filled.MenuBook),
        Template("Dhikr / Tasbeeh", "dhikr", 33, Icons.Filled.SelfImprovement),
        Template("Morning adhkar", "dhikr", 1, Icons.Filled.WbSunny),
        Template("Evening adhkar", "dhikr", 1, Icons.Filled.NightsStay),
        Template("Custom habit", "custom", 1, Icons.Filled.Add)
    )

    @Composable
    fun Home(
        onBack: () -> Unit,
        onNavigate: (String) -> Unit,
        bottomContentPadding: Dp = 0.dp
    ) {
        val colors = suhoodColors
        val context = LocalContext.current
        val dao = remember { (context.applicationContext as SuhoodApplication).database.dao() }
        val scope = rememberCoroutineScope()
        val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
        val habits by dao.observeHabits().collectAsState(initial = emptyList())
        val completions by dao.observeHabitCompletionsForDate(today).collectAsState(initial = emptyList())
        val doneIds = remember(completions) { completions.map { it.habitId }.toSet() }
        var showSetup by remember { mutableStateOf(false) }

        if (showSetup) {
            Setup(onBack = { showSetup = false }, onSaved = { showSetup = false })
            return
        }

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(bottom = bottomContentPadding)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(colors.surface)
                        .border(1.dp, colors.borderSubtle, CircleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = colors.textPrimary, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(14.dp))
                Text(
                    "My Journey",
                    style = SuhoodTypography.headlineSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
                    color = colors.textPrimary
                )
            }

            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    "Build good habits",
                    style = SuhoodTypography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Track Salah, Quran, Dhikr and personal goals. Missed days stay in history — no guilt messaging.",
                    style = SuhoodTypography.bodySmall,
                    color = colors.textMuted,
                    lineHeight = 18.sp
                )
                Spacer(Modifier.height(18.dp))

                val completed = habits.count { it.enabled && it.id in doneIds }
                val total = habits.count { it.enabled }
                val progress = if (total == 0) 0f else completed.toFloat() / total

                GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProgressRing(progress = progress, label = "$completed", sub = "OF $total")
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("Today", style = SuhoodTypography.labelMedium, color = colors.textMuted)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                if (total == 0) "Add your first habit"
                                else "$completed / $total habits completed",
                                style = SuhoodTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = colors.textPrimary
                            )
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))
                Text("Today's habits", style = SuhoodTypography.labelLarge, color = colors.textMuted)
                Spacer(Modifier.height(10.dp))

                if (habits.isEmpty()) {
                    GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(20.dp)) {
                        Text("No habits yet", color = colors.textPrimary, style = SuhoodTypography.titleMedium)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Start with Salah, Quran, Hifz, Dhikr or a custom goal.",
                            color = colors.textMuted,
                            style = SuhoodTypography.bodySmall
                        )
                    }
                } else {
                    habits.filter { it.enabled }.forEach { h ->
                        val done = h.id in doneIds
                        GlassCard(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            onClick = {
                                if (!done) {
                                    scope.launch {
                                        dao.upsertHabitCompletion(
                                            HabitCompletionEntity(
                                                id = "${h.id}|$today",
                                                habitId = h.id,
                                                dateIso = today,
                                                count = h.targetCount
                                            )
                                        )
                                    }
                                }
                            },
                            selected = done,
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (done) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                                    null,
                                    tint = if (done) colors.accentLavender else colors.textMuted,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(h.name, style = SuhoodTypography.titleMedium, color = colors.textPrimary)
                                    Text(
                                        "${h.category} · target ${h.targetCount}/${h.frequency}",
                                        style = SuhoodTypography.bodySmall,
                                        color = colors.textMuted
                                    )
                                }
                                if (done) {
                                    Text("Done", color = colors.accentLavender, style = SuhoodTypography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                GradientCta(text = "Add habit", onClick = { showSetup = true })
            }
        }
    }

    @Composable
    fun Setup(onBack: () -> Unit, onSaved: () -> Unit) {
        val colors = suhoodColors
        val context = LocalContext.current
        val dao = remember { (context.applicationContext as SuhoodApplication).database.dao() }
        val scope = rememberCoroutineScope()
        var selected by remember { mutableIntStateOf(0) }
        var customName by remember { mutableStateOf(templates[0].name) }
        var target by remember { mutableIntStateOf(1) }

        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(colors.surface)
                        .border(1.dp, colors.borderSubtle, CircleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = colors.textPrimary, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(14.dp))
                Text(
                    "New habit",
                    style = SuhoodTypography.headlineSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
                    color = colors.textPrimary
                )
            }

            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 100.dp)
            ) {
                Text("Templates", style = SuhoodTypography.labelLarge, color = colors.textMuted)
                Spacer(Modifier.height(10.dp))

                templates.forEachIndexed { i, tpl ->
                    val isSel = selected == i
                    val scale by animateFloatAsState(
                        if (isSel) 1f else 0.99f,
                        spring(stiffness = Spring.StiffnessMediumLow),
                        label = "tpl$i"
                    )
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .scale(scale)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isSel) Brush.verticalGradient(
                                    listOf(colors.accentLavender.copy(alpha = 0.16f), Color.Transparent)
                                ) else Brush.verticalGradient(listOf(colors.surface, colors.surface))
                            )
                            .border(
                                1.dp,
                                if (isSel) colors.accentLavender else colors.borderSubtle,
                                RoundedCornerShape(20.dp)
                            )
                            .clickable {
                                selected = i
                                target = tpl.target.coerceIn(1, 99)
                                if (tpl.category != "custom") customName = tpl.name
                                else if (customName in templates.map { it.name }) customName = ""
                            }
                            .padding(15.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .background(if (isSel) colors.accentLavender else colors.surfaceElevated),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                tpl.icon,
                                null,
                                tint = if (isSel) Color.White else colors.accentLavender,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(tpl.name.ifBlank { "Custom habit" }, style = SuhoodTypography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = colors.textPrimary)
                            Text(tpl.category, style = SuhoodTypography.labelSmall, color = colors.textMuted)
                        }
                        Box(
                            Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(if (isSel) colors.accentLavender else Color.Transparent)
                                .border(1.5.dp, if (isSel) colors.accentLavender else colors.borderSubtle, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSel) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(12.dp))
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }

                Spacer(Modifier.height(12.dp))
                Text("Name", style = SuhoodTypography.labelLarge, color = colors.textMuted)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = customName,
                    onValueChange = { customName = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("e.g. Morning Salah on time", color = colors.textMuted) },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.accentLavender,
                        unfocusedBorderColor = colors.borderSubtle,
                        focusedContainerColor = colors.surface,
                        unfocusedContainerColor = colors.surface,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        cursorColor = colors.accentLavender
                    )
                )

                Spacer(Modifier.height(18.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.surface)
                        .border(1.dp, colors.borderSubtle, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Daily target", style = SuhoodTypography.titleSmall.copy(fontWeight = FontWeight.Medium), color = colors.textPrimary)
                        Text("How many times a day", style = SuhoodTypography.labelSmall, color = colors.textMuted)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StepperBtn(label = "−") { target = (target - 1).coerceAtLeast(1) }
                        Spacer(Modifier.width(14.dp))
                        Text(
                            "$target",
                            style = SuhoodTypography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.textPrimary,
                            modifier = Modifier.widthIn(min = 24.dp)
                        )
                        Spacer(Modifier.width(14.dp))
                        StepperBtn(label = "+") { target = (target + 1).coerceAtMost(99) }
                    }
                }
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, colors.background.copy(alpha = 0.95f), colors.background)))
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                GradientCta(
                    text = "Create habit",
                    onClick = {
                        val name = customName.ifBlank { templates[selected].name.ifBlank { "Custom habit" } }
                        val cat = templates[selected].category
                        scope.launch {
                            dao.upsertHabit(
                                HabitEntity(
                                    id = UUID.randomUUID().toString(),
                                    name = name,
                                    category = cat,
                                    targetCount = target
                                )
                            )
                            onSaved()
                        }
                    }
                )
            }
        }
    }

    @Composable
    private fun StepperBtn(label: String, onClick: () -> Unit) {
        val colors = suhoodColors
        Box(
            Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(colors.surfaceElevated)
                .border(1.dp, colors.borderSubtle, CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Text(label, color = colors.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Medium)
        }
    }

    @Composable
    private fun GradientCta(text: String, onClick: () -> Unit) {
        val colors = suhoodColors
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.linearGradient(listOf(colors.accentLavender, Color(0xFF9A82F5))))
                .clickable(onClick = onClick)
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text, color = Color.White, style = SuhoodTypography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
        }
    }

    @Composable
    private fun ProgressRing(progress: Float, label: String, sub: String) {
        val colors = suhoodColors
        val animated by animateFloatAsState(progress.coerceIn(0f, 1f), tween(900), label = "ring")
        Box(Modifier.size(84.dp), contentAlignment = Alignment.Center) {
            androidx.compose.foundation.Canvas(Modifier.size(84.dp)) {
                val stroke = 8.dp.toPx()
                drawCircle(color = colors.surfaceElevated, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
                drawArc(
                    color = colors.accentLavender,
                    startAngle = -90f,
                    sweepAngle = 360f * animated,
                    useCenter = false,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(label, style = SuhoodTypography.titleLarge.copy(fontWeight = FontWeight.SemiBold), color = colors.textPrimary)
                Text(sub, style = SuhoodTypography.labelSmall.copy(fontSize = 9.sp), color = colors.textMuted)
            }
        }
    }
}

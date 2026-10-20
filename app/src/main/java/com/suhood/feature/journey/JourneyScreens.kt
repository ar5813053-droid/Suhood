package com.suhood.feature.journey

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.suhood.app.SuhoodApplication
import com.suhood.core.database.HabitCompletionEntity
import com.suhood.core.database.HabitEntity
import com.suhood.core.designsystem.components.GlassCard
import com.suhood.core.designsystem.components.GlassButton
import com.suhood.core.designsystem.components.ScreenHeader
import com.suhood.core.designsystem.theme.SuhoodSpacing
import com.suhood.core.designsystem.theme.SuhoodTypography
import com.suhood.core.designsystem.theme.suhoodColors
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object JourneyScreens {

    private val templates = listOf(
        Triple("Morning Salah on time", "salah", 1),
        Triple("Read Quran pages", "quran", 2),
        Triple("Hifz revision", "hifz", 1),
        Triple("Dhikr / Tasbeeh", "dhikr", 33),
        Triple("Morning adhkar", "dhikr", 1),
        Triple("Evening adhkar", "dhikr", 1),
        Triple("Custom habit", "custom", 1)
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
        val today = remember {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        }
        val habits by dao.observeHabits().collectAsState(initial = emptyList())
        val completions by dao.observeHabitCompletionsForDate(today).collectAsState(initial = emptyList())
        val doneIds = remember(completions) { completions.map { it.habitId }.toSet() }
        var showSetup by remember { mutableStateOf(false) }

        if (showSetup) {
            Setup(
                onBack = { showSetup = false },
                onSaved = { showSetup = false }
            )
            return
        }

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(bottom = bottomContentPadding)
        ) {
            ScreenHeader(title = "My Journey", onBack = onBack)
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = SuhoodSpacing.screenHorizontal)
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    "Build good habits",
                    style = SuhoodTypography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Track Salah, Quran, Dhikr and personal goals. Missed days stay in history — no guilt messaging.",
                    style = SuhoodTypography.bodySmall,
                    color = colors.textMuted
                )
                Spacer(Modifier.height(16.dp))

                val completed = habits.count { it.enabled && it.id in doneIds }
                val total = habits.count { it.enabled }
                GlassCard(Modifier.fillMaxWidth()) {
                    Text("Today", style = SuhoodTypography.labelMedium, color = colors.textMuted)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (total == 0) "Add your first habit"
                        else "$completed / $total habits completed",
                        style = SuhoodTypography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )
                }

                Spacer(Modifier.height(16.dp))
                Text("Today’s habits", style = SuhoodTypography.titleSmall, color = colors.textMuted)
                Spacer(Modifier.height(8.dp))

                if (habits.isEmpty()) {
                    GlassCard(Modifier.fillMaxWidth()) {
                        Text("No habits yet", color = colors.textPrimary, style = SuhoodTypography.titleMedium)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Start with Salah, Quran, Hifz, Dhikr or a custom goal.",
                            color = colors.textMuted,
                            style = SuhoodTypography.bodySmall
                        )
                        Spacer(Modifier.height(12.dp))
                        GlassButton(text = "Create habit", onClick = { showSetup = true })
                    }
                } else {
                    habits.filter { it.enabled }.forEach { h ->
                        val done = h.id in doneIds
                        GlassCard(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            onClick = {
                                scope.launch {
                                    if (done) return@launch
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
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (done) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                                    contentDescription = null,
                                    tint = if (done) colors.accentMint else colors.textMuted
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
                                    Text("Done", color = colors.accentMint, style = SuhoodTypography.labelMedium)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    GlassButton(text = "Add habit", onClick = { showSetup = true })
                }
            }
        }
    }

    @Composable
    fun Setup(onBack: () -> Unit, onSaved: () -> Unit) {
        val colors = suhoodColors
        val context = LocalContext.current
        val dao = remember { (context.applicationContext as SuhoodApplication).database.dao() }
        val scope = rememberCoroutineScope()
        var selected by remember { mutableStateOf(0) }
        var customName by remember { mutableStateOf("") }
        var target by remember { mutableStateOf("1") }

        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            ScreenHeader(title = "New habit", onBack = onBack)
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = SuhoodSpacing.screenHorizontal)
            ) {
                Text("Templates", style = SuhoodTypography.titleSmall, color = colors.textMuted)
                Spacer(Modifier.height(8.dp))
                templates.forEachIndexed { i, (name, cat, tgt) ->
                    GlassCard(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        onClick = {
                            selected = i
                            target = tgt.toString()
                            if (cat != "custom") customName = name
                        }
                    ) {
                        Text(
                            name,
                            color = if (selected == i) colors.accentLavender else colors.textPrimary,
                            style = SuhoodTypography.titleMedium
                        )
                        Text(cat, color = colors.textMuted, style = SuhoodTypography.bodySmall)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Name", style = SuhoodTypography.labelMedium, color = colors.textMuted)
                androidx.compose.material3.OutlinedTextField(
                    value = customName,
                    onValueChange = { customName = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                Text("Daily target", style = SuhoodTypography.labelMedium, color = colors.textMuted)
                androidx.compose.material3.OutlinedTextField(
                    value = target,
                    onValueChange = { target = it.filter { c -> c.isDigit() }.take(4) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(16.dp))
                GlassButton(
                    text = "Save habit",
                    onClick = {
                        val name = customName.ifBlank { templates[selected].first }
                        val cat = templates[selected].second
                        val tgt = target.toIntOrNull() ?: 1
                        scope.launch {
                            dao.upsertHabit(
                                HabitEntity(
                                    id = UUID.randomUUID().toString(),
                                    name = name,
                                    category = cat,
                                    targetCount = tgt.coerceAtLeast(1)
                                )
                            )
                            onSaved()
                        }
                    }
                )
            }
        }
    }
}

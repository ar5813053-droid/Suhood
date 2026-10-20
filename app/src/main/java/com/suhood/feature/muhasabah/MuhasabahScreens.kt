package com.suhood.feature.muhasabah

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.suhood.app.SuhoodApplication
import com.suhood.core.database.MuhasabahEntryEntity
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

object MuhasabahScreens {

    private val prompts = listOf(
        "What did I learn from today's prayers and how did they impact my day?",
        "Where did I feel closest to Allah today?",
        "What distracted me from remembrance today?",
        "One blessing I must thank Allah for today."
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
        val entries by dao.observeMuhasabah().collectAsState(initial = emptyList())
        val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
        var writing by remember { mutableStateOf(false) }
        var historyMode by remember { mutableStateOf(false) }

        if (writing) {
            Editor(
                dateIso = today,
                onBack = { writing = false },
                onSaved = { writing = false }
            )
            return
        }

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(bottom = bottomContentPadding)
        ) {
            ScreenHeader(title = "Muhasabah", onBack = onBack)
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = SuhoodSpacing.screenHorizontal)
                    .padding(bottom = 24.dp)
            ) {
                // Segment Write | History
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassCard(
                        modifier = Modifier.weight(1f),
                        onClick = { writing = true; historyMode = false }
                    ) {
                        Text(
                            "Write",
                            style = SuhoodTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = colors.textPrimary
                        )
                    }
                    GlassCard(
                        modifier = Modifier.weight(1f),
                        onClick = { historyMode = true }
                    ) {
                        Text(
                            "History",
                            style = SuhoodTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = colors.textPrimary
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                GlassCard(Modifier.fillMaxWidth(), onClick = { writing = true }) {
                    Text("💡", style = SuhoodTypography.titleLarge)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        prompts.random(),
                        style = SuhoodTypography.bodyMedium,
                        color = colors.textSecondary
                    )
                }
                Spacer(Modifier.height(16.dp))
                if (!historyMode) {
                    Text("Today", style = SuhoodTypography.titleSmall, color = colors.textMuted)
                    Spacer(Modifier.height(8.dp))
                    val todayEntry = entries.find { it.dateIso == today }
                    if (todayEntry == null) {
                        GlassCard(Modifier.fillMaxWidth(), onClick = { writing = true }) {
                            Text("Start today’s reflection", color = colors.textPrimary)
                            Text(
                                "Write reflections, gratitude, lessons and intentions.",
                                color = colors.textMuted,
                                style = SuhoodTypography.bodySmall
                            )
                        }
                    } else {
                        GlassCard(Modifier.fillMaxWidth(), onClick = { writing = true }) {
                            Text("Today’s entry saved", color = colors.accentMint)
                            if (todayEntry.reflection.isNotBlank()) {
                                Text(
                                    todayEntry.reflection.take(120),
                                    color = colors.textSecondary,
                                    style = SuhoodTypography.bodySmall
                                )
                            }
                        }
                    }
                } else {
                    Text("Previous entries", style = SuhoodTypography.titleSmall, color = colors.textMuted)
                    Spacer(Modifier.height(8.dp))
                    if (entries.isEmpty()) {
                        Text("No entries yet. Private and stored only on this device.", color = colors.textMuted)
                    } else {
                        entries.forEach { e ->
                            GlassCard(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Text(e.dateIso, color = colors.accentLavender, style = SuhoodTypography.labelLarge)
                                if (e.reflection.isNotBlank()) {
                                    Text(e.reflection.take(140), color = colors.textPrimary, style = SuhoodTypography.bodySmall)
                                }
                                if (e.gratitude.isNotBlank()) {
                                    Text("Gratitude: ${e.gratitude.take(80)}", color = colors.textMuted, style = SuhoodTypography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun Editor(dateIso: String, onBack: () -> Unit, onSaved: () -> Unit) {
        val colors = suhoodColors
        val context = LocalContext.current
        val dao = remember { (context.applicationContext as SuhoodApplication).database.dao() }
        val scope = rememberCoroutineScope()
        var reflection by remember { mutableStateOf("") }
        var gratitude by remember { mutableStateOf("") }
        var lessons by remember { mutableStateOf("") }
        var intention by remember { mutableStateOf("") }
        var status by remember { mutableStateOf("") }

        LaunchedEffect(dateIso) {
            dao.getMuhasabah(dateIso)?.let {
                reflection = it.reflection
                gratitude = it.gratitude
                lessons = it.lessons
                intention = it.intention
            }
        }

        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            ScreenHeader(title = "Journal", onBack = onBack)
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = SuhoodSpacing.screenHorizontal)
                    .padding(bottom = 24.dp)
            ) {
                Text(dateIso, style = SuhoodTypography.labelMedium, color = colors.textMuted)
                if (status.isNotBlank()) {
                    Text(status, color = colors.accentMint, style = SuhoodTypography.labelSmall)
                }
                Spacer(Modifier.height(12.dp))
                Text("Reflection", style = SuhoodTypography.titleSmall, color = colors.textPrimary)
                OutlinedTextField(
                    value = reflection,
                    onValueChange = { reflection = it },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                    placeholder = { Text("Write your daily reflections, thoughts, and spiritual journey here…") }
                )
                Spacer(Modifier.height(12.dp))
                Text("Gratitude", style = SuhoodTypography.titleSmall, color = colors.textPrimary)
                OutlinedTextField(
                    value = gratitude,
                    onValueChange = { gratitude = it },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                    placeholder = { Text("Express your gratitude to Allah…") }
                )
                Spacer(Modifier.height(12.dp))
                Text("Lessons", style = SuhoodTypography.titleSmall, color = colors.textPrimary)
                OutlinedTextField(
                    value = lessons,
                    onValueChange = { lessons = it },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text("Tomorrow’s intention", style = SuhoodTypography.titleSmall, color = colors.textPrimary)
                OutlinedTextField(
                    value = intention,
                    onValueChange = { intention = it },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp)
                )
                Spacer(Modifier.height(16.dp))
                GlassButton(
                    text = "Save entry",
                    onClick = {
                        scope.launch {
                            dao.upsertMuhasabah(
                                MuhasabahEntryEntity(
                                    dateIso = dateIso,
                                    reflection = reflection,
                                    gratitude = gratitude,
                                    lessons = lessons,
                                    intention = intention
                                )
                            )
                            status = "Saved · private on this device"
                            onSaved()
                        }
                    }
                )
            }
        }
    }
}

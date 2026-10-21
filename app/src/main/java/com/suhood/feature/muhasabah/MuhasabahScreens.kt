package com.suhood.feature.muhasabah

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suhood.app.SuhoodApplication
import com.suhood.core.database.MuhasabahEntryEntity
import com.suhood.core.designsystem.components.GlassCard
import com.suhood.core.designsystem.theme.SuhoodSpacing
import com.suhood.core.designsystem.theme.SuhoodTypography
import com.suhood.core.designsystem.theme.suhoodColors
import kotlinx.coroutines.delay
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
            Editor(dateIso = today, onBack = { writing = false }, onSaved = { writing = false })
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
                    "Muhasabah",
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
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SegmentChip(
                        text = "Write",
                        active = !historyMode,
                        modifier = Modifier.weight(1f),
                        onClick = { writing = true; historyMode = false }
                    )
                    SegmentChip(
                        text = "History",
                        active = historyMode,
                        modifier = Modifier.weight(1f),
                        onClick = { historyMode = true }
                    )
                }
                Spacer(Modifier.height(14.dp))
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { writing = true },
                    contentPadding = PaddingValues(18.dp)
                ) {
                    Text(
                        prompts.random(),
                        style = SuhoodTypography.bodyMedium,
                        color = colors.textSecondary,
                        lineHeight = 22.sp
                    )
                }
                Spacer(Modifier.height(16.dp))
                if (!historyMode) {
                    Text("Today", style = SuhoodTypography.labelLarge, color = colors.textMuted)
                    Spacer(Modifier.height(8.dp))
                    val todayEntry = entries.find { it.dateIso == today }
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { writing = true },
                        contentPadding = PaddingValues(18.dp)
                    ) {
                        if (todayEntry == null) {
                            Text("Start today's reflection", color = colors.textPrimary, style = SuhoodTypography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Write reflections, gratitude, lessons and intentions.",
                                color = colors.textMuted,
                                style = SuhoodTypography.bodySmall
                            )
                        } else {
                            Text("Today's entry saved", color = colors.accentLavender, style = SuhoodTypography.titleSmall)
                            if (todayEntry.reflection.isNotBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    todayEntry.reflection.take(140),
                                    color = colors.textSecondary,
                                    style = SuhoodTypography.bodySmall
                                )
                            }
                        }
                    }
                } else {
                    Text("Previous entries", style = SuhoodTypography.labelLarge, color = colors.textMuted)
                    Spacer(Modifier.height(8.dp))
                    if (entries.isEmpty()) {
                        Text(
                            "No entries yet. Private and stored only on this device.",
                            color = colors.textMuted,
                            style = SuhoodTypography.bodySmall
                        )
                    } else {
                        entries.forEach { e ->
                            GlassCard(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                onClick = { /* open editor for date in future */ },
                                contentPadding = PaddingValues(16.dp)
                            ) {
                                Text(e.dateIso, color = colors.accentLavender, style = SuhoodTypography.labelLarge)
                                if (e.reflection.isNotBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(e.reflection.take(140), color = colors.textPrimary, style = SuhoodTypography.bodySmall)
                                }
                                if (e.gratitude.isNotBlank()) {
                                    Text(
                                        "Gratitude: ${e.gratitude.take(80)}",
                                        color = colors.textMuted,
                                        style = SuhoodTypography.bodySmall
                                    )
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
        var showToast by remember { mutableStateOf(false) }

        LaunchedEffect(dateIso) {
            dao.getMuhasabah(dateIso)?.let {
                reflection = it.reflection
                gratitude = it.gratitude
                lessons = it.lessons
                intention = it.intention
            }
        }

        Box(Modifier.fillMaxSize()) {
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
                        "Journal",
                        style = SuhoodTypography.headlineSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
                        color = colors.textPrimary
                    )
                    Spacer(Modifier.weight(1f))
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(colors.surface)
                            .border(1.dp, colors.borderSubtle, RoundedCornerShape(999.dp))
                            .padding(horizontal = 13.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.CalendarMonth, null, tint = colors.accentYellow, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(
                            formatDisplayDate(dateIso),
                            style = SuhoodTypography.labelMedium,
                            color = colors.textSecondary
                        )
                    }
                }

                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 100.dp)
                ) {
                    JournalField(
                        title = "Reflection",
                        subtitle = "How did today feel, spiritually?",
                        icon = Icons.Filled.EditNote,
                        value = reflection,
                        onValueChange = { reflection = it },
                        minHeight = 108.dp,
                        placeholder = "Write your daily reflections, thoughts, and spiritual journey here…"
                    )
                    JournalField(
                        title = "Gratitude",
                        subtitle = "What are you thankful for?",
                        icon = Icons.Filled.Favorite,
                        value = gratitude,
                        onValueChange = { gratitude = it },
                        minHeight = 74.dp,
                        placeholder = "Express your gratitude to Allah…"
                    )
                    JournalField(
                        title = "Lessons",
                        subtitle = "What did you learn today?",
                        icon = Icons.Filled.Lightbulb,
                        value = lessons,
                        onValueChange = { lessons = it },
                        minHeight = 74.dp,
                        placeholder = "A lesson, a reminder, an ayah that stayed with you…"
                    )
                    JournalField(
                        title = "Tomorrow's intention",
                        subtitle = "One thing to carry forward",
                        icon = Icons.Filled.MyLocation,
                        value = intention,
                        onValueChange = { intention = it },
                        minHeight = 74.dp,
                        placeholder = "Set an intention for tomorrow…"
                    )
                }

                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, colors.background.copy(alpha = 0.95f), colors.background)
                            )
                        )
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.linearGradient(listOf(colors.accentLavender, colors.strongAccent)))
                            .clickable {
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
                                    showToast = true
                                    delay(1600)
                                    showToast = false
                                    onSaved()
                                }
                            }
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Save, null, tint = Color.White, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Save entry",
                                color = Color.White,
                                style = SuhoodTypography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = showToast,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 120.dp)
            ) {
                Row(
                    Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(colors.surfaceElevated)
                        .border(1.dp, colors.borderSubtle, RoundedCornerShape(999.dp))
                        .padding(horizontal = 18.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Check, null, tint = colors.accentLavender, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Entry saved", style = SuhoodTypography.labelLarge, color = colors.textPrimary)
                }
            }
        }
    }

    @Composable
    private fun JournalField(
        title: String,
        subtitle: String,
        icon: ImageVector,
        value: String,
        onValueChange: (String) -> Unit,
        minHeight: Dp,
        placeholder: String
    ) {
        val colors = suhoodColors
        Column(Modifier.padding(top = 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(colors.accentLavender.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = colors.accentLavender, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(9.dp))
                Column {
                    Text(title, style = SuhoodTypography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = colors.textPrimary)
                    Text(subtitle, style = SuhoodTypography.labelSmall, color = colors.textMuted)
                }
            }
            Spacer(Modifier.height(9.dp))
            Box {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = minHeight),
                    placeholder = { Text(placeholder, color = colors.textMuted, style = SuhoodTypography.bodyMedium) },
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
                Text(
                    "${value.length}",
                    style = SuhoodTypography.labelSmall.copy(fontSize = 11.sp),
                    color = colors.textMuted,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                )
            }
        }
    }

    @Composable
    private fun SegmentChip(text: String, active: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
        val colors = suhoodColors
        Box(
            modifier
                .clip(RoundedCornerShape(16.dp))
                .background(if (active) colors.accentLavender.copy(alpha = 0.16f) else colors.surface)
                .border(1.dp, if (active) colors.accentLavender.copy(alpha = 0.4f) else colors.borderSubtle, RoundedCornerShape(16.dp))
                .clickable(onClick = onClick)
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text,
                style = SuhoodTypography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = if (active) colors.accentLavender else colors.textPrimary
            )
        }
    }

    private fun formatDisplayDate(iso: String): String {
        return try {
            val inFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val outFmt = SimpleDateFormat("d MMM yyyy", Locale.US)
            outFmt.format(inFmt.parse(iso)!!)
        } catch (_: Exception) {
            iso
        }
    }
}

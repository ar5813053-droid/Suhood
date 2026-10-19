package com.suhood.feature.alarms

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suhood.app.SuhoodApplication
import com.suhood.core.database.AlarmEntity
import com.suhood.core.designsystem.components.*
import com.suhood.core.designsystem.theme.SuhoodSpacing
import com.suhood.core.designsystem.theme.SuhoodTypography
import com.suhood.core.designsystem.theme.suhoodColors
import com.suhood.core.model.ChallengeType
import com.suhood.core.notifications.AlarmScheduler
import com.suhood.core.prayer.FajrAutoScheduler
import com.suhood.core.prayer.PrayerSettingsStore
import java.util.UUID
import kotlinx.coroutines.launch

object AlarmScreens {

    @Composable
    fun List(
        onBack: () -> Unit,
        onCreate: () -> Unit,
        onEdit: (String) -> Unit,
        onSimulateRing: (String) -> Unit
    ) {
        val context = LocalContext.current
        val app = context.applicationContext as SuhoodApplication
        val dao = app.database.dao()
        val alarms by dao.observeAlarms().collectAsStateWithLifecycle(initialValue = emptyList())
        val scope = rememberCoroutineScope()
        val colors = suhoodColors
        val prayerStore = remember { PrayerSettingsStore(context) }
        val prayer by prayerStore.settings.collectAsStateWithLifecycle(
            initialValue = com.suhood.core.prayer.PrayerSettings()
        )

        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            ScreenHeader(
                title = "Alarms",
                onBack = onBack,
                actions = {
                    GlassButton(text = "Add", onClick = onCreate)
                }
            )
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = SuhoodSpacing.screenHorizontal)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    "Uses AlarmManager. Grant Notifications and Alarms & reminders for reliable delivery.",
                    style = SuhoodTypography.bodySmall,
                    color = colors.textMuted
                )
                Spacer(Modifier.height(16.dp))

                // Automatic Fajr
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Automatic Fajr alarm", style = SuhoodTypography.titleMedium, color = colors.textPrimary)
                    Text(
                        "Rings ${prayer.fajrLeadMinutes} minutes before effective Fajr start (from Prayer settings).",
                        style = SuhoodTypography.bodySmall,
                        color = colors.textMuted
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassButton(
                            text = if (prayer.autoFajrEnabled) "On" else "Off",
                            onClick = {
                                scope.launch {
                                    FajrAutoScheduler.setEnabled(context, !prayer.autoFajrEnabled)
                                }
                            }
                        )
                        GlassButton(
                            text = "Skip today",
                            onClick = { scope.launch { FajrAutoScheduler.skipToday(context) } },
                            primary = false
                        )
                    }
                    if (prayer.fajrSkipDate.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text("Skipped date: ${prayer.fajrSkipDate}", style = SuhoodTypography.labelSmall, color = colors.textMuted)
                    }
                }
                Spacer(Modifier.height(16.dp))

                val userAlarms = alarms.filter { !it.isFajrAuto }
                userAlarms.forEach { alarm ->
                    GlassCard(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        onClick = { onEdit(alarm.id) }
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "%02d:%02d".format(alarm.hour, alarm.minute),
                                    style = SuhoodTypography.headlineSmall,
                                    color = colors.textPrimary
                                )
                                Text(alarm.label, style = SuhoodTypography.bodySmall, color = colors.textSecondary)
                                Text(
                                    buildString {
                                        if (alarm.repeatDaysCsv.isBlank()) append("One-time")
                                        else append("Repeats")
                                        append(" · ")
                                        append(alarm.challengeType)
                                        if (alarm.soundUri.isNotBlank()) append(" · Custom sound")
                                        else append(" · Default tone")
                                    },
                                    style = SuhoodTypography.labelSmall,
                                    color = colors.textMuted
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                GlassPill(
                                    selected = alarm.enabled,
                                    onClick = {
                                        scope.launch {
                                            val updated = alarm.copy(enabled = !alarm.enabled)
                                            dao.upsertAlarm(updated)
                                            AlarmScheduler.schedule(context, updated)
                                        }
                                    }
                                ) {
                                    Text(
                                        if (alarm.enabled) "On" else "Off",
                                        style = SuhoodTypography.labelMedium,
                                        color = colors.textPrimary
                                    )
                                }
                            }
                        }
                    }
                }
                if (userAlarms.isEmpty()) {
                    Spacer(Modifier.height(24.dp))
                    EmptyState("No alarms", "Create an alarm with time, sound, and optional wake-up challenge.")
                }
            }
        }
    }

    @Composable
    fun CreateEdit(alarmId: String?, onBack: () -> Unit, onSaved: () -> Unit) {
        val context = LocalContext.current
        val app = context.applicationContext as SuhoodApplication
        val dao = app.database.dao()
        val scope = rememberCoroutineScope()
        val colors = suhoodColors

        var hour by remember { mutableIntStateOf(5) }
        var minute by remember { mutableIntStateOf(0) }
        var label by remember { mutableStateOf("Alarm") }
        var enabled by remember { mutableStateOf(true) }
        var soundUri by remember { mutableStateOf("") }
        var challenge by remember { mutableStateOf(ChallengeType.NONE) }
        var days by remember { mutableStateOf(setOf<Int>()) } // Calendar DAY_OF_WEEK
        var loaded by remember { mutableStateOf(false) }

        LaunchedEffect(alarmId) {
            if (alarmId != null) {
                val existing = dao.getAlarm(alarmId)
                if (existing != null) {
                    hour = existing.hour
                    minute = existing.minute
                    label = existing.label
                    enabled = existing.enabled
                    soundUri = existing.soundUri
                    challenge = runCatching { ChallengeType.valueOf(existing.challengeType) }
                        .getOrDefault(ChallengeType.NONE)
                    days = existing.repeatDaysCsv.split(',')
                        .mapNotNull { it.trim().toIntOrNull() }.toSet()
                }
            }
            loaded = true
        }

        val pickSound = rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri: Uri? ->
            if (uri != null) {
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                soundUri = uri.toString()
            }
        }

        if (!loaded) return

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SuhoodSpacing.screenHorizontal)
                .padding(bottom = 32.dp)
        ) {
            ScreenHeader(title = if (alarmId == null) "New alarm" else "Edit alarm", onBack = onBack)
            Spacer(Modifier.height(12.dp))

            Text("Time (24h)", style = SuhoodTypography.labelMedium, color = colors.textMuted)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GlassSearchField(
                    query = hour.toString().padStart(2, '0'),
                    onQueryChange = { hour = it.toIntOrNull()?.coerceIn(0, 23) ?: hour },
                    placeholder = "HH",
                    modifier = Modifier.weight(1f)
                )
                GlassSearchField(
                    query = minute.toString().padStart(2, '0'),
                    onQueryChange = { minute = it.toIntOrNull()?.coerceIn(0, 59) ?: minute },
                    placeholder = "MM",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text("Label", style = SuhoodTypography.labelMedium, color = colors.textMuted)
            GlassSearchField(query = label, onQueryChange = { label = it }, placeholder = "Label")

            Spacer(Modifier.height(16.dp))
            Text("Repeat (Sun=1 … Sat=7)", style = SuhoodTypography.labelMedium, color = colors.textMuted)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf(1 to "S", 2 to "M", 3 to "T", 4 to "W", 5 to "T", 6 to "F", 7 to "S").forEach { (d, letter) ->
                    GlassPill(
                        selected = d in days,
                        onClick = {
                            days = if (d in days) days - d else days + d
                        }
                    ) {
                        Text(letter, style = SuhoodTypography.labelMedium, color = colors.textPrimary)
                    }
                }
            }
            Text(
                if (days.isEmpty()) "One-time (next occurrence)" else "Repeats on selected days",
                style = SuhoodTypography.labelSmall,
                color = colors.textMuted
            )

            Spacer(Modifier.height(16.dp))
            Text("Sound", style = SuhoodTypography.labelMedium, color = colors.textMuted)
            Text(
                if (soundUri.isBlank()) "System default alarm tone" else "Custom file selected",
                style = SuhoodTypography.bodySmall,
                color = colors.textSecondary
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassButton(
                    text = "Pick audio",
                    onClick = { pickSound.launch(arrayOf("audio/*")) }
                )
                GlassButton(
                    text = "Default",
                    onClick = { soundUri = "" },
                    primary = false
                )
            }

            Spacer(Modifier.height(16.dp))
            Text("Wake-up challenge", style = SuhoodTypography.labelMedium, color = colors.textMuted)
            val types = ChallengeType.entries
            types.forEach { type ->
                GlassCard(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    selected = challenge == type,
                    onClick = { challenge = type }
                ) {
                    Text(type.name, style = SuhoodTypography.titleSmall, color = colors.textPrimary)
                }
            }

            Spacer(Modifier.height(24.dp))
            GlassButton(
                text = "Save",
                onClick = {
                    scope.launch {
                        val id = alarmId ?: UUID.randomUUID().toString()
                        val entity = AlarmEntity(
                            id = id,
                            hour = hour,
                            minute = minute,
                            label = label.ifBlank { "Alarm" },
                            enabled = enabled,
                            repeatDaysCsv = days.sorted().joinToString(","),
                            soundUri = soundUri,
                            challengeType = challenge.name,
                            isFajrAuto = false
                        )
                        dao.upsertAlarm(entity)
                        AlarmScheduler.schedule(context, entity)
                        onSaved()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            if (alarmId != null) {
                Spacer(Modifier.height(8.dp))
                GlassButton(
                    text = "Delete",
                    onClick = {
                        scope.launch {
                            AlarmScheduler.cancel(context, alarmId)
                            dao.deleteAlarm(alarmId)
                            onSaved()
                        }
                    },
                    primary = false,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    @Composable
    fun Dismissed(onBack: () -> Unit) {
        val colors = suhoodColors
        Column(
            Modifier.fillMaxSize().statusBarsPadding().padding(SuhoodSpacing.screenHorizontal),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Alarm dismissed", style = SuhoodTypography.headlineLarge, color = colors.textPrimary)
            Spacer(Modifier.height(16.dp))
            GlassButton(text = "Back", onClick = onBack)
        }
    }
}

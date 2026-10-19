package com.suhood.feature.alarms

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.launch

object AlarmScreens {

    private fun format12h(hour24: Int, minute: Int): Pair<String, String> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour24)
            set(Calendar.MINUTE, minute)
        }
        val h = cal.get(Calendar.HOUR).let { if (it == 0) 12 else it }
        val ampm = if (cal.get(Calendar.AM_PM) == Calendar.AM) "am" else "pm"
        return "%d:%02d".format(h, minute) to ampm
    }

    private fun repeatLabel(csv: String): String {
        if (csv.isBlank()) return "Once"
        val days = csv.split(',').mapNotNull { it.trim().toIntOrNull() }.toSet()
        return if (days.size >= 7) "Daily" else "Custom"
    }

    @Composable
    fun Dashboard(
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
        val userAlarms = alarms.filter { !it.isFajrAuto }.sortedWith(
            compareBy({ it.hour }, { it.minute })
        )

        Box(Modifier.fillMaxSize().statusBarsPadding()) {
            Column(Modifier.fillMaxSize()) {
                ScreenHeader(title = "Alarm", onBack = onBack)
                LazyColumn(
                    contentPadding = PaddingValues(
                        horizontal = SuhoodSpacing.screenHorizontal,
                        vertical = 8.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                "Automatic Fajr",
                                style = SuhoodTypography.titleSmall,
                                color = colors.textPrimary
                            )
                            Text(
                                "${prayer.fajrLeadMinutes} min before Fajr · from Prayer settings",
                                style = SuhoodTypography.labelSmall,
                                color = colors.textMuted
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    if (prayer.autoFajrEnabled) "Enabled" else "Disabled",
                                    color = colors.textSecondary
                                )
                                Switch(
                                    checked = prayer.autoFajrEnabled,
                                    onCheckedChange = {
                                        scope.launch { FajrAutoScheduler.setEnabled(context, it) }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedTrackColor = Color(0xFF3B82F6),
                                        checkedThumbColor = Color.White
                                    )
                                )
                            }
                            if (prayer.autoFajrEnabled) {
                                Spacer(Modifier.height(8.dp))
                                GlassButton(
                                    text = "Skip today's Fajr alarm",
                                    onClick = { scope.launch { FajrAutoScheduler.skipToday(context) } },
                                    primary = false,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                    items(userAlarms, key = { it.id }) { alarm ->
                        val (timeStr, ampm) = format12h(alarm.hour, alarm.minute)
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onEdit(alarm.id) }
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            timeStr,
                                            style = SuhoodTypography.headlineLarge.copy(
                                                fontSize = 40.sp,
                                                fontWeight = FontWeight.Light
                                            ),
                                            color = if (alarm.enabled) colors.textPrimary
                                            else colors.textMuted
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            ampm,
                                            style = SuhoodTypography.titleMedium,
                                            color = if (alarm.enabled) colors.textSecondary
                                            else colors.textMuted,
                                            modifier = Modifier.padding(bottom = 6.dp)
                                        )
                                    }
                                    Text(
                                        buildString {
                                            append(repeatLabel(alarm.repeatDaysCsv))
                                            if (alarm.label.isNotBlank() && alarm.label != "Alarm") {
                                                append(" · "); append(alarm.label)
                                            }
                                        },
                                        style = SuhoodTypography.bodySmall,
                                        color = colors.textMuted
                                    )
                                }
                                Switch(
                                    checked = alarm.enabled,
                                    onCheckedChange = { on ->
                                        scope.launch {
                                            val u = alarm.copy(enabled = on)
                                            dao.upsertAlarm(u)
                                            AlarmScheduler.schedule(context, u)
                                        }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedTrackColor = Color(0xFF3B82F6),
                                        checkedThumbColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                    if (userAlarms.isEmpty()) {
                        item {
                            EmptyState(
                                "No alarms",
                                "Tap + to create a real AlarmManager alarm."
                            )
                        }
                    }
                    item { Spacer(Modifier.height(88.dp)) }
                }
            }
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2563EB))
                    .clickable(onClick = onCreate),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Icon(
                    Icons.Filled.Add,
                    contentDescription = "Add alarm",
                    tint = Color.White
                )
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

        var hour24 by remember { mutableIntStateOf(5) }
        var minute by remember { mutableIntStateOf(0) }
        var label by remember { mutableStateOf("") }
        var soundUri by remember { mutableStateOf("") }
        var challenge by remember { mutableStateOf(ChallengeType.NONE) }
        var days by remember { mutableStateOf(setOf<Int>()) }
        var vibrate by remember { mutableStateOf(true) }
        var deleteAfter by remember { mutableStateOf(false) }
        var loaded by remember { mutableStateOf(false) }

        LaunchedEffect(alarmId) {
            if (alarmId != null) {
                dao.getAlarm(alarmId)?.let { e ->
                    hour24 = e.hour
                    minute = e.minute
                    label = e.label
                    soundUri = e.soundUri
                    challenge = runCatching { ChallengeType.valueOf(e.challengeType) }
                        .getOrDefault(ChallengeType.NONE)
                    days = e.repeatDaysCsv.split(',').mapNotNull { it.trim().toIntOrNull() }.toSet()
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
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                soundUri = uri.toString()
            }
        }

        if (!loaded) return

        val isAm = hour24 < 12
        val hour12 = when (val h = hour24 % 12) {
            0 -> 12
            else -> h
        }

        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassIconButton(
                    icon = Icons.Filled.Close,
                    contentDescription = "Close",
                    onClick = onBack
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Add alarm", style = SuhoodTypography.titleMedium, color = colors.textPrimary)
                    val next = AlarmScheduler.nextTriggerMillis(
                        hour24, minute, days.sorted().joinToString(",")
                    )
                    val mins = ((next - System.currentTimeMillis()) / 60_000L).coerceAtLeast(0)
                    Text(
                        "Alarm in ${mins / 60} hours ${mins % 60} minutes",
                        style = SuhoodTypography.labelSmall,
                        color = colors.textMuted
                    )
                }
                GlassIconButton(
                    icon = Icons.Filled.Check,
                    contentDescription = "Save",
                    onClick = {
                        scope.launch {
                            val id = alarmId ?: UUID.randomUUID().toString()
                            val entity = AlarmEntity(
                                id = id,
                                hour = hour24,
                                minute = minute,
                                label = label.ifBlank { "Alarm" },
                                enabled = true,
                                repeatDaysCsv = days.sorted().joinToString(","),
                                soundUri = soundUri,
                                challengeType = challenge.name,
                                isFajrAuto = false
                            )
                            dao.upsertAlarm(entity)
                            AlarmScheduler.schedule(context, entity)
                            onSaved()
                        }
                    }
                )
            }

            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = SuhoodSpacing.screenHorizontal)
            ) {
                // Time wheels (scrollable columns)
                Row(
                    Modifier.fillMaxWidth().height(200.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        listOf("AM", "PM").forEach { labelAmpm ->
                            val selected = (labelAmpm == "AM") == isAm
                            Text(
                                labelAmpm,
                                style = SuhoodTypography.headlineSmall,
                                color = if (selected) colors.textPrimary else colors.textMuted,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .clickable {
                                        hour24 = if (labelAmpm == "AM") {
                                            if (hour12 == 12) 0 else hour12
                                        } else {
                                            if (hour12 == 12) 12 else hour12 + 12
                                        }
                                    }
                            )
                        }
                    }
                    Column(
                        Modifier.verticalScroll(rememberScrollState()).height(180.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        (1..12).forEach { h ->
                            val selected = h == hour12
                            Text(
                                "%02d".format(h),
                                fontSize = if (selected) 36.sp else 22.sp,
                                color = if (selected) colors.textPrimary else colors.textMuted,
                                modifier = Modifier
                                    .padding(vertical = 6.dp)
                                    .clickable {
                                        hour24 = if (isAm) {
                                            if (h == 12) 0 else h
                                        } else {
                                            if (h == 12) 12 else h + 12
                                        }
                                    }
                            )
                        }
                    }
                    Column(
                        Modifier.verticalScroll(rememberScrollState()).height(180.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        (0..59).forEach { m ->
                            val selected = m == minute
                            Text(
                                "%02d".format(m),
                                fontSize = if (selected) 36.sp else 22.sp,
                                color = if (selected) colors.textPrimary else colors.textMuted,
                                modifier = Modifier
                                    .padding(vertical = 6.dp)
                                    .clickable { minute = m }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                SettingsRow(
                    title = "Ringtone",
                    value = if (soundUri.isBlank()) "Default alarm" else "Custom file",
                    onClick = { pickSound.launch(arrayOf("audio/*")) }
                )
                SettingsRow(
                    title = "Repeat",
                    value = repeatLabel(days.sorted().joinToString(",")),
                    onClick = { }
                )
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf(1 to "S", 2 to "M", 3 to "T", 4 to "W", 5 to "T", 6 to "F", 7 to "S")
                        .forEach { (d, letter) ->
                            val on = d in days
                            Box(
                                Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (on) Color(0xFF2563EB) else colors.surfaceGlass)
                                    .clickable {
                                        days = if (on) days - d else days + d
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(letter, color = colors.textPrimary, fontSize = 12.sp)
                            }
                        }
                }
                SwitchRow("Vibrate when alarm sounds", vibrate) { vibrate = it }
                SwitchRow("Delete after goes off", deleteAfter) { deleteAfter = it }

                Spacer(Modifier.height(12.dp))
                Text("Label", style = SuhoodTypography.labelMedium, color = colors.textMuted)
                GlassSearchField(
                    query = label,
                    onQueryChange = { label = it },
                    placeholder = "Enter label"
                )

                Spacer(Modifier.height(16.dp))
                Text("Wake-up challenge", style = SuhoodTypography.labelMedium, color = colors.textMuted)
                ChallengeType.entries.forEach { type ->
                    GlassCard(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        selected = challenge == type,
                        onClick = { challenge = type }
                    ) {
                        Text(type.name, color = colors.textPrimary)
                    }
                }

                if (alarmId != null) {
                    Spacer(Modifier.height(16.dp))
                    GlassButton(
                        text = "Delete alarm",
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
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    @Composable
    private fun SettingsRow(title: String, value: String, onClick: () -> Unit) {
        val colors = suhoodColors
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, style = SuhoodTypography.bodyLarge, color = colors.textPrimary)
            Text(value, style = SuhoodTypography.bodyMedium, color = colors.textMuted)
        }
    }

    @Composable
    private fun SwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
        val colors = suhoodColors
        Row(
            Modifier.fillMaxWidth().padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = SuhoodTypography.bodyLarge, color = colors.textPrimary)
            Switch(
                checked = checked,
                onCheckedChange = onChange,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = Color(0xFF3B82F6),
                    checkedThumbColor = Color.White
                )
            )
        }
    }

    @Composable
    fun Dismissed(onDone: () -> Unit) {
        val colors = suhoodColors
        Column(
            Modifier.fillMaxSize().statusBarsPadding().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Alarm dismissed", style = SuhoodTypography.headlineMedium, color = colors.textPrimary)
            Spacer(Modifier.height(16.dp))
            GlassButton(text = "Back", onClick = onDone)
        }
    }

    @Composable
    fun Ringing(onQuiz: () -> Unit, onDhikr: () -> Unit, onDismiss: () -> Unit) {
        val colors = suhoodColors
        Column(
            Modifier.fillMaxSize().statusBarsPadding().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Alarm", style = SuhoodTypography.headlineMedium, color = colors.textPrimary)
            Text("Real ring uses system AlarmRingActivity", color = colors.textMuted)
            Spacer(Modifier.height(16.dp))
            GlassButton(text = "Math challenge", onClick = onQuiz, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            GlassButton(text = "Tap challenge", onClick = onDhikr, primary = false, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            GlassButton(text = "Dismiss", onClick = onDismiss, primary = false, modifier = Modifier.fillMaxWidth())
        }
    }

    @Composable
    fun QuizChallenge(onComplete: () -> Unit) {
        val colors = suhoodColors
        var a by remember { mutableIntStateOf(4) }
        var b by remember { mutableIntStateOf(7) }
        var input by remember { mutableStateOf("") }
        Column(
            Modifier.fillMaxSize().statusBarsPadding().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("$a + $b = ?", style = SuhoodTypography.headlineLarge, color = colors.textPrimary)
            Spacer(Modifier.height(12.dp))
            GlassSearchField(query = input, onQueryChange = { input = it }, placeholder = "Answer")
            Spacer(Modifier.height(12.dp))
            GlassButton(
                text = "Submit",
                onClick = { if (input.trim().toIntOrNull() == a + b) onComplete() },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    @Composable
    fun DhikrChallenge(onComplete: () -> Unit) {
        var taps by remember { mutableIntStateOf(0) }
        val colors = suhoodColors
        Column(
            Modifier.fillMaxSize().statusBarsPadding().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GlassButton(
                text = "Tap (${taps}/5)",
                onClick = { taps++; if (taps >= 5) onComplete() },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

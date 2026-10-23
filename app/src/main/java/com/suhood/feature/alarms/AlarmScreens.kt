package com.suhood.feature.alarms
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextFieldDefaults

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
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
                Text("+", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Light)
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

        var showMusicPicker by remember { mutableStateOf(false) }
        val pickSoundFallback = rememberLauncherForActivityResult(
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
                // Cyclic hour/minute wheels (wrap around; virtualized, not infinite memory)
                val hourCycle = 12
                val minuteCycle = 60
                val virtualMid = 5000
                val hourListState = rememberLazyListState(
                    initialFirstVisibleItemIndex = virtualMid + (hour12 - 1)
                )
                val minuteListState = rememberLazyListState(
                    initialFirstVisibleItemIndex = virtualMid + minute
                )
                val hourSnap = rememberSnapFlingBehavior(lazyListState = hourListState)
                val minuteSnap = rememberSnapFlingBehavior(lazyListState = minuteListState)

                LaunchedEffect(hourListState.isScrollInProgress) {
                    if (!hourListState.isScrollInProgress) {
                        val idx = hourListState.firstVisibleItemIndex
                        val h = ((idx % hourCycle) + hourCycle) % hourCycle + 1 // 1..12
                        val newHour24 = if (isAm) {
                            if (h == 12) 0 else h
                        } else {
                            if (h == 12) 12 else h + 12
                        }
                        if (newHour24 != hour24) hour24 = newHour24
                    }
                }
                LaunchedEffect(minuteListState.isScrollInProgress) {
                    if (!minuteListState.isScrollInProgress) {
                        val idx = minuteListState.firstVisibleItemIndex
                        val m = ((idx % minuteCycle) + minuteCycle) % minuteCycle // 0..59
                        if (m != minute) minute = m
                    }
                }

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
                                color = if (selected) colors.accentLavender else colors.textMuted,
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
                    Box(Modifier.height(180.dp).width(72.dp), contentAlignment = Alignment.Center) {
                        LazyColumn(
                            state = hourListState,
                            flingBehavior = hourSnap,
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            items(10_000) { index ->
                                val h = (index % hourCycle) + 1
                                val selected = h == hour12
                                Text(
                                    "%02d".format(h),
                                    fontSize = if (selected) 36.sp else 22.sp,
                                    color = if (selected) colors.accentLavender else colors.textMuted,
                                    modifier = Modifier
                                        .padding(vertical = 8.dp)
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
                    }
                    Box(Modifier.height(180.dp).width(72.dp), contentAlignment = Alignment.Center) {
                        LazyColumn(
                            state = minuteListState,
                            flingBehavior = minuteSnap,
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            items(10_000) { index ->
                                val m = index % minuteCycle
                                val selected = m == minute
                                Text(
                                    "%02d".format(m),
                                    fontSize = if (selected) 36.sp else 22.sp,
                                    color = if (selected) colors.accentLavender else colors.textMuted,
                                    modifier = Modifier
                                        .padding(vertical = 8.dp)
                                        .clickable { minute = m }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                SettingsRow(
                    title = "Alarm music",
                    value = if (soundUri.isBlank()) "Default alarm" else "Custom track",
                    onClick = { showMusicPicker = true }
                )
                if (showMusicPicker) {
                    AlarmMusicPickerSheet(
                        onDismiss = { showMusicPicker = false },
                        onSelect = { uri, _ ->
                            soundUri = uri
                            showMusicPicker = false
                        },
                        onPickFile = {
                            showMusicPicker = false
                            pickSoundFallback.launch(arrayOf("audio/*"))
                        }
                    )
                }
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
                Spacer(Modifier.height(8.dp))
                Text("Wake up challenge", style = SuhoodTypography.titleSmall, color = colors.textPrimary)
                Spacer(Modifier.height(6.dp))
                SwitchRow(
                    "Enable wake-up challenge",
                    challenge != ChallengeType.NONE
                ) {
                    challenge = if (it) ChallengeType.MATH else ChallengeType.NONE
                }
                if (challenge != ChallengeType.NONE) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            ChallengeType.MATH to "Math",
                            ChallengeType.TAP to "Taps",
                            ChallengeType.NONE to "Off"
                        ).forEach { (type, label) ->
                            val sel = challenge == type
                            GlassPill(selected = sel, onClick = { challenge = type }) {
                                Text(label, color = if (sel) colors.textPrimary else colors.textMuted)
                            }
                        }
                    }
                }
                SwitchRow("Delete after goes off", deleteAfter) { deleteAfter = it }

                Spacer(Modifier.height(12.dp))
                Text("Label", style = SuhoodTypography.labelMedium, color = colors.textMuted)
                GlassSearchField(
                    query = label,
                    onQueryChange = { label = it },
                    placeholder = "Enter label"
                )


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



@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlarmMusicPickerSheet(
    onDismiss: () -> Unit,
    onSelect: (uri: String, title: String) -> Unit,
    onPickFile: () -> Unit
) {
    val context = LocalContext.current
    val colors = suhoodColors
    var query by remember { mutableStateOf("") }
    var tracks by remember { mutableStateOf<List<Triple<String, String, String>>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var permissionOk by remember { mutableStateOf(false) }
    var selectedUri by remember { mutableStateOf<String?>(null) }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> permissionOk = granted }

    LaunchedEffect(Unit) {
        val perm = if (android.os.Build.VERSION.SDK_INT >= 33)
            android.Manifest.permission.READ_MEDIA_AUDIO
        else android.Manifest.permission.READ_EXTERNAL_STORAGE
        val granted = androidx.core.content.ContextCompat.checkSelfPermission(
            context, perm
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        permissionOk = granted
        if (!granted) permLauncher.launch(perm)
    }

    LaunchedEffect(permissionOk) {
        if (!permissionOk) { loading = false; return@LaunchedEffect }
        loading = true
        tracks = withContext(Dispatchers.IO) { loadDeviceAudio(context) }
        loading = false
    }

    val filtered = remember(tracks, query) {
        if (query.isBlank()) tracks
        else tracks.filter {
            it.second.contains(query, ignoreCase = true) ||
                it.third.contains(query, ignoreCase = true)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xF21C1C20),
        contentColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 14.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.2f))
            )
        }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 28.dp)
        ) {
            // Search
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .padding(top = 8.dp),
                singleLine = true,
                placeholder = { Text("Search songs…", color = Color.White.copy(alpha = 0.28f)) },
                leadingIcon = { Icon(Icons.Filled.Search, null, tint = Color.White.copy(alpha = 0.35f)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White.copy(alpha = 0.92f),
                    unfocusedTextColor = Color.White.copy(alpha = 0.92f),
                    focusedBorderColor = Color(0xFF7B7FFF),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.08f),
                    focusedContainerColor = Color.White.copy(alpha = 0.07f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.07f),
                    cursorColor = Color(0xFF7B7FFF)
                ),
                shape = RoundedCornerShape(14.dp)
            )

            // Quick actions
            Column(Modifier.padding(horizontal = 18.dp, vertical = 10.dp)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelect("", "Default alarm") }
                        .padding(13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x2E7B7FFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.MusicNote, null, tint = Color(0xFF7B7FFF), modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("Use default alarm sound", color = Color(0xFF7B7FFF), fontWeight = FontWeight.Medium)
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onPickFile)
                        .padding(13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.07f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Folder, null, tint = Color.White.copy(alpha = 0.48f), modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("Browse files…", color = Color.White.copy(alpha = 0.48f), fontWeight = FontWeight.Medium)
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(horizontal = 18.dp))
            Text(
                "ON THIS DEVICE",
                color = Color.White.copy(alpha = 0.28f),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp,
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 10.dp)
            )

            when {
                !permissionOk -> Text(
                    "Allow music access to list your songs in-app.",
                    color = Color.White.copy(alpha = 0.48f),
                    modifier = Modifier.padding(horizontal = 18.dp)
                )
                loading -> Text(
                    "Loading library…",
                    color = Color.White.copy(alpha = 0.48f),
                    modifier = Modifier.padding(horizontal = 18.dp)
                )
                filtered.isEmpty() -> Text(
                    "No songs found.",
                    color = Color.White.copy(alpha = 0.48f),
                    modifier = Modifier.padding(horizontal = 18.dp)
                )
                else -> LazyColumn(
                    Modifier
                        .heightIn(max = 360.dp)
                        .padding(horizontal = 18.dp)
                ) {
                    items(filtered, key = { it.first }) { (uri, title, artist) ->
                        val active = selectedUri == uri
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (active) Color(0x2E7B7FFF) else Color.Transparent)
                                .clickable {
                                    selectedUri = uri
                                    onSelect(uri, title)
                                }
                                .padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF1E1E2E), Color(0xFF2A1F3D))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.MusicNote,
                                    null,
                                    tint = if (active) Color(0xFF7B7FFF) else Color.White.copy(alpha = 0.3f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    title,
                                    color = if (active) Color(0xFF7B7FFF) else Color.White.copy(alpha = 0.92f),
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1
                                )
                                Text(
                                    artist.ifBlank { "Unknown artist" },
                                    color = Color.White.copy(alpha = 0.48f),
                                    fontSize = 13.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


private fun loadDeviceAudio(context: android.content.Context): List<Triple<String, String, String>> {
    val out = ArrayList<Triple<String, String, String>>()
    val projection = arrayOf(
        android.provider.MediaStore.Audio.Media._ID,
        android.provider.MediaStore.Audio.Media.TITLE,
        android.provider.MediaStore.Audio.Media.DISPLAY_NAME,
        android.provider.MediaStore.Audio.Media.ARTIST
    )
    val selection = "${android.provider.MediaStore.Audio.Media.IS_MUSIC} != 0"
    runCatching {
        context.contentResolver.query(
            android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection, selection, null,
            android.provider.MediaStore.Audio.Media.TITLE + " ASC"
        )?.use { c ->
            val idIdx = c.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media._ID)
            val titleIdx = c.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.TITLE)
            val nameIdx = c.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.DISPLAY_NAME)
            val artistIdx = c.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.ARTIST)
            while (c.moveToNext()) {
                val id = c.getLong(idIdx)
                val title = c.getString(titleIdx)?.takeIf { it.isNotBlank() } ?: c.getString(nameIdx) ?: "Track"
                val artist = c.getString(artistIdx)?.takeIf { it.isNotBlank() && it != "<unknown>" } ?: ""
                val uri = android.content.ContentUris.withAppendedId(
                    android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                ).toString()
                out.add(Triple(uri, title, artist))
            }
        }
    }
    return out
}

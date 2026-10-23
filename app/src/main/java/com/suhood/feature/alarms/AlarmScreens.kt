package com.suhood.feature.alarms
import androidx.compose.foundation.layout.PaddingValues
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

        var hour24 by remember { mutableIntStateOf(6) }
        var minute by remember { mutableIntStateOf(30) }
        var label by remember { mutableStateOf("") }
        var soundUri by remember { mutableStateOf("") }
        var challenge by remember { mutableStateOf(ChallengeType.NONE) }
        var days by remember { mutableStateOf(setOf<Int>()) }
        var vibrate by remember { mutableStateOf(true) }
        var snooze by remember { mutableStateOf(true) }
        var loaded by remember { mutableStateOf(false) }
        var showMusicPicker by remember { mutableStateOf(false) }
        var showRepeatSheet by remember { mutableStateOf(false) }

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
        val hour12 = when (val h = hour24 % 12) { 0 -> 12; else -> h }

        val accentOrange = Color(0xFFFF9F0A)
        val rowBg = Color(0xFF1C1C1E)
        val groupShape = RoundedCornerShape(12.dp)

        fun save() {
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

        Column(
            Modifier
                .fillMaxSize()
                .background(Color(0xFF000000))
                .statusBarsPadding()
        ) {
            // iOS header: Cancel · Add Alarm · Save
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Cancel",
                    color = accentOrange,
                    style = SuhoodTypography.bodyLarge,
                    modifier = Modifier.clickable(onClick = onBack)
                )
                Text(
                    if (alarmId == null) "Add Alarm" else "Edit Alarm",
                    style = SuhoodTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White
                )
                Text(
                    "Save",
                    color = accentOrange,
                    style = SuhoodTypography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.clickable { save() }
                )
            }

            // Drum-roll time picker
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                // Selection band
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                )

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hours 1–12
                    IosWheel(
                        items = (1..12).map { it.toString().padStart(2, '0') },
                        selectedIndex = hour12 - 1,
                        onSelected = { idx ->
                            val h12 = idx + 1
                            hour24 = if (isAm) {
                                if (h12 == 12) 0 else h12
                            } else {
                                if (h12 == 12) 12 else h12 + 12
                            }
                        },
                        modifier = Modifier.width(72.dp)
                    )
                    // Minutes 00–59
                    IosWheel(
                        items = (0..59).map { it.toString().padStart(2, '0') },
                        selectedIndex = minute,
                        onSelected = { minute = it },
                        modifier = Modifier.width(72.dp)
                    )
                    // AM / PM
                    IosWheel(
                        items = listOf("AM", "PM"),
                        selectedIndex = if (isAm) 0 else 1,
                        onSelected = { idx ->
                            val wantAm = idx == 0
                            if (wantAm && hour24 >= 12) hour24 -= 12
                            if (!wantAm && hour24 < 12) hour24 += 12
                        },
                        modifier = Modifier.width(72.dp),
                        textSize = 22.sp
                    )
                }

                // Top/bottom fade masks
                Column(Modifier.fillMaxSize()) {
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Black, Color.Transparent)
                                )
                            )
                    )
                    Spacer(Modifier.height(44.dp))
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black)
                                )
                            )
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                // Grouped settings card
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(groupShape)
                        .background(rowBg)
                ) {
                    IosSettingsRow(
                        title = "Repeat",
                        value = when {
                            days.isEmpty() -> "Never"
                            days.size == 7 -> "Every day"
                            days == setOf(2, 3, 4, 5, 6) -> "Weekdays"
                            else -> days.sorted().joinToString(" ") {
                                listOf("S", "M", "T", "W", "T", "F", "S").getOrElse(it - 1) { "?" }
                            }
                        },
                        onClick = { showRepeatSheet = true },
                        showDivider = true
                    )
                    IosSettingsRow(
                        title = "Label",
                        value = label.ifBlank { "Alarm" },
                        onClick = { /* inline edit below */ },
                        showDivider = true
                    )
                    IosSettingsRow(
                        title = "Sound",
                        value = when {
                            soundUri.isBlank() -> "Default"
                            else -> soundUri.substringAfterLast('/').take(18)
                        },
                        onClick = { showMusicPicker = true },
                        showDivider = true
                    )
                    IosSwitchRow(
                        title = "Snooze",
                        checked = snooze,
                        onChange = { snooze = it },
                        showDivider = false
                    )
                }

                // Label editor
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Label", color = Color.White.copy(alpha = 0.35f)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White.copy(alpha = 0.2f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.08f),
                        focusedContainerColor = rowBg,
                        unfocusedContainerColor = rowBg,
                        cursorColor = accentOrange
                    ),
                    shape = groupShape
                )

                // Wake-up challenge
                Spacer(Modifier.height(20.dp))
                Text(
                    "WAKE UP CHALLENGE",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.6.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(groupShape)
                        .background(rowBg)
                ) {
                    IosSwitchRow(
                        title = "Enable challenge",
                        checked = challenge != ChallengeType.NONE,
                        onChange = {
                            challenge = if (it) ChallengeType.MATH else ChallengeType.NONE
                        },
                        showDivider = challenge != ChallengeType.NONE
                    )
                    if (challenge != ChallengeType.NONE) {
                        listOf(
                            ChallengeType.MATH to "Math puzzle",
                            ChallengeType.TAP to "Tap sequence",
                            ChallengeType.MEMORY to "Memory"
                        ).forEachIndexed { i, (type, name) ->
                            val last = i == 2
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { challenge = type }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(name, color = Color.White, style = SuhoodTypography.bodyLarge)
                                if (challenge == type) {
                                    Text("✓", color = accentOrange, fontSize = 18.sp)
                                }
                            }
                            if (!last) {
                                HorizontalDivider(
                                    color = Color.White.copy(alpha = 0.08f),
                                    modifier = Modifier.padding(start = 16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(groupShape)
                        .background(rowBg)
                ) {
                    IosSwitchRow(
                        title = "Vibrate",
                        checked = vibrate,
                        onChange = { vibrate = it },
                        showDivider = false
                    )
                }

                if (alarmId != null) {
                    Spacer(Modifier.height(24.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(groupShape)
                            .background(rowBg)
                            .clickable {
                                scope.launch {
                                    AlarmScheduler.cancel(context, alarmId)
                                    dao.deleteAlarm(alarmId)
                                    onSaved()
                                }
                            }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Delete Alarm", color = Color(0xFFFF453A), style = SuhoodTypography.bodyLarge)
                    }
                }
                Spacer(Modifier.height(40.dp))
            }
        }

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

        if (showRepeatSheet) {
            ModalBottomSheet(
                onDismissRequest = { showRepeatSheet = false },
                containerColor = Color(0xFF1C1C1E),
                contentColor = Color.White
            ) {
                Text(
                    "Repeat",
                    style = SuhoodTypography.titleMedium,
                    color = Color.White,
                    modifier = Modifier.padding(16.dp)
                )
                val labels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                labels.forEachIndexed { i, name ->
                    val day = i + 1
                    val on = day in days
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                days = if (on) days - day else days + day
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(name, color = Color.White)
                        if (on) Text("✓", color = accentOrange)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    @Composable
    private fun IosSettingsRow(
        title: String,
        value: String,
        onClick: () -> Unit,
        showDivider: Boolean
    ) {
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, color = Color.White, style = SuhoodTypography.bodyLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(value, color = Color.White.copy(alpha = 0.45f), style = SuhoodTypography.bodyMedium)
                    Text("  ›", color = Color.White.copy(alpha = 0.3f))
                }
            }
            if (showDivider) {
                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.08f),
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }
    }

    @Composable
    private fun IosSwitchRow(
        title: String,
        checked: Boolean,
        onChange: (Boolean) -> Unit,
        showDivider: Boolean
    ) {
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, color = Color.White, style = SuhoodTypography.bodyLarge)
                Switch(
                    checked = checked,
                    onCheckedChange = onChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF30D158),
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color.White.copy(alpha = 0.2f)
                    )
                )
            }
            if (showDivider) {
                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.08f),
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }
    }

    /** Smooth iOS-style wheel picker. */
    @Composable
    private fun IosWheel(
        items: List<String>,
        selectedIndex: Int,
        onSelected: (Int) -> Unit,
        modifier: Modifier = Modifier,
        textSize: androidx.compose.ui.unit.TextUnit = 28.sp
    ) {
        val itemH = 44.dp
        val listState = rememberLazyListState(
            initialFirstVisibleItemIndex = selectedIndex.coerceIn(0, items.lastIndex)
        )
        val fling = rememberSnapFlingBehavior(lazyListState = listState)

        LaunchedEffect(listState.isScrollInProgress) {
            if (!listState.isScrollInProgress) {
                val idx = listState.firstVisibleItemIndex.coerceIn(0, items.lastIndex)
                if (idx != selectedIndex) onSelected(idx)
            }
        }
        LaunchedEffect(selectedIndex) {
            if (!listState.isScrollInProgress &&
                listState.firstVisibleItemIndex != selectedIndex
            ) {
                listState.animateScrollToItem(selectedIndex.coerceIn(0, items.lastIndex))
            }
        }

        LazyColumn(
            state = listState,
            flingBehavior = fling,
            modifier = modifier.height(220.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(vertical = 88.dp)
        ) {
            items(items.size) { i ->
                val selected = i == selectedIndex
                Box(
                    Modifier
                        .height(itemH)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        items[i],
                        color = if (selected) Color.White else Color.White.copy(alpha = 0.28f),
                        fontSize = textSize,
                        fontWeight = if (selected) FontWeight.Medium else FontWeight.Light
                    )
                }
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
                        Icon(Icons.Filled.MusicNote, null, tint = Color.White.copy(alpha = 0.48f), modifier = Modifier.size(16.dp))
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

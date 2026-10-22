package com.suhood.feature.phone

import android.Manifest
import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.telecom.TelecomManager
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val PhoneBg = Color(0xFF0A0A0F)
private val GlassFill = Color(0x1AFFFFFF)
private val GlassBorder = Color(0x38FFFFFF)
private val Label = Color(0xF2FFFFFF)
private val Label2 = Color(0x99FFFFFF)
private val Label3 = Color(0x61FFFFFF)
private val Green = Color(0xFF34C759)
private val Red = Color(0xFFFF3B30)

private enum class PhoneTab { KEYPAD, RECENTS, CONTACTS, MORE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneRoot(onBack: (() -> Unit)? = null) {
    val context = LocalContext.current
    val prefs = remember { PhonePrefs(context) }
    val repo = remember { PhoneRepository(context) }
    var tab by remember { mutableStateOf(PhoneTab.KEYPAD) }
    var showAppearance by remember { mutableStateOf(false) }
    var showRingtone by remember { mutableStateOf(false) }
    var digits by remember { mutableStateOf("") }

    if (showAppearance) {
        CallAppearanceStudio(prefs, onBack = { showAppearance = false })
        return
    }
    if (showRingtone) {
        RingtonePickerScreen(prefs, onBack = { showRingtone = false })
        return
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF1A1A2E), Color(0xFF12141C), PhoneBg))
            )
    ) {
        Column(Modifier.fillMaxSize()) {
            when (tab) {
                PhoneTab.KEYPAD -> KeypadPane(
                    digits = digits,
                    onDigits = { digits = it },
                    prefs = prefs,
                    repo = repo,
                    onBack = onBack
                )
                PhoneTab.RECENTS -> RecentsPane(repo = repo, prefs = prefs)
                PhoneTab.CONTACTS -> ContactsPane(repo = repo, prefs = prefs)
                PhoneTab.MORE -> MorePane(
                    prefs = prefs,
                    repo = repo,
                    onAppearance = { showAppearance = true },
                    onRingtone = { showRingtone = true },
                    onBack = onBack
                )
            }
        }
        // Glass tab bar
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color(0xBF12141C))
                .border(0.5.dp, Color(0x1FFFFFFF))
                .navigationBarsPadding()
                .padding(top = 10.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TabItem("Keypad", Icons.Filled.Dialpad, tab == PhoneTab.KEYPAD) { tab = PhoneTab.KEYPAD }
            TabItem("Recents", Icons.Filled.History, tab == PhoneTab.RECENTS) { tab = PhoneTab.RECENTS }
            TabItem("Contacts", Icons.Filled.People, tab == PhoneTab.CONTACTS) { tab = PhoneTab.CONTACTS }
            TabItem("More", Icons.Filled.Menu, tab == PhoneTab.MORE) { tab = PhoneTab.MORE }
        }
    }
}

@Composable
private fun TabItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = if (selected) Green else Label2, modifier = Modifier.size(22.dp))
        Text(label, color = if (selected) Green else Label2, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun KeypadPane(
    digits: String,
    onDigits: (String) -> Unit,
    prefs: PhonePrefs,
    repo: PhoneRepository,
    onBack: (() -> Unit)?
) {
    val context = LocalContext.current
    val display = PhoneNumberUtils.display(digits, prefs.numeralStyle)
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(bottom = 96.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Label)
                }
            }
            Text("Suhood Phone", color = Label, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Spacer(Modifier.width(48.dp))
        }
        Text(
            text = display.ifBlank { " " },
            color = Label,
            fontSize = if (digits.length > 12) 26.sp else 38.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = 2.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .heightIn(min = 56.dp)
        )
        val keys = listOf(
            "1" to "", "2" to "ABC", "3" to "DEF",
            "4" to "GHI", "5" to "JKL", "6" to "MNO",
            "7" to "PQRS", "8" to "TUV", "9" to "WXYZ",
            "*" to "", "0" to "+", "#" to ""
        )
        Column(Modifier.padding(horizontal = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            keys.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    row.forEach { (num, alpha) ->
                        GlassKey(num, alpha, prefs.numeralStyle) {
                            DialPadTone.play(num.first())
                            onDigits(digits + num)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 44.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                if (digits.isNotBlank()) {
                    // open contacts filter
                }
            }) {
                Icon(Icons.Filled.PersonAdd, null, tint = if (digits.isNotBlank()) Label else Color.Transparent)
            }
            Box(
                Modifier
                    .size(70.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Green, Color(0xFF28A745))))
                    .clickable {
                        val n = PhoneNumberUtils.normalize(digits)
                        if (!PhoneNumberUtils.isValidForDial(n)) {
                            Toast.makeText(context, "Enter a number", Toast.LENGTH_SHORT).show()
                            return@clickable
                        }
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE)
                            != PackageManager.PERMISSION_GRANTED
                        ) {
                            Toast.makeText(context, "Grant Phone permission in Settings", Toast.LENGTH_LONG).show()
                            return@clickable
                        }
                        repo.placeCall(n)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Call, null, tint = Color.White, modifier = Modifier.size(32.dp))
            }
            IconButton(onClick = {
                if (digits.isNotEmpty()) onDigits(digits.dropLast(1))
            }) {
                Icon(Icons.AutoMirrored.Filled.Backspace, null, tint = if (digits.isNotEmpty()) Label else Label3)
            }
        }
    }
}

@Composable
private fun GlassKey(num: String, alpha: String, style: NumeralStyle, onClick: () -> Unit) {
    val shown = PhoneNumberUtils.display(num, style)
    Box(
        Modifier
            .size(78.dp)
            .clip(CircleShape)
            .background(GlassFill)
            .border(1.dp, GlassBorder, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(shown, color = Label, fontSize = 28.sp, fontWeight = FontWeight.Light)
            if (alpha.isNotEmpty()) {
                Text(alpha, color = Label2, fontSize = 9.sp, letterSpacing = 1.sp)
            }
        }
    }
}

@Composable
private fun RecentsPane(repo: PhoneRepository, prefs: PhonePrefs) {
    val context = LocalContext.current
    var filter by remember { mutableStateOf("All") }
    var logs by remember { mutableStateOf<List<RecentCall>>(emptyList()) }
    LaunchedEffect(Unit) {
        logs = withContext(Dispatchers.IO) { repo.loadRecents() }
    }
    val filtered = when (filter) {
        "Missed" -> logs.filter { it.type == android.provider.CallLog.Calls.MISSED_TYPE }
        "Outgoing" -> logs.filter { it.type == android.provider.CallLog.Calls.OUTGOING_TYPE }
        "Incoming" -> logs.filter { !it.type == android.provider.CallLog.Calls.OUTGOING_TYPE && !it.type == android.provider.CallLog.Calls.MISSED_TYPE }
        else -> logs
    }
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(bottom = 96.dp)) {
        Text("Recents", color = Label, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, modifier = Modifier.padding(16.dp))
        Row(Modifier.padding(horizontal = 16.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("All", "Missed", "Outgoing", "Incoming").forEach { f ->
                val sel = filter == f
                Box(
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (sel) Green else GlassFill)
                        .border(1.dp, if (sel) Green else GlassBorder, RoundedCornerShape(20.dp))
                        .clickable { filter = f }
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(f, color = if (sel) Color.White else Label2, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
        if (filtered.isEmpty()) {
            Text("No calls yet", color = Label3, modifier = Modifier.padding(24.dp))
        } else {
            LazyColumn {
                items(filtered) { e ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                val n = PhoneNumberUtils.normalize(e.number)
                                if (n.isNotBlank()) repo.placeCall(n)
                            }
                            .padding(horizontal = 22.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (e.type == android.provider.CallLog.Calls.MISSED_TYPE) Icons.Filled.CallMissed else if (e.type == android.provider.CallLog.Calls.OUTGOING_TYPE) Icons.Filled.CallMade else Icons.Filled.CallReceived,
                            null,
                            tint = if (e.type == android.provider.CallLog.Calls.MISSED_TYPE) Red else Label2,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                PhoneNumberUtils.display(e.number, prefs.numeralStyle),
                                color = if (e.type == android.provider.CallLog.Calls.MISSED_TYPE) Red else Label,
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp
                            )
                            Text(
                                SimpleDateFormat("MMM d · HH:mm", Locale.getDefault()).format(Date(e.dateMs)),
                                color = Label2,
                                fontSize = 12.sp
                            )
                        }
                        IconButton(onClick = {
                            prefs.block(e.number)
                            Toast.makeText(context, "Blocked", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Filled.Block, null, tint = Label3)
                        }
                    }
                    HorizontalDivider(color = Color(0x1FFFFFFF))
                }
            }
        }
    }
}

@Composable
private fun ContactsPane(repo: PhoneRepository, prefs: PhonePrefs) {
    var query by remember { mutableStateOf("") }
    var contacts by remember { mutableStateOf<List<PhoneContact>>(emptyList()) }
    LaunchedEffect(Unit) {
        contacts = withContext(Dispatchers.IO) { repo.loadContacts() }
    }
    val filtered = contacts.filter {
        query.isBlank() || it.name.contains(query, true) || it.number.contains(query)
    }
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(bottom = 96.dp)) {
        Text("Contacts", color = Label, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, modifier = Modifier.padding(16.dp))
        Row(
            Modifier
                .padding(horizontal = 22.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(GlassFill)
                .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Search, null, tint = Label3, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                textStyle = LocalTextStyle.current.copy(color = Label, fontSize = 15.sp),
                modifier = Modifier.weight(1f),
                singleLine = true,
                decorationBox = { inner ->
                    if (query.isEmpty()) Text("Search contacts", color = Label3, fontSize = 15.sp)
                    inner()
                }
            )
        }
        LazyColumn(Modifier.padding(top = 8.dp)) {
            items(filtered) { c ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { repo.placeCall(PhoneNumberUtils.normalize(c.number)) }
                        .padding(horizontal = 22.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0x3334C759))
                            .border(1.dp, GlassBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            c.name.take(1).uppercase().ifBlank { "#" },
                            color = Green,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(c.name.ifBlank { c.number }, color = Label, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                        Text(PhoneNumberUtils.display(c.number, prefs.numeralStyle), color = Label2, fontSize = 12.sp)
                    }
                    Icon(Icons.Filled.Call, null, tint = Green, modifier = Modifier.size(22.dp))
                }
                HorizontalDivider(color = Color(0x1FFFFFFF))
            }
        }
    }
}

@Composable
private fun MorePane(
    prefs: PhonePrefs,
    repo: PhoneRepository,
    onAppearance: () -> Unit,
    onRingtone: () -> Unit,
    onBack: (() -> Unit)?
) {
    val context = LocalContext.current
    var isDefault by remember { mutableStateOf(repo.isDefaultDialer()) }
    var enabled by remember { mutableStateOf(prefs.phoneEnabled) }
    var quiet by remember { mutableStateOf(prefs.quietHoursEnabled) }
    var numeral by remember { mutableStateOf(prefs.numeralStyle) }
    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        isDefault = repo.isDefaultDialer()
    }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp)
            .padding(bottom = 110.dp)
    ) {
        Text("Settings", color = Label, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, modifier = Modifier.padding(vertical = 16.dp))

        GlassSettingsCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Suhood Phone Icon", color = Label, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text("Separate launcher icon for the dialer", color = Label2, fontSize = 12.sp)
                }
                Switch(checked = enabled, onCheckedChange = {
                    enabled = it
                    prefs.phoneEnabled = it
                    setPhoneLauncherEnabled(context, it)
                })
            }
        }
        Spacer(Modifier.height(12.dp))
        GlassSettingsCard(onClick = onAppearance) {
            Text("Call Appearance Studio", color = Label, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text("Wallpapers, button styles, preview", color = Label2, fontSize = 12.sp)
        }
        Spacer(Modifier.height(12.dp))
        GlassSettingsCard(onClick = onRingtone) {
            Text("Ringtone", color = Label, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(prefs.ringtoneTitle ?: "System default · search device nasheeds", color = Label2, fontSize = 12.sp)
        }
        Spacer(Modifier.height(12.dp))
        GlassSettingsCard {
            Text("Default Phone App", color = Label, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(
                if (isDefault) "Suhood is the default Phone app" else "Required for in-call UI",
                color = Label2,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { requestDefaultDialer(context, roleLauncher) },
                colors = ButtonDefaults.buttonColors(containerColor = Green)
            ) {
                Text(if (isDefault) "Manage default apps" else "Request default Phone role")
            }
        }
        Spacer(Modifier.height(12.dp))
        GlassSettingsCard {
            Text("Permissions", color = Label, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text("Contacts, call log, phone — on-device only", color = Label2, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            Button(onClick = {
                permLauncher.launch(
                    arrayOf(
                        Manifest.permission.READ_CONTACTS,
                        Manifest.permission.READ_CALL_LOG,
                        Manifest.permission.CALL_PHONE,
                        Manifest.permission.READ_PHONE_STATE
                    )
                )
            }) { Text("Grant permissions") }
        }
        Spacer(Modifier.height(12.dp))
        GlassSettingsCard {
            Text("Dial-pad Numerals", color = Label, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Spacer(Modifier.height(8.dp))
            NumeralStyle.entries.forEach { style ->
                Row(
                    Modifier.fillMaxWidth().clickable {
                        numeral = style
                        prefs.numeralStyle = style
                    }.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = numeral == style, onClick = {
                        numeral = style
                        prefs.numeralStyle = style
                    })
                    Text(
                        when (style) {
                            NumeralStyle.WESTERN -> "Western  0123456789"
                            NumeralStyle.ARABIC_INDIC -> "Arabic-Indic  ٠١٢٣٤٥٦٧٨٩"
                            NumeralStyle.PERSIAN -> "Persian  ۰۱۲۳۴۵۶۷۸۹"
                        },
                        color = Label,
                        fontSize = 14.sp
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        GlassSettingsCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Quiet Hours", color = Label, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text("${prefs.quietStartHour}:00 – ${prefs.quietEndHour}:00", color = Label2, fontSize = 12.sp)
                }
                Switch(checked = quiet, onCheckedChange = {
                    quiet = it
                    prefs.quietHoursEnabled = it
                })
            }
        }
        Spacer(Modifier.height(12.dp))
        GlassSettingsCard {
            Text("Blocked Numbers", color = Label, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            val blocked = prefs.blocked()
            Text(if (blocked.isEmpty()) "None blocked" else blocked.joinToString(), color = Label3, fontSize = 12.sp)
        }
        Text(
            PhoneAssetManifest.NOTE,
            color = Label3,
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        )
    }
}

@Composable
private fun GlassSettingsCard(onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(GlassFill)
            .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallAppearanceStudio(prefs: PhonePrefs, onBack: () -> Unit) {
    val context = LocalContext.current
    val wallpapers = remember { PhoneWallpapers.list(context) }
    var selectedWp by remember { mutableStateOf(prefs.wallpaperAsset) }
    var btnStyle by remember { mutableStateOf(prefs.callButtonStyle) }
    var incoming by remember { mutableStateOf(prefs.incomingTheme) }
    var tab by remember { mutableStateOf(0) } // 0 wallpapers 1 themes 2 buttons

    Scaffold(
        containerColor = PhoneBg,
        topBar = {
            TopAppBar(
                title = { Text("Appearance Studio", color = Label, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Label)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PhoneBg)
            )
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            Row(Modifier.padding(horizontal = 12.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Wallpapers", "Colour themes", "Buttons").forEachIndexed { i, label ->
                    FilterChip(selected = tab == i, onClick = { tab = i }, label = { Text(label) })
                }
            }
            when (tab) {
                0 -> {
                    Text("${wallpapers.size} HD wallpapers · offline", color = Label2, fontSize = 12.sp, modifier = Modifier.padding(12.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(wallpapers) { name ->
                            val selected = selectedWp == name
                            Box(
                                Modifier
                                    .aspectRatio(9f / 16f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(2.dp, if (selected) Green else GlassBorder, RoundedCornerShape(12.dp))
                                    .clickable {
                                        selectedWp = name
                                        prefs.wallpaperAsset = name
                                        Toast.makeText(context, "Wallpaper applied", Toast.LENGTH_SHORT).show()
                                    }
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data("file:///android_asset/phone_wallpapers/$name")
                                        .build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
                1 -> {
                    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(CallThemeId.entries) { id ->
                            val c = CallThemes.colors(id)
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(GlassFill)
                                    .clickable {
                                        incoming = id
                                        prefs.incomingTheme = id
                                        prefs.ongoingTheme = id
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(c.background))
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(id.label, color = Label, fontWeight = FontWeight.Medium)
                                    Text(id.category, color = Label2, fontSize = 12.sp)
                                }
                                if (incoming == id) Icon(Icons.Filled.Check, null, tint = Green)
                            }
                        }
                    }
                }
                else -> {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        CallButtonStyle.entries.forEach { style ->
                            GlassSettingsCard(onClick = {
                                btnStyle = style
                                prefs.callButtonStyle = style
                            }) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(style.name.replace('_', ' '), color = Label, modifier = Modifier.weight(1f))
                                    if (btnStyle == style) Icon(Icons.Filled.Check, null, tint = Green)
                                }
                            }
                        }
                        Button(
                            onClick = {
                                context.startActivity(PhoneCallActivity.previewIntent(context, prefs.incomingTheme))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Green),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Preview incoming / ongoing call")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RingtonePickerScreen(prefs: PhonePrefs, onBack: () -> Unit) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("nasheed") }
    var results by remember { mutableStateOf<List<Pair<String, Uri>>>(emptyList()) }
    var playing by remember { mutableStateOf<Uri?>(null) }
    var ringtone by remember { mutableStateOf<android.media.Ringtone?>(null) }

    fun load() {
        results = runCatching {
            val list = mutableListOf<Pair<String, Uri>>()
            val rm = RingtoneManager(context)
            rm.setType(RingtoneManager.TYPE_RINGTONE or RingtoneManager.TYPE_NOTIFICATION)
            val cursor = rm.cursor
            while (cursor.moveToNext()) {
                val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX) ?: continue
                val uri = rm.getRingtoneUri(cursor.position) ?: continue
                if (query.isBlank() || title.contains(query, true)) {
                    list += title to uri
                }
            }
            list.take(80)
        }.getOrDefault(emptyList())
    }

    LaunchedEffect(query) { load() }

    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            prefs.ringtoneUri = uri.toString()
            prefs.ringtoneTitle = uri.lastPathSegment ?: "Custom"
            Toast.makeText(context, "Ringtone saved", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        containerColor = PhoneBg,
        topBar = {
            TopAppBar(
                title = { Text("Ringtone", color = Label) },
                navigationIcon = {
                    IconButton(onClick = {
                        ringtone?.stop()
                        onBack()
                    }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Label) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PhoneBg)
            )
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(16.dp)) {
            Text(
                "Search ringtones already on this phone (including nasheed/islamic if installed). " +
                    "Zedge files you downloaded work here offline — we do not stream Zedge inside the app (copyright).",
                color = Label2,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search: nasheed, islamic…") },
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { pickFile.launch(arrayOf("audio/*")) }) { Text("Pick audio file") }
                TextButton(onClick = {
                    prefs.ringtoneUri = null
                    prefs.ringtoneTitle = null
                    Toast.makeText(context, "System default", Toast.LENGTH_SHORT).show()
                }) { Text("Reset") }
            }
            Text("Selected: ${prefs.ringtoneTitle ?: "System default"}", color = Green, fontSize = 13.sp, modifier = Modifier.padding(vertical = 8.dp))
            LazyColumn {
                items(results) { (title, uri) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                ringtone?.stop()
                                val r = RingtoneManager.getRingtone(context, uri)
                                ringtone = r
                                r?.play()
                                playing = uri
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.MusicNote, null, tint = Label2)
                        Spacer(Modifier.width(12.dp))
                        Text(title, color = Label, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        TextButton(onClick = {
                            prefs.ringtoneUri = uri.toString()
                            prefs.ringtoneTitle = title
                            Toast.makeText(context, "Set as Suhood ringtone", Toast.LENGTH_SHORT).show()
                        }) { Text("Set") }
                    }
                    HorizontalDivider(color = Color(0x1FFFFFFF))
                }
            }
        }
    }
}

fun setPhoneLauncherEnabled(context: Context, enabled: Boolean) {
    val pm = context.packageManager
    val component = ComponentName(context, PhoneLauncherActivity::class.java)
    pm.setComponentEnabledSetting(
        component,
        if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
        PackageManager.DONT_KILL_APP
    )
}

fun requestDefaultDialer(context: Context, launcher: ManagedActivityResultLauncher<Intent, ActivityResult>) {
    try {
        setPhoneLauncherEnabled(context, true)
        PhonePrefs(context).phoneEnabled = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val rm = context.getSystemService(RoleManager::class.java)
            if (rm != null) {
                if (!rm.isRoleAvailable(RoleManager.ROLE_DIALER)) {
                    Toast.makeText(context, "Phone role is not available on this device", Toast.LENGTH_LONG).show()
                    openDefaultAppsSettings(context, launcher)
                    return
                }
                if (rm.isRoleHeld(RoleManager.ROLE_DIALER)) {
                    Toast.makeText(context, "Suhood is already the default Phone app", Toast.LENGTH_SHORT).show()
                    openDefaultAppsSettings(context, launcher)
                    return
                }
                launcher.launch(rm.createRequestRoleIntent(RoleManager.ROLE_DIALER))
                return
            }
        }
        @Suppress("DEPRECATION")
        val change = Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
            putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, context.packageName)
        }
        if (change.resolveActivity(context.packageManager) != null) {
            launcher.launch(change)
        } else {
            openDefaultAppsSettings(context, launcher)
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Could not open Phone role request: ${e.message ?: "error"}", Toast.LENGTH_LONG).show()
        openDefaultAppsSettings(context, launcher)
    }
}

fun openDefaultAppsSettings(context: Context, launcher: ManagedActivityResultLauncher<Intent, ActivityResult>) {
    try {
        launcher.launch(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
    } catch (_: Exception) {
        try {
            context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {
            Toast.makeText(context, "Open Settings → Apps → Default apps → Phone app", Toast.LENGTH_LONG).show()
        }
    }
}

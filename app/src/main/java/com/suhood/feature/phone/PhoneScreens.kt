package com.suhood.feature.phone

import android.Manifest
import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.CallLog
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suhood.core.designsystem.components.GlassCard
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class PhoneTab { KEYPAD, RECENTS, CONTACTS, MORE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneRoot(onBack: (() -> Unit)? = null, openAppearance: Boolean = false) {
    val context = LocalContext.current
    val prefs = remember { PhonePrefs(context) }
    val repo = remember { PhoneRepository(context) }
    var tab by remember { mutableStateOf(PhoneTab.KEYPAD) }
    var showAppearance by remember { mutableStateOf(openAppearance) }

    Box(Modifier.fillMaxSize().background(Color(0xFF08090B))) {
        val c = CallThemes.colors(prefs.dialPadTheme)
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Brush.verticalGradient(listOf(c.background, c.surface, c.background)))
            drawPath(Path().apply {
                addOval(Rect(size.width * 0.55f, -size.height * 0.1f, size.width * 1.15f, size.height * 0.4f))
            }, c.accent.copy(alpha = 0.07f))
        }
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(bottom = 72.dp)) {
            Row(Modifier.fillMaxWidth().padding(12.dp, 8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color(0xFFF5F6F7))
                }
                Column(Modifier.weight(1f)) {
                    Text("Suhood Phone", color = Color(0xFFF5F6F7), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text(
                        if (repo.isDefaultDialer()) "Default Phone app active"
                        else "Integrated dialer · optional default Phone role",
                        color = Color(0xFFA8ADB5), fontSize = 11.sp
                    )
                }
                IconButton(onClick = { showAppearance = true }) {
                    Icon(Icons.Filled.Palette, null, tint = Color(0xFFC7CBD1))
                }
            }
            when (tab) {
                PhoneTab.KEYPAD -> DialPadScreen(prefs, repo)
                PhoneTab.RECENTS -> RecentsScreen(prefs, repo)
                PhoneTab.CONTACTS -> ContactsScreen(prefs, repo)
                PhoneTab.MORE -> PhoneMoreScreen(prefs, repo) { showAppearance = true }
            }
        }
        NavigationBar(Modifier.align(Alignment.BottomCenter), containerColor = Color(0xEE111316)) {
            listOf(
                PhoneTab.KEYPAD to (Icons.Filled.Dialpad to "Keypad"),
                PhoneTab.RECENTS to (Icons.Filled.History to "Recents"),
                PhoneTab.CONTACTS to (Icons.Filled.Contacts to "Contacts"),
                PhoneTab.MORE to (Icons.Filled.Tune to "More")
            ).forEach { (t, pair) ->
                NavigationBarItem(
                    selected = tab == t,
                    onClick = { tab = t },
                    icon = { Icon(pair.first, null) },
                    label = { Text(pair.second, fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF3DDC97),
                        selectedTextColor = Color(0xFF3DDC97),
                        indicatorColor = Color(0x223DDC97),
                        unselectedIconColor = Color(0xFFA8ADB5),
                        unselectedTextColor = Color(0xFFA8ADB5)
                    )
                )
            }
        }
        if (showAppearance) CallAppearanceStudio(prefs) { showAppearance = false }
    }
}

@Composable
private fun DialPadScreen(prefs: PhonePrefs, repo: PhoneRepository) {
    val context = LocalContext.current
    var digits by remember { mutableStateOf("") }
    val style = prefs.numeralStyle
    val theme = CallThemes.colors(prefs.dialPadTheme)
    val callPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted && digits.isNotBlank()) repo.placeCall(digits)
        else Toast.makeText(context, "Opening system dialer if permission denied", Toast.LENGTH_SHORT).show()
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(20.dp))
        Text(
            PhoneNumberUtils.display(digits, style).ifBlank { "Enter number" },
            color = if (digits.isBlank()) theme.secondary else theme.text,
            fontSize = if (digits.length > 14) 26.sp else 34.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        if (digits.isNotBlank() && prefs.isBlocked(digits)) {
            Text("This number is blocked", color = Color(0xFFCE1126), fontSize = 12.sp)
        }
        Spacer(Modifier.height(18.dp))
        listOf(listOf("1","2","3"), listOf("4","5","6"), listOf("7","8","9"), listOf("*","0","#")).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                row.forEach { key ->
                    Box(
                        Modifier.size(74.dp).clip(CircleShape).background(theme.surface.copy(alpha = 0.9f))
                            .border(0.5.dp, Color.White.copy(0.08f), CircleShape)
                            .clickable { digits = PhoneNumberUtils.normalize(digits + key) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(PhoneNumberUtils.display(key, style), color = theme.text, fontSize = 28.sp)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = {
                if (digits.isNotBlank()) { prefs.block(digits); Toast.makeText(context, "Blocked", Toast.LENGTH_SHORT).show() }
            }) { Icon(Icons.Filled.Block, null, tint = theme.secondary) }
            Box(
                Modifier.size(70.dp).clip(CircleShape).background(Color(0xFF3DDC97)).clickable {
                    val n = PhoneNumberUtils.normalize(digits)
                    when {
                        !PhoneNumberUtils.isValidForDial(n) -> Toast.makeText(context, "Enter a number", Toast.LENGTH_SHORT).show()
                        prefs.isBlocked(n) -> Toast.makeText(context, "Number is blocked", Toast.LENGTH_SHORT).show()
                        !repo.hasCallPermission() -> callPerm.launch(Manifest.permission.CALL_PHONE)
                        !repo.placeCall(n) -> Toast.makeText(context, "Unable to place call", Toast.LENGTH_SHORT).show()
                    }
                },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Filled.Call, null, tint = Color(0xFF08090B), modifier = Modifier.size(30.dp)) }
            IconButton(onClick = { if (digits.isNotEmpty()) digits = digits.dropLast(1) }) {
                Icon(Icons.AutoMirrored.Filled.Backspace, null, tint = theme.secondary)
            }
        }
    }
}

@Composable
private fun RecentsScreen(prefs: PhonePrefs, repo: PhoneRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var filter by remember { mutableStateOf<String?>(null) }
    var list by remember { mutableStateOf<List<RecentCall>>(emptyList()) }
    var needPerm by remember { mutableStateOf(!repo.hasCallLogPermission()) }
    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        needPerm = !ok
        if (ok) scope.launch { list = repo.loadRecents(filter) }
    }
    LaunchedEffect(filter) {
        if (repo.hasCallLogPermission()) { needPerm = false; list = repo.loadRecents(filter) }
    }
    val fmt = remember { SimpleDateFormat("dd MMM · HH:mm", Locale.getDefault()) }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(null to "All", "missed" to "Missed", "incoming" to "In", "outgoing" to "Out").forEach { (f, label) ->
                FilterChip(selected = filter == f, onClick = { filter = f }, label = { Text(label) })
            }
        }
        when {
            needPerm -> {
                Spacer(Modifier.height(16.dp))
                GlassCard(Modifier.fillMaxWidth()) {
                    Text("Call log permission", color = Color(0xFFF5F6F7), fontWeight = FontWeight.SemiBold)
                    Text("On-device only. Never uploaded.", color = Color(0xFFA8ADB5), fontSize = 12.sp)
                    Button(onClick = { perm.launch(Manifest.permission.READ_CALL_LOG) }) { Text("Allow") }
                }
            }
            list.isEmpty() -> Text("No recent calls", color = Color(0xFFA8ADB5), modifier = Modifier.padding(24.dp))
            else -> LazyColumn(contentPadding = PaddingValues(vertical = 12.dp)) {
                items(list, key = { it.id }) { call ->
                    val title = call.name?.takeIf { it.isNotBlank() } ?: PhoneNumberUtils.display(call.number, prefs.numeralStyle)
                    Row(Modifier.fillMaxWidth().clickable { repo.placeCall(call.number) }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            when (call.type) {
                                CallLog.Calls.MISSED_TYPE -> Icons.Filled.CallMissed
                                CallLog.Calls.OUTGOING_TYPE -> Icons.Filled.CallMade
                                else -> Icons.Filled.CallReceived
                            }, null,
                            tint = if (call.type == CallLog.Calls.MISSED_TYPE) Color(0xFFCE1126) else Color(0xFF3DDC97)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(title, color = Color(0xFFF5F6F7), fontWeight = FontWeight.Medium)
                            Text("${call.typeLabel} · ${fmt.format(Date(call.dateMs))}", color = Color(0xFFA8ADB5), fontSize = 12.sp)
                        }
                        IconButton(onClick = { prefs.block(call.number); Toast.makeText(context, "Blocked", Toast.LENGTH_SHORT).show() }) {
                            Icon(Icons.Filled.Block, null, tint = Color(0xFFA8ADB5))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactsScreen(prefs: PhonePrefs, repo: PhoneRepository) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var list by remember { mutableStateOf<List<PhoneContact>>(emptyList()) }
    var needPerm by remember { mutableStateOf(!repo.hasContactsPermission()) }
    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        needPerm = !ok
        if (ok) scope.launch { list = repo.loadContacts(query) }
    }
    LaunchedEffect(query) {
        if (repo.hasContactsPermission()) { needPerm = false; list = repo.loadContacts(query) }
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Search contacts") }, singleLine = true)
        if (needPerm) {
            Spacer(Modifier.height(16.dp))
            GlassCard(Modifier.fillMaxWidth()) {
                Text("Contacts permission", color = Color(0xFFF5F6F7), fontWeight = FontWeight.SemiBold)
                Text("Read on-device only. Never uploaded.", color = Color(0xFFA8ADB5), fontSize = 12.sp)
                Button(onClick = { perm.launch(Manifest.permission.READ_CONTACTS) }) { Text("Allow") }
            }
        } else LazyColumn(contentPadding = PaddingValues(vertical = 12.dp)) {
            items(list, key = { it.id + it.number }) { c ->
                Row(Modifier.fillMaxWidth().clickable { repo.placeCall(c.number) }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF1A1C20)), contentAlignment = Alignment.Center) {
                        Text(c.name.take(1).uppercase(), color = Color(0xFF3DDC97), fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(c.name, color = Color(0xFFF5F6F7))
                        Text(PhoneNumberUtils.display(c.number, prefs.numeralStyle), color = Color(0xFFA8ADB5), fontSize = 12.sp)
                    }
                    if (c.starred) Icon(Icons.Filled.Star, null, tint = Color(0xFFE0C36A), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun PhoneMoreScreen(prefs: PhonePrefs, repo: PhoneRepository, onAppearance: () -> Unit) {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(prefs.enabled) }
    var numerals by remember { mutableStateOf(prefs.numeralStyle) }
    var quiet by remember { mutableStateOf(prefs.quietHoursEnabled) }
    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GlassCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Enable Suhood Phone icon", color = Color(0xFFF5F6F7), fontWeight = FontWeight.SemiBold)
                    Text("Separate launcher icon for the dialer", color = Color(0xFFA8ADB5), fontSize = 12.sp)
                }
                Switch(checked = enabled, onCheckedChange = {
                    enabled = it; prefs.enabled = it
                    setPhoneLauncherEnabled(context, it)
                })
            }
        }
        GlassCard(Modifier.fillMaxWidth().clickable { requestDefaultDialer(context, roleLauncher) }) {
            Text("Default Phone app", color = Color(0xFFF5F6F7), fontWeight = FontWeight.SemiBold)
            Text(if (repo.isDefaultDialer()) "Suhood is the default Phone app" else "Tap to request Phone role (you confirm)", color = Color(0xFFA8ADB5), fontSize = 12.sp)
        }
        GlassCard(Modifier.fillMaxWidth().clickable(onClick = onAppearance)) {
            Text("Call Appearance Studio", color = Color(0xFFF5F6F7), fontWeight = FontWeight.SemiBold)
            Text("Themes · flags · geometric presets", color = Color(0xFFA8ADB5), fontSize = 12.sp)
        }
        GlassCard(Modifier.fillMaxWidth()) {
            Text("Dial-pad numerals", color = Color(0xFFF5F6F7), fontWeight = FontWeight.SemiBold)
            NumeralStyle.entries.forEach { s ->
                Row(Modifier.fillMaxWidth().clickable { numerals = s; prefs.numeralStyle = s }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = numerals == s, onClick = { numerals = s; prefs.numeralStyle = s })
                    Text("${s.name} · ${PhoneNumberUtils.display("0123456789", s)}", color = Color(0xFFC7CBD1), fontSize = 13.sp)
                }
            }
        }
        GlassCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Quiet hours preference", color = Color(0xFFF5F6F7), fontWeight = FontWeight.SemiBold)
                    Text("${prefs.quietStartHour}:00 – ${prefs.quietEndHour}:00", color = Color(0xFFA8ADB5), fontSize = 12.sp)
                }
                Switch(checked = quiet, onCheckedChange = { quiet = it; prefs.quietHoursEnabled = it })
            }
        }
        GlassCard(Modifier.fillMaxWidth()) {
            Text("Blocked numbers", color = Color(0xFFF5F6F7), fontWeight = FontWeight.SemiBold)
            val blocked = prefs.blocked().toList()
            if (blocked.isEmpty()) Text("None", color = Color(0xFFA8ADB5), fontSize = 12.sp)
            else blocked.forEach { n ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(PhoneNumberUtils.display(n, prefs.numeralStyle), color = Color(0xFFC7CBD1), modifier = Modifier.weight(1f))
                    TextButton(onClick = { prefs.unblock(n) }) { Text("Unblock") }
                }
            }
        }
        Text(PhoneAssetManifest.NOTE, color = Color(0xFF6A6E76), fontSize = 10.sp)
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallAppearanceStudio(prefs: PhonePrefs, onBack: () -> Unit) {
    val colors = suhoodColors
    var selected by remember { mutableStateOf(prefs.incomingTheme) }
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("All") }
    val categories = listOf("All") + CallThemes.allCategories()
    val filtered = CallThemeId.entries.filter {
        (category == "All" || it.category == category) &&
            (query.isBlank() || it.label.contains(query, true) || it.category.contains(query, true))
    }
    Scaffold(
        containerColor = Color(0xFF08090B),
        topBar = {
            TopAppBar(
                title = { Text("Call Appearance Studio", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF08090B))
            )
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(horizontal = 16.dp)) {
            // Live preview
            val c = CallThemes.colors(selected)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.verticalGradient(listOf(c.background, c.surface, c.accent.copy(alpha = 0.35f))))
                    .border(0.5.dp, Color.White.copy(0.12f), RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text("Incoming preview", color = c.secondary, fontSize = 12.sp)
                    Text("Amina · ${PhoneNumberUtils.display("+1 555 0100", prefs.numeralStyle)}", color = c.text, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(selected.label, color = c.accent, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search themes…") },
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.forEach { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text(cat) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                items(filtered, key = { it.name }) { theme ->
                    val tc = CallThemes.colors(theme)
                    val isFav = theme.name in prefs.favoriteThemes
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(tc.surface)
                            .border(
                                if (selected == theme) 1.5.dp else 0.5.dp,
                                if (selected == theme) tc.accent else Color.White.copy(0.08f),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { selected = theme }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Brush.linearGradient(listOf(tc.background, tc.accent.copy(alpha = 0.55f))))
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(theme.label, color = Color.White, fontWeight = FontWeight.Medium)
                            Text(theme.category, color = Color(0xFFA8ADB5), fontSize = 11.sp)
                        }
                        IconButton(onClick = { prefs.toggleFavoriteTheme(theme) }) {
                            Icon(
                                if (isFav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                null,
                                tint = if (isFav) Color(0xFFFF8A80) else Color.White
                            )
                        }
                        TextButton(onClick = {
                            selected = theme
                            prefs.incomingTheme = theme
                            prefs.dialPadTheme = theme
                            prefs.ongoingTheme = theme
                            Toast.makeText(LocalContext.current, "Applied ${theme.label}", Toast.LENGTH_SHORT).show()
                        }) { Text("Apply") }
                    }
                }
            }
            Text(PhoneAssetManifest.NOTE, color = Color(0xFFA8ADB5), fontSize = 11.sp, modifier = Modifier.padding(bottom = 12.dp))
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val rm = context.getSystemService(RoleManager::class.java)
            if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_DIALER) && !rm.isRoleHeld(RoleManager.ROLE_DIALER)) {
                launcher.launch(rm.createRequestRoleIntent(RoleManager.ROLE_DIALER))
                return
            }
        }
        launcher.launch(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
    } catch (_: Exception) {
        Toast.makeText(context, "Open system settings to change default Phone app", Toast.LENGTH_LONG).show()
    }
}

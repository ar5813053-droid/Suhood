package com.suhood.feature.phone

import androidx.compose.foundation.ExperimentalFoundationApi


import android.Manifest
import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.CallLog
import android.provider.Settings
import android.telecom.TelecomManager
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val PhoneBg = Color(0xFF000000)
private val Surface = Color(0xFF1C1C1E)
private val Surface2 = Color(0xFF2C2C2E)
private val Label = Color(0xFFFFFFFF)
private val Label2 = Color(0x99EBEBF5)
private val Label3 = Color(0x61EBEBF5)
private val Green = Color(0xFF34C759)
private val Red = Color(0xFFFF3B30)
private val Blue = Color(0xFF0A84FF)

private enum class PhoneTab { KEYPAD, RECENTS, CONTACTS, MORE }
private sealed class PhoneOverlay {
    data object None : PhoneOverlay()
    data object Appearance : PhoneOverlay()
    data object Setup : PhoneOverlay()
    data class ContactDetail(val contact: PhoneContact) : PhoneOverlay()
    data object AddContact : PhoneOverlay()
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PhoneRoot(onBack: (() -> Unit)? = null) {
    val context = LocalContext.current
    val prefs = remember { PhonePrefs(context) }
    val repo = remember { PhoneRepository(context) }
    var tab by remember { mutableStateOf(PhoneTab.KEYPAD) }
    var overlay by remember {
        mutableStateOf<PhoneOverlay>(
            if (prefs.setupComplete) PhoneOverlay.None else PhoneOverlay.Setup
        )
    }
    var digits by remember { mutableStateOf("") }
    var contacts by remember { mutableStateOf<List<PhoneContact>>(emptyList()) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var selectionMode by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun refreshContacts() {
        scope.launch {
            contacts = withContext(Dispatchers.IO) { repo.loadContacts() }
        }
    }

    when (val o = overlay) {
        is PhoneOverlay.Setup -> {
            PhoneSetupGate(
                repo = repo,
                prefs = prefs,
                onDone = { overlay = PhoneOverlay.None }
            )
            return
        }
        is PhoneOverlay.Appearance -> {
            CallAppearanceStudio(prefs, onBack = { overlay = PhoneOverlay.None })
            return
        }
        is PhoneOverlay.ContactDetail -> {
            ContactDetailScreen(
                contact = o.contact,
                prefs = prefs,
                repo = repo,
                onBack = { overlay = PhoneOverlay.None },
                onCall = {
                    repo.placeCall(o.contact.number)
                }
            )
            return
        }
        is PhoneOverlay.AddContact -> {
            AddContactScreen(
                repo = repo,
                onBack = { overlay = PhoneOverlay.None },
                onSaved = {
                    overlay = PhoneOverlay.None
                    refreshContacts()
                }
            )
            return
        }
        PhoneOverlay.None -> Unit
    }

    if (showDeleteConfirm) {
        val names = contacts.filter { it.id in selectedIds }.map { it.name }.take(12)
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete contacts?", color = Label) },
            text = {
                Column {
                    Text("Are you sure you want to delete?", color = Label2, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    names.forEach {
                        Text("• $it", color = Label3, fontSize = 12.sp)
                    }
                    if (selectedIds.size > names.size) {
                        Text("…and ${selectedIds.size - names.size} more", color = Label3, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val n = repo.deleteContacts(selectedIds)
                    Toast.makeText(context, "Deleted $n", Toast.LENGTH_SHORT).show()
                    selectedIds = emptySet()
                    selectionMode = false
                    showDeleteConfirm = false
                    refreshContacts()
                }) { Text("Delete", color = Red) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel", color = Blue) }
            },
            containerColor = Surface
        )
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(PhoneBg)
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
                PhoneTab.CONTACTS -> ContactsPane(
                    contacts = contacts,
                    prefs = prefs,
                    selectionMode = selectionMode,
                    selectedIds = selectedIds,
                    onLoad = { refreshContacts() },
                    onToggleSelect = { id ->
                        selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
                        if (selectedIds.isEmpty()) selectionMode = false
                    },
                    onLongPress = { id ->
                        selectionMode = true
                        selectedIds = selectedIds + id
                    },
                    onOpen = { c -> overlay = PhoneOverlay.ContactDetail(c) },
                    onAdd = { overlay = PhoneOverlay.AddContact },
                    onDeleteClick = {
                        if (selectedIds.isNotEmpty()) showDeleteConfirm = true
                    },
                    onCancelSelection = {
                        selectionMode = false
                        selectedIds = emptySet()
                    }
                )
                PhoneTab.MORE -> MorePane(
                    prefs = prefs,
                    repo = repo,
                    onAppearance = { overlay = PhoneOverlay.Appearance },
                    onBack = onBack
                )
            }
        }
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color(0xE6111111))
                .navigationBarsPadding()
                .padding(top = 10.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TabItem("Keypad", Icons.Filled.Dialpad, tab == PhoneTab.KEYPAD) { tab = PhoneTab.KEYPAD }
            TabItem("Recents", Icons.Filled.History, tab == PhoneTab.RECENTS) { tab = PhoneTab.RECENTS }
            TabItem("Contacts", Icons.Filled.People, tab == PhoneTab.CONTACTS) {
                tab = PhoneTab.CONTACTS
                refreshContacts()
            }
            TabItem("More", Icons.Filled.Menu, tab == PhoneTab.MORE) { tab = PhoneTab.MORE }
        }
    }
}

@Composable
private fun TabItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = if (selected) Green else Label2, modifier = Modifier.size(22.dp))
        Text(label, color = if (selected) Green else Label2, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun PhoneSetupGate(repo: PhoneRepository, prefs: PhonePrefs, onDone: () -> Unit) {
    val context = LocalContext.current
    var isDefault by remember { mutableStateOf(repo.isDefaultDialer()) }
    var step by remember { mutableIntStateOf(0) }
    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        isDefault = repo.isDefaultDialer()
        if (isDefault) step = 2
    }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { map ->
        if (map.values.any { it }) step = 1
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) isDefault = repo.isDefaultDialer()
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(PhoneBg)
            .statusBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.PhoneInTalk, null, tint = Green, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(16.dp))
        Text("Welcome to Suhood Phone", color = Label, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Grant permissions and set Suhood as your default Phone app to unlock the full call experience.",
            color = Label2,
            textAlign = TextAlign.Center,
            fontSize = 14.sp
        )
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = {
                permLauncher.launch(
                    arrayOf(
                        Manifest.permission.READ_CONTACTS,
                        Manifest.permission.WRITE_CONTACTS,
                        Manifest.permission.READ_CALL_LOG,
                        Manifest.permission.CALL_PHONE,
                        Manifest.permission.READ_PHONE_STATE,
                        Manifest.permission.ANSWER_PHONE_CALLS
                    )
                )
            },
            colors = ButtonDefaults.buttonColors(containerColor = Surface2),
            modifier = Modifier.fillMaxWidth()
        ) { Text("1. Allow permissions") }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { requestDefaultDialer(context, roleLauncher) },
            colors = ButtonDefaults.buttonColors(containerColor = Green),
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (isDefault) "Default Phone ✓" else "2. Set as default Phone app") }
        Spacer(Modifier.height(12.dp))
        Text(
            if (isDefault) "Default Phone is enabled"
            else "You must confirm in the system dialog",
            color = Label3,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                prefs.setupComplete = true
                onDone()
            },
            enabled = true,
            colors = ButtonDefaults.buttonColors(containerColor = Blue),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isDefault) "Continue" else "Continue anyway")
        }
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
    var recents by remember { mutableStateOf<List<RecentCall>>(emptyList()) }
    LaunchedEffect(Unit) {
        recents = withContext(Dispatchers.IO) { repo.loadRecents().take(5) }
    }
    val display = PhoneNumberUtils.display(digits, prefs.numeralStyle)
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(bottom = 96.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Label)
                }
            } else Spacer(Modifier.width(48.dp))
            Text("Suhood Phone", color = Label, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Spacer(Modifier.width(48.dp))
        }
        // Recent calls above keypad when no digits
        if (digits.isEmpty() && recents.isNotEmpty()) {
            Text("Recents", color = Label3, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
            recents.forEach { e ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            onDigits(e.number)
                        }
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (e.type == CallLog.Calls.MISSED_TYPE) Icons.Filled.CallMissed else Icons.Filled.Call,
                        null,
                        tint = if (e.type == CallLog.Calls.MISSED_TYPE) Red else Label2,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(e.name ?: PhoneNumberUtils.display(e.number, prefs.numeralStyle), color = Label, fontSize = 15.sp)
                        Text(
                            SimpleDateFormat("MMM d · HH:mm", Locale.getDefault()).format(Date(e.dateMs)),
                            color = Label3,
                            fontSize = 11.sp
                        )
                    }
                    IconButton(onClick = { repo.placeCall(e.number) }) {
                        Icon(Icons.Filled.Call, null, tint = Green, modifier = Modifier.size(20.dp))
                    }
                }
            }
        } else {
            Text(
                text = display.ifBlank { " " },
                color = Label,
                fontSize = if (digits.length > 12) 28.sp else 40.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp).heightIn(min = 52.dp)
            )
        }
        Spacer(Modifier.weight(1f, fill = digits.isEmpty() && recents.isNotEmpty()))
        val keys = listOf(
            "1" to "", "2" to "ABC", "3" to "DEF",
            "4" to "GHI", "5" to "JKL", "6" to "MNO",
            "7" to "PQRS", "8" to "TUV", "9" to "WXYZ",
            "*" to "", "0" to "+", "#" to ""
        )
        Column(Modifier.padding(horizontal = 32.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            keys.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    row.forEach { (num, alpha) ->
                        Box(
                            Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(Surface2)
                                .clickable {
                                    DialPadTone.play(num.first())
                                    onDigits(digits + num)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(PhoneNumberUtils.display(num, prefs.numeralStyle), color = Label, fontSize = 30.sp, fontWeight = FontWeight.Normal)
                                if (alpha.isNotEmpty()) Text(alpha, color = Label3, fontSize = 9.sp, letterSpacing = 1.sp)
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Box(Modifier.fillMaxWidth().padding(bottom = 8.dp), contentAlignment = Alignment.Center) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 40.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.size(48.dp))
                Box(
                    Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Green)
                        .clickable {
                            val n = PhoneNumberUtils.normalize(digits)
                            if (!PhoneNumberUtils.isValidForDial(n)) {
                                Toast.makeText(context, "Enter a number", Toast.LENGTH_SHORT).show()
                            } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                                Toast.makeText(context, "Grant Phone permission", Toast.LENGTH_LONG).show()
                            } else {
                                repo.placeCall(n)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Call, null, tint = Color.White, modifier = Modifier.size(32.dp))
                }
                IconButton(onClick = { if (digits.isNotEmpty()) onDigits(digits.dropLast(1)) }) {
                    Icon(Icons.AutoMirrored.Filled.Backspace, null, tint = if (digits.isNotEmpty()) Label else Label3)
                }
            }
        }
    }
}

@Composable
private fun RecentsPane(repo: PhoneRepository, prefs: PhonePrefs) {
    var filter by remember { mutableStateOf("All") }
    var logs by remember { mutableStateOf<List<RecentCall>>(emptyList()) }
    LaunchedEffect(Unit) { logs = withContext(Dispatchers.IO) { repo.loadRecents() } }
    val filtered = when (filter) {
        "Missed" -> logs.filter { it.type == CallLog.Calls.MISSED_TYPE }
        "Outgoing" -> logs.filter { it.type == CallLog.Calls.OUTGOING_TYPE }
        "Incoming" -> logs.filter {
            it.type != CallLog.Calls.OUTGOING_TYPE && it.type != CallLog.Calls.MISSED_TYPE
        }
        else -> logs
    }
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(bottom = 96.dp)) {
        Text("Recents", color = Label, fontWeight = FontWeight.Bold, fontSize = 28.sp, modifier = Modifier.padding(20.dp))
        Row(Modifier.padding(horizontal = 16.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("All", "Missed", "Outgoing", "Incoming").forEach { f ->
                val sel = filter == f
                Box(
                    Modifier.clip(RoundedCornerShape(20.dp)).background(if (sel) Green else Surface2)
                        .clickable { filter = f }.padding(horizontal = 14.dp, vertical = 6.dp)
                ) { Text(f, color = if (sel) Color.White else Label2, fontSize = 13.sp) }
            }
        }
        LazyColumn {
            items(filtered) { e ->
                Row(
                    Modifier.fillMaxWidth().clickable { repo.placeCall(e.number) }.padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (e.type == CallLog.Calls.MISSED_TYPE) Icons.Filled.CallMissed
                        else if (e.type == CallLog.Calls.OUTGOING_TYPE) Icons.Filled.CallMade
                        else Icons.Filled.CallReceived,
                        null,
                        tint = if (e.type == CallLog.Calls.MISSED_TYPE) Red else Label2,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(e.name ?: PhoneNumberUtils.display(e.number, prefs.numeralStyle), color = if (e.type == CallLog.Calls.MISSED_TYPE) Red else Label, fontSize = 16.sp)
                        Text(SimpleDateFormat("MMM d · HH:mm", Locale.getDefault()).format(Date(e.dateMs)), color = Label3, fontSize = 12.sp)
                    }
                }
                HorizontalDivider(color = Color(0x22FFFFFF))
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun ContactsPane(
    contacts: List<PhoneContact>,
    prefs: PhonePrefs,
    selectionMode: Boolean,
    selectedIds: Set<String>,
    onLoad: () -> Unit,
    onToggleSelect: (String) -> Unit,
    onLongPress: (String) -> Unit,
    onOpen: (PhoneContact) -> Unit,
    onAdd: () -> Unit,
    onDeleteClick: () -> Unit,
    onCancelSelection: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { onLoad() }
    val filtered = contacts.filter {
        query.isBlank() || it.name.contains(query, true) || it.number.contains(query)
    }
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(bottom = 96.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectionMode) {
                TextButton(onClick = onCancelSelection) { Text("Cancel", color = Blue) }
                Text("${selectedIds.size} selected", color = Label, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                TextButton(onClick = onDeleteClick) { Text("Delete", color = Red) }
            } else {
                Text("Contacts", color = Label, fontWeight = FontWeight.Bold, fontSize = 28.sp, modifier = Modifier.weight(1f))
                IconButton(onClick = onAdd) {
                    Icon(Icons.Filled.AddCircle, null, tint = Green, modifier = Modifier.size(32.dp))
                }
            }
        }
        Row(
            Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Surface2)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Search, null, tint = Label3, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                textStyle = LocalTextStyle.current.copy(color = Label, fontSize = 15.sp),
                modifier = Modifier.weight(1f),
                singleLine = true,
                decorationBox = { inner ->
                    if (query.isEmpty()) Text("Name or number", color = Label3, fontSize = 15.sp)
                    inner()
                }
            )
        }
        LazyColumn(Modifier.padding(top = 8.dp)) {
            items(filtered, key = { it.id + it.number }) { c ->
                val selected = c.id in selectedIds
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(if (selected) Color(0x330A84FF) else Color.Transparent)
                        .combinedClickable(
                            onClick = {
                                if (selectionMode) onToggleSelect(c.id) else onOpen(c)
                            },
                            onLongClick = { onLongPress(c.id) }
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(44.dp).clip(CircleShape).background(Surface2),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(c.name.take(1).uppercase().ifBlank { "#" }, color = Label, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(c.name.ifBlank { c.number }, color = Label, fontSize = 16.sp)
                        Text(PhoneNumberUtils.display(c.number, prefs.numeralStyle), color = Label2, fontSize = 13.sp)
                    }
                }
                HorizontalDivider(color = Color(0x22FFFFFF), modifier = Modifier.padding(start = 74.dp))
            }
        }
    }
}

@Composable
private fun ContactDetailScreen(
    contact: PhoneContact,
    prefs: PhonePrefs,
    repo: PhoneRepository,
    onBack: () -> Unit,
    onCall: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().background(PhoneBg).statusBarsPadding()
    ) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("Back", color = Blue) }
            Text("Contact", color = Label, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(64.dp))
        }
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(24.dp))
            Box(Modifier.size(96.dp).clip(CircleShape).background(Surface2), contentAlignment = Alignment.Center) {
                Text(contact.name.take(1).uppercase().ifBlank { "#" }, color = Label, fontSize = 40.sp)
            }
            Spacer(Modifier.height(16.dp))
            Text(contact.name, color = Label, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(PhoneNumberUtils.display(contact.number, prefs.numeralStyle), color = Label2, fontSize = 16.sp)
            Spacer(Modifier.height(28.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = onCall, modifier = Modifier.size(56.dp).clip(CircleShape).background(Green)) {
                        Icon(Icons.Filled.Call, null, tint = Color.White)
                    }
                    Text("Call", color = Label2, fontSize = 12.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = { /* message: open SMS */ }, modifier = Modifier.size(56.dp).clip(CircleShape).background(Surface2)) {
                        Icon(Icons.Filled.Message, null, tint = Label)
                    }
                    Text("Message", color = Label2, fontSize = 12.sp)
                }
            }
        }
        Spacer(Modifier.height(32.dp))
        Column(Modifier.padding(horizontal = 16.dp).clip(RoundedCornerShape(12.dp)).background(Surface)) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("mobile", color = Label3, fontSize = 12.sp)
                    Text(PhoneNumberUtils.display(contact.number, prefs.numeralStyle), color = Blue, fontSize = 17.sp)
                }
            }
        }
    }
}

@Composable
private fun AddContactScreen(repo: PhoneRepository, onBack: () -> Unit, onSaved: () -> Unit) {
    val context = LocalContext.current
    var first by remember { mutableStateOf("") }
    var last by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().background(PhoneBg).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("Cancel", color = Blue) }
            Text("New Contact", color = Label, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold)
            TextButton(onClick = {
                if (phone.isBlank() && first.isBlank()) {
                    Toast.makeText(context, "Enter a name or number", Toast.LENGTH_SHORT).show()
                    return@TextButton
                }
                val ok = repo.addContact(first, last, company, phone, email)
                if (ok) {
                    Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                    onSaved()
                } else {
                    Toast.makeText(context, "Need Contacts write permission", Toast.LENGTH_LONG).show()
                }
            }) { Text("Done", color = Blue, fontWeight = FontWeight.SemiBold) }
        }
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(16.dp))
            Box(Modifier.size(88.dp).clip(CircleShape).background(Surface2), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Person, null, tint = Label3, modifier = Modifier.size(48.dp))
            }
            Text("Add Photo", color = Blue, fontSize = 14.sp, modifier = Modifier.padding(8.dp))
        }
        Spacer(Modifier.height(12.dp))
        FieldBlock {
            DarkField("First name", first) { first = it }
            HorizontalDivider(color = Color(0x22FFFFFF))
            DarkField("Last name", last) { last = it }
            HorizontalDivider(color = Color(0x22FFFFFF))
            DarkField("Company", company) { company = it }
        }
        Spacer(Modifier.height(16.dp))
        FieldBlock {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.AddCircle, null, tint = Green)
                Spacer(Modifier.width(12.dp))
                BasicTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    textStyle = LocalTextStyle.current.copy(color = Label, fontSize = 16.sp),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (phone.isEmpty()) Text("add phone", color = Label3)
                        inner()
                    }
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        FieldBlock {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.AddCircle, null, tint = Green)
                Spacer(Modifier.width(12.dp))
                BasicTextField(
                    value = email,
                    onValueChange = { email = it },
                    textStyle = LocalTextStyle.current.copy(color = Label, fontSize = 16.sp),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (email.isEmpty()) Text("add email", color = Label3)
                        inner()
                    }
                )
            }
        }
    }
}

@Composable
private fun FieldBlock(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp).clip(RoundedCornerShape(12.dp)).background(Surface), content = content)
}

@Composable
private fun DarkField(placeholder: String, value: String, onChange: (String) -> Unit) {
    BasicTextField(
        value = value,
        onValueChange = onChange,
        textStyle = LocalTextStyle.current.copy(color = Label, fontSize = 16.sp),
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        singleLine = true,
        decorationBox = { inner ->
            if (value.isEmpty()) Text(placeholder, color = Label3, fontSize = 16.sp)
            inner()
        }
    )
}

@Composable
private fun MorePane(
    prefs: PhonePrefs,
    repo: PhoneRepository,
    onAppearance: () -> Unit,
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
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) isDefault = repo.isDefaultDialer()
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp).padding(bottom = 110.dp)
    ) {
        Text("Settings", color = Label, fontWeight = FontWeight.Bold, fontSize = 28.sp, modifier = Modifier.padding(vertical = 12.dp))
        SettingsCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Suhood Phone icon", color = Label, fontWeight = FontWeight.SemiBold)
                    Text("Separate launcher icon", color = Label2, fontSize = 12.sp)
                }
                Switch(checked = enabled, onCheckedChange = {
                    enabled = it
                    prefs.phoneEnabled = it
                    setPhoneLauncherEnabled(context, it)
                })
            }
        }
        Spacer(Modifier.height(10.dp))
        SettingsCard(onClick = onAppearance) {
            Text("Call Appearance Studio", color = Label, fontWeight = FontWeight.SemiBold)
            Text("Wallpapers, themes, button styles", color = Label2, fontSize = 12.sp)
        }
        Spacer(Modifier.height(10.dp))
        SettingsCard {
            Text("Default Phone app", color = Label, fontWeight = FontWeight.SemiBold)
            Text(if (isDefault) "Suhood is the default Phone app" else "Required for full in-call UI", color = Label2, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            Button(onClick = { requestDefaultDialer(context, roleLauncher) }, colors = ButtonDefaults.buttonColors(containerColor = Green)) {
                Text(if (isDefault) "Manage default apps" else "Set as default Phone")
            }
        }
        Spacer(Modifier.height(10.dp))
        SettingsCard {
            Text("Permissions", color = Label, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Button(onClick = {
                permLauncher.launch(
                    arrayOf(
                        Manifest.permission.READ_CONTACTS,
                        Manifest.permission.WRITE_CONTACTS,
                        Manifest.permission.READ_CALL_LOG,
                        Manifest.permission.CALL_PHONE,
                        Manifest.permission.READ_PHONE_STATE
                    )
                )
            }) { Text("Grant permissions") }
        }
        Spacer(Modifier.height(10.dp))
        SettingsCard {
            Text("Dial-pad numerals", color = Label, fontWeight = FontWeight.SemiBold)
            NumeralStyle.entries.forEach { style ->
                Row(Modifier.fillMaxWidth().clickable { numeral = style; prefs.numeralStyle = style }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = numeral == style, onClick = { numeral = style; prefs.numeralStyle = style })
                    Text(
                        when (style) {
                            NumeralStyle.WESTERN -> "Western 0123456789"
                            NumeralStyle.ARABIC_INDIC -> "Arabic-Indic ٠١٢٣٤٥٦٧٨٩"
                            NumeralStyle.PERSIAN -> "Persian ۰۱۲۳۴۵۶۷۸۹"
                        },
                        color = Label
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        SettingsCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Quiet hours", color = Label, fontWeight = FontWeight.SemiBold)
                    Text("${prefs.quietStartHour}:00 – ${prefs.quietEndHour}:00", color = Label2, fontSize = 12.sp)
                }
                Switch(checked = quiet, onCheckedChange = { quiet = it; prefs.quietHoursEnabled = it })
            }
        }
        Spacer(Modifier.height(10.dp))
        SettingsCard {
            Text("Blocked numbers", color = Label, fontWeight = FontWeight.SemiBold)
            val b = prefs.blocked()
            Text(if (b.isEmpty()) "None" else b.joinToString(), color = Label3, fontSize = 12.sp)
        }
        Spacer(Modifier.height(10.dp))
        SettingsCard(onClick = {
            prefs.setupComplete = false
            Toast.makeText(context, "Setup will show next launch", Toast.LENGTH_SHORT).show()
        }) {
            Text("Run setup again", color = Blue)
        }
    }
}

@Composable
private fun SettingsCard(onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Surface)
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
    var tab by remember { mutableIntStateOf(0) }

    Scaffold(
        containerColor = PhoneBg,
        topBar = {
            TopAppBar(
                title = { Text("Appearance", color = Label) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Label) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PhoneBg)
            )
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            Row(Modifier.padding(12.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Wallpapers", "Themes", "Buttons").forEachIndexed { i, l ->
                    FilterChip(selected = tab == i, onClick = { tab = i }, label = { Text(l) })
                }
            }
            when (tab) {
                0 -> LazyVerticalGrid(columns = GridCells.Fixed(3), contentPadding = PaddingValues(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(wallpapers) { name ->
                        Box(
                            Modifier.aspectRatio(9f / 16f).clip(RoundedCornerShape(12.dp))
                                .border(2.dp, if (selectedWp == name) Green else Color.Transparent, RoundedCornerShape(12.dp))
                                .clickable {
                                    selectedWp = name
                                    prefs.wallpaperAsset = name
                                    Toast.makeText(context, "Applied", Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context).data("file:///android_asset/phone_wallpapers/$name").build(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        }
                    }
                }
                1 -> LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(CallThemeId.entries) { id ->
                        val c = CallThemes.colors(id)
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Surface).clickable {
                                incoming = id
                                prefs.incomingTheme = id
                                prefs.ongoingTheme = id
                            }.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(c.background))
                            Spacer(Modifier.width(12.dp))
                            Text(id.label, color = Label, modifier = Modifier.weight(1f))
                            if (incoming == id) Icon(Icons.Filled.Check, null, tint = Green)
                        }
                    }
                }
                else -> Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    CallButtonStyle.entries.forEach { style ->
                        SettingsCard(onClick = { btnStyle = style; prefs.callButtonStyle = style }) {
                            Row {
                                Text(style.name.replace('_', ' '), color = Label, modifier = Modifier.weight(1f))
                                if (btnStyle == style) Icon(Icons.Filled.Check, null, tint = Green)
                            }
                        }
                    }
                    Button(
                        onClick = { context.startActivity(PhoneCallActivity.previewIntent(context, prefs.incomingTheme)) },
                        colors = ButtonDefaults.buttonColors(containerColor = Green),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Preview call screen") }
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
        if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
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
                    Toast.makeText(context, "Phone role not available", Toast.LENGTH_LONG).show()
                    openDefaultAppsSettings(context, launcher)
                    return
                }
                if (rm.isRoleHeld(RoleManager.ROLE_DIALER)) {
                    Toast.makeText(context, "Already default Phone app", Toast.LENGTH_SHORT).show()
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
        if (change.resolveActivity(context.packageManager) != null) launcher.launch(change)
        else openDefaultAppsSettings(context, launcher)
    } catch (e: Exception) {
        Toast.makeText(context, e.message ?: "Error", Toast.LENGTH_LONG).show()
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
            Toast.makeText(context, "Settings → Default apps → Phone", Toast.LENGTH_LONG).show()
        }
    }
}

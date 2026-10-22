package com.suhood.feature.phone

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.suhood.core.designsystem.theme.SuhoodTypography
import com.suhood.core.designsystem.theme.suhoodColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneSettingsScreen(
    onBack: () -> Unit,
    onOpenPhone: () -> Unit,
    onOpenAppearance: () -> Unit
) {
    val colors = suhoodColors
    val context = LocalContext.current
    val prefs = remember { PhonePrefs(context) }
    val repo = remember { PhoneRepository(context) }
    var enabled by remember { mutableStateOf(prefs.phoneEnabled) }
    var quiet by remember { mutableStateOf(prefs.quietHoursEnabled) }
    var numeral by remember { mutableStateOf(prefs.numeralStyle) }
    var isDefault by remember { mutableStateOf(repo.isDefaultDialer()) }
    val sims = remember { repo.listSims() }
    var simId by remember { mutableIntStateOf(prefs.preferredSimId) }

    val roleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { isDefault = repo.isDefaultDialer() }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) {
                isDefault = repo.isDefaultDialer()
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Suhood Phone",
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background)
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PhonePanel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Call, contentDescription = null, tint = colors.accentLavender)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Enable Suhood Phone",
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary
                        )
                        Text(
                            "Shows a separate Phone launcher icon",
                            color = colors.textMuted,
                            style = SuhoodTypography.labelSmall
                        )
                    }
                    Switch(
                        checked = enabled,
                        onCheckedChange = {
                            enabled = it
                            prefs.phoneEnabled = it
                            setPhoneLauncherEnabled(context, it)
                        }
                    )
                }
            }

            PhonePanel(onClick = onOpenPhone) {
                Text("Open Suhood Phone", fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                Text(
                    "Dialer, contacts, recents",
                    color = colors.textMuted,
                    style = SuhoodTypography.labelSmall
                )
            }

            PhonePanel(onClick = onOpenAppearance) {
                Text(
                    "Call Appearance Studio",
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
                Text(
                    "Themes, flags, numerals — offline originals",
                    color = colors.textMuted,
                    style = SuhoodTypography.labelSmall
                )
            }

            PhonePanel {
                Text("Default Phone app", fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                Text(
                    if (isDefault) "Suhood is the default dialer"
                    else "Required for in-call UI and screening",
                    color = colors.textMuted,
                    style = SuhoodTypography.labelSmall
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = { requestDefaultDialer(context, roleLauncher) }) {
                    Text(
                        if (isDefault) "Open default-app settings"
                        else "Request default Phone role"
                    )
                }
            }

            PhonePanel {
                Text("Permissions", fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                Text(
                    "Contacts, call log, phone — used only on-device",
                    color = colors.textMuted,
                    style = SuhoodTypography.labelSmall
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        permLauncher.launch(
                            arrayOf(
                                Manifest.permission.READ_CONTACTS,
                                Manifest.permission.READ_CALL_LOG,
                                Manifest.permission.CALL_PHONE,
                                Manifest.permission.READ_PHONE_STATE
                            )
                        )
                    }
                ) {
                    Text("Grant phone permissions")
                }
            }

            PhonePanel {
                Text("Dial-pad numerals", fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                Text(
                    "Visual only — calls still use standard digits",
                    color = colors.textMuted,
                    style = SuhoodTypography.labelSmall
                )
                Spacer(Modifier.height(8.dp))
                NumeralStyle.entries.forEach { style ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                numeral = style
                                prefs.numeralStyle = style
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = numeral == style,
                            onClick = {
                                numeral = style
                                prefs.numeralStyle = style
                            }
                        )
                        Text(
                            when (style) {
                                NumeralStyle.WESTERN -> "Western 0123456789"
                                NumeralStyle.ARABIC_INDIC -> "Arabic-Indic ٠١٢٣٤٥٦٧٨٩"
                                NumeralStyle.PERSIAN -> "Persian / Urdu ۰۱۲۳۴۵۶۷۸۹"
                            },
                            color = colors.textPrimary
                        )
                    }
                }
            }

            if (sims.size > 1) {
                PhonePanel {
                    Text("Preferred SIM", fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                simId = -1
                                prefs.preferredSimId = -1
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = simId < 0,
                            onClick = {
                                simId = -1
                                prefs.preferredSimId = -1
                            }
                        )
                        Text("System default", color = colors.textPrimary)
                    }
                    sims.forEach { (idx, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    simId = idx
                                    prefs.preferredSimId = idx
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = simId == idx,
                                onClick = {
                                    simId = idx
                                    prefs.preferredSimId = idx
                                }
                            )
                            Text(label, color = colors.textPrimary)
                        }
                    }
                }
            }

            PhonePanel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Quiet hours", fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                        Text(
                            "Silence unknown incoming ${prefs.quietStartHour}:00–${prefs.quietEndHour}:00",
                            color = colors.textMuted,
                            style = SuhoodTypography.labelSmall
                        )
                    }
                    Switch(
                        checked = quiet,
                        onCheckedChange = {
                            quiet = it
                            prefs.quietHoursEnabled = it
                        }
                    )
                }
            }

            Text(
                PhoneAssetManifest.NOTE,
                style = SuhoodTypography.labelSmall,
                color = colors.textMuted
            )
        }
    }
}

@Composable
private fun PhonePanel(
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val base = Modifier
        .fillMaxWidth()
        .clip(shape)
        .background(Color(0xFF141519))
        .border(0.5.dp, Color(0x33FFFFFF), shape)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        .padding(16.dp)
    Column(modifier = base, content = content)
}

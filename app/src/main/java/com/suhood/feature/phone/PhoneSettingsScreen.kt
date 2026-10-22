package com.suhood.feature.phone

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.suhood.core.designsystem.components.GlassCard
import com.suhood.core.designsystem.theme.SuhoodTypography
import com.suhood.core.designsystem.theme.suhoodColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneSettingsScreen(onBack: () -> Unit, onOpenPhone: () -> Unit, onOpenAppearance: () -> Unit) {
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

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = { Text("Suhood Phone", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = colors.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background)
            )
        }
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GlassCard(Modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Call, null, tint = colors.accentLavender)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Enable Suhood Phone", fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                        Text("Shows a separate Phone launcher icon", color = colors.textMuted, style = SuhoodTypography.labelSmall)
                    }
                    Switch(checked = enabled, onCheckedChange = {
                        enabled = it
                        prefs.phoneEnabled = it
                        setPhoneLauncherEnabled(context, it)
                    })
                }
            }

            GlassCard(Modifier = Modifier.fillMaxWidth().clickable { onOpenPhone() }) {
                Text("Open Suhood Phone", fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                Text("Dialer, contacts, recents", color = colors.textMuted, style = SuhoodTypography.labelSmall)
            }

            GlassCard(Modifier = Modifier.fillMaxWidth().clickable { onOpenAppearance() }) {
                Text("Call Appearance Studio", fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                Text("Themes, flags, numerals — offline originals", color = colors.textMuted, style = SuhoodTypography.labelSmall)
            }

            GlassCard(Modifier = Modifier.fillMaxWidth()) {
                Text("Default Phone app", fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                Text(
                    if (isDefault) "Suhood is the default dialer"
                    else "Required for in-call UI and screening",
                    color = colors.textMuted,
                    style = SuhoodTypography.labelSmall
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = { requestDefaultDialer(context, roleLauncher) }) {
                    Text(if (isDefault) "Open default-app settings" else "Request default Phone role")
                }
            }

            GlassCard(Modifier = Modifier.fillMaxWidth()) {
                Text("Permissions", fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                Text("Contacts, call log, phone — used only on-device", color = colors.textMuted, style = SuhoodTypography.labelSmall)
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
                }) { Text("Grant phone permissions") }
            }

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Dial pad numerals", fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                NumeralStyle.entries.forEach { style ->
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            numeral = style
                            prefs.numeralStyle = style
                        }.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = numeral == style, onClick = {
                            numeral = style
                            prefs.numeralStyle = style
                        })
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
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Preferred SIM", fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                    Row(Modifier.fillMaxWidth().clickable {
                        simId = -1
                        prefs.preferredSimId = -1
                    }, verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = simId < 0, onClick = {
                            simId = -1
                            prefs.preferredSimId = -1
                        })
                        Text("System default", color = colors.textPrimary)
                    }
                    sims.forEach { (idx, label) ->
                        Row(Modifier.fillMaxWidth().clickable {
                            simId = idx
                            prefs.preferredSimId = idx
                        }, verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = simId == idx, onClick = {
                                simId = idx
                                prefs.preferredSimId = idx
                            })
                            Text(label, color = colors.textPrimary)
                        }
                    }
                }
            }

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Quiet hours", fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                        Text(
                            "Silence unknown incoming calls ${prefs.quietStartHour}:00–${prefs.quietEndHour}:00 (not a reject)",
                            color = colors.textMuted,
                            style = SuhoodTypography.labelSmall
                        )
                    }
                    Switch(checked = quiet, onCheckedChange = {
                        quiet = it
                        prefs.quietHoursEnabled = it
                    })
                }
            }

            Text(PhoneAssetManifest.NOTE, style = SuhoodTypography.labelSmall, color = colors.textMuted)
        }
    }
}

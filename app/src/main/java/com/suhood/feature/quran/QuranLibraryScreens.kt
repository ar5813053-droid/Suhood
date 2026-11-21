package com.suhood.feature.quran

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.suhood.core.designsystem.components.GlassButton
import com.suhood.core.designsystem.components.GlassCard
import com.suhood.core.designsystem.theme.SuhoodTypography
import com.suhood.core.designsystem.theme.suhoodColors
import com.suhood.core.quran.EditionDownloadState
import com.suhood.core.quran.FawazEdition
import com.suhood.core.quran.FawazLanguageGroup

private val Bg = Color(0xFF08090B)
private val Surface = Color(0xFF141519)
private val Elevated = Color(0xFF1A1C20)
private val TextPrimary = Color(0xFFF5F6F7)
private val TextSecondary = Color(0xFFC7CBD1)
private val Border = Color.White.copy(alpha = 0.08f)

@Composable
fun QuranLibraryScreen(
    onBack: () -> Unit,
    onLanguage: (String) -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as Application
    val vm: QuranLibraryViewModel = viewModel(factory = QuranLibraryViewModel.Factory(app))
    val colors = suhoodColors
    val loading by vm.loading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val languages by vm.languages.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val myLib by vm.myLibrary.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val progress by vm.downloadProgress.collectAsStateWithLifecycle()

    LaunchedEffect(message) {
        if (message != null) {
            kotlinx.coroutines.delay(2500)
            vm.clearMessage()
        }
    }

    val trending = remember(languages) { vm.trending() }
    val searchHits = remember(query, languages) { vm.searchResults() }

    Column(
        Modifier
            .fillMaxSize()
            .background(Bg)
            .statusBarsPadding()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Column(Modifier.weight(1f)) {
                Text("Quran Library", style = SuhoodTypography.titleLarge, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Text(
                    "Explore the Quran in languages around the world.",
                    style = SuhoodTypography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = vm::setQuery,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Search language, translator, script…", color = TextSecondary.copy(alpha = 0.7f)) },
            leadingIcon = { Icon(Icons.Filled.Search, null, tint = TextSecondary) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Surface,
                unfocusedContainerColor = Surface,
                focusedBorderColor = Border,
                unfocusedBorderColor = Border,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = TextPrimary
            )
        )

        if (loading && languages.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFB8BEC6))
            }
            return
        }

        LazyColumn(
            contentPadding = PaddingValues(bottom = 32.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (!error.isNullOrBlank()) {
                item {
                    Text(
                        error ?: "",
                        color = Color(0xFFE0C36A),
                        style = SuhoodTypography.labelSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
            if (!message.isNullOrBlank()) {
                item {
                    Text(
                        message ?: "",
                        color = Color(0xFF9FE8C8),
                        style = SuhoodTypography.labelSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }

            if (query.isBlank()) {
                if (myLib.isNotEmpty()) {
                    item {
                        SectionTitle("My Translations")
                    }
                    items(myLib, key = { it.id }) { ed ->
                        EditionCard(
                            edition = ed,
                            statusLabel = statusLabel(vm.status(ed.id), progress[ed.id]),
                            onAdd = { vm.add(ed.id) },
                            onRemove = { vm.remove(ed.id) },
                            onDownload = { vm.download(ed) },
                            onRemoveDownload = { vm.removeDownload(ed.id) }
                        )
                    }
                }

                if (trending.isNotEmpty()) {
                    item { SectionTitle("Trending Picks") }
                    item {
                        Row(
                            Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 12.dp)
                        ) {
                            trending.forEach { ed ->
                                TrendingChip(ed) { onLanguage(ed.language) }
                            }
                        }
                    }
                }

                item {
                    SectionTitle("Browse All Languages")
                    Text(
                        "${languages.size} languages · ${languages.sumOf { it.editions.size }} editions from verified catalogue",
                        style = SuhoodTypography.labelSmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 16.dp, bottom = 8.dp)
                    )
                }
                items(languages, key = { it.language }) { group ->
                    LanguageRow(group) { onLanguage(group.language) }
                }
            } else {
                item { SectionTitle("Search results (${searchHits.size})") }
                items(searchHits, key = { it.id }) { ed ->
                    EditionCard(
                        edition = ed,
                        statusLabel = statusLabel(vm.status(ed.id), progress[ed.id]),
                        onAdd = { vm.add(ed.id) },
                        onRemove = { vm.remove(ed.id) },
                        onDownload = { vm.download(ed) },
                        onRemoveDownload = { vm.removeDownload(ed.id) }
                    )
                }
            }

            item {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Source: Fawaz Ahmed Quran API (jsDelivr). Attribution stays with each translator. Downloads are optional and user-initiated.",
                    style = SuhoodTypography.labelSmall,
                    color = TextSecondary.copy(alpha = 0.75f),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(8.dp))
                GlassButton(
                    text = "Refresh catalogue",
                    onClick = { vm.reloadCatalogue() },
                    primary = false,
                    modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun QuranLanguageDetailScreen(
    language: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as Application
    val vm: QuranLibraryViewModel = viewModel(factory = QuranLibraryViewModel.Factory(app))
    val group = vm.languageGroup(language)
    val progress by vm.downloadProgress.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()

    Column(
        Modifier.fillMaxSize().background(Bg).statusBarsPadding()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary)
            }
            Column {
                Text(language, style = SuhoodTypography.titleLarge, color = TextPrimary)
                Text(
                    "${group?.count ?: 0} verified edition(s)",
                    style = SuhoodTypography.bodySmall,
                    color = TextSecondary
                )
            }
        }
        if (!message.isNullOrBlank()) {
            Text(message ?: "", color = Color(0xFF9FE8C8), modifier = Modifier.padding(16.dp), style = SuhoodTypography.labelSmall)
        }
        LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
            items(group?.editions.orEmpty(), key = { it.id }) { ed ->
                EditionCard(
                    edition = ed,
                    statusLabel = statusLabel(vm.status(ed.id), progress[ed.id]),
                    onAdd = { vm.add(ed.id) },
                    onRemove = { vm.remove(ed.id) },
                    onDownload = { vm.download(ed) },
                    onRemoveDownload = { vm.removeDownload(ed.id) }
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = SuhoodTypography.titleMedium,
        color = TextPrimary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
    )
}

@Composable
private fun LanguageRow(group: FawazLanguageGroup, onClick: () -> Unit) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clickable(onClick = onClick)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Elevated)
                    .border(1.dp, Border, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    group.language.take(2).uppercase(),
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(group.language, color = TextPrimary, style = SuhoodTypography.titleSmall)
                Text(
                    buildString {
                        append(group.count)
                        append(if (group.count == 1) " translation" else " translations")
                        if (group.hasRoman) append(" · includes Roman/Latin script")
                    },
                    color = TextSecondary,
                    style = SuhoodTypography.labelSmall
                )
            }
            Text("Explore", color = Color(0xFFB8BEC6), style = SuhoodTypography.labelMedium)
        }
    }
}

@Composable
private fun TrendingChip(ed: FawazEdition, onClick: () -> Unit) {
    Column(
        Modifier
            .padding(6.dp)
            .width(160.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Surface)
            .border(1.dp, Border, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Text(ed.language, color = TextPrimary, style = SuhoodTypography.titleSmall, maxLines = 1)
        Text(
            ed.author.ifBlank { ed.displayTitle },
            color = TextSecondary,
            style = SuhoodTypography.labelSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (ed.isLikelyRomanScript) {
            Text("Roman / Latin script", color = Color(0xFF9FE8C8), style = SuhoodTypography.labelSmall)
        }
    }
}

@Composable
private fun EditionCard(
    edition: FawazEdition,
    statusLabel: String,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
    onDownload: () -> Unit,
    onRemoveDownload: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
    ) {
        Text(
            edition.author.ifBlank { edition.displayTitle },
            color = TextPrimary,
            style = SuhoodTypography.titleSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        Text(
            "${edition.language} · ${edition.direction.uppercase()} · ${edition.id}",
            color = TextSecondary,
            style = SuhoodTypography.labelSmall
        )
        if (edition.isLikelyRomanScript) {
            Text("Latin/Roman script edition", color = Color(0xFF9FE8C8), style = SuhoodTypography.labelSmall)
        }
        if (edition.source.isNotBlank()) {
            Text(
                "Source: ${edition.source}",
                color = TextSecondary.copy(alpha = 0.8f),
                style = SuhoodTypography.labelSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(statusLabel, color = TextSecondary, style = SuhoodTypography.labelSmall)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val st = statusLabel
            GlassButton(
                text = if (st.startsWith("In library") || st.contains("Added") || st.contains("Offline")) "Remove" else "Add",
                onClick = {
                    if (st.startsWith("In library") || st.contains("Offline") || st.contains("Added")) onRemove()
                    else onAdd()
                },
                primary = !(st.startsWith("In library") || st.contains("Offline")),
                modifier = Modifier.weight(1f)
            )
            GlassButton(
                text = when {
                    st.contains("Downloading") -> "…"
                    st.contains("Offline") -> "Remove DL"
                    else -> "Download"
                },
                onClick = {
                    if (st.contains("Offline")) onRemoveDownload() else onDownload()
                },
                primary = false,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private fun statusLabel(
    status: com.suhood.core.quran.LibraryEditionStatus,
    progress: Int?
): String {
    return when {
        status.downloadState == EditionDownloadState.DOWNLOADING ->
            "Downloading ${progress ?: status.progressPercent}%"
        status.downloadState == EditionDownloadState.DOWNLOADED && status.inMyLibrary ->
            "In library · Available Offline"
        status.downloadState == EditionDownloadState.DOWNLOADED ->
            "Available Offline"
        status.inMyLibrary ->
            "In library · Add"
        else -> "Available · Not downloaded"
    }
}

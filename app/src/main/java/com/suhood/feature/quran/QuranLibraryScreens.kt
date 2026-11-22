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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.suhood.core.quran.LanguageTranslationOption

private val Bg = Color(0xFF08090B)
private val Surface = Color(0xFF141519)
private val Elevated = Color(0xFF1C1E24)
private val TextPrimary = Color(0xFFF5F6F7)
private val TextSecondary = Color(0xFFB8BDC6)
private val TextMuted = Color(0xFF8B919A)
private val Border = Color.White.copy(alpha = 0.08f)
private val Accent = Color(0xFFC9CDD4)
private val SelectedBorder = Color(0xFF6B9EFF).copy(alpha = 0.55f)

/**
 * Quran Translations library — one preferred translation per language.
 * Tapping a language selects it and opens the existing Quran reader (no download gate).
 */
@Composable
fun QuranLibraryScreen(
    onBack: () -> Unit,
    onSelectTranslation: (editionId: String) -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as Application
    val vm: QuranLibraryViewModel = viewModel(factory = QuranLibraryViewModel.Factory(app))
    val query by vm.query.collectAsStateWithLifecycle()
    val languages by vm.languages.collectAsStateWithLifecycle()
    val popular by vm.popular.collectAsStateWithLifecycle()
    val activeId by vm.activeTranslationId.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.refresh() }
    LaunchedEffect(message) {
        if (message != null) {
            kotlinx.coroutines.delay(1600)
            vm.clearMessage()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Bg)
            .statusBarsPadding()
    ) {
        // Header
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Column(Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    "Quran Translations",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Explore Quran translations in your language",
                    color = TextMuted,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Search
        OutlinedTextField(
            value = query,
            onValueChange = { vm.setQuery(it) },
            placeholder = {
                Text("Search language or translator", color = TextMuted, fontSize = 14.sp)
            },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted)
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedBorderColor = Border.copy(alpha = 0.35f),
                unfocusedBorderColor = Border,
                focusedContainerColor = Surface,
                unfocusedContainerColor = Surface,
                cursorColor = Accent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        LazyColumn(
            contentPadding = PaddingValues(bottom = 32.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Popular languages
            if (query.isBlank() && popular.isNotEmpty()) {
                item {
                    SectionLabel("Popular")
                }
                item {
                    Row(
                        Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        popular.forEach { option ->
                            PopularChip(
                                option = option,
                                selected = option.edition.id == activeId,
                                onClick = {
                                    val id = vm.selectLanguage(option)
                                    onSelectTranslation(id)
                                }
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }

            // All languages (one per language)
            item {
                SectionLabel(
                    if (query.isBlank()) "All languages" else "Results"
                )
            }

            if (languages.isEmpty()) {
                item {
                    Text(
                        "No languages match your search.",
                        color = TextMuted,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)
                    )
                }
            } else {
                items(languages, key = { it.edition.langCode }) { option ->
                    LanguageRow(
                        option = option,
                        selected = option.edition.id == activeId,
                        onClick = {
                            val id = vm.selectLanguage(option)
                            onSelectTranslation(id)
                        }
                    )
                }
            }

            item {
                Spacer(Modifier.height(16.dp))
                Text(
                    "Translations load on demand while you read. No download required.",
                    color = TextMuted.copy(alpha = 0.75f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        color = TextSecondary,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.3.sp,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 8.dp)
    )
}

@Composable
private fun PopularChip(
    option: LanguageTranslationOption,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (selected) Elevated else Surface
    val borderColor = if (selected) SelectedBorder else Border
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .width(88.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.06f))
        ) {
            Text(
                option.language.take(2).uppercase(),
                color = if (selected) Accent else TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            option.language,
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun LanguageRow(
    option: LanguageTranslationOption,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (selected) Elevated else Surface
    val borderColor = if (selected) SelectedBorder else Border
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.05f))
        ) {
            Text(
                option.language.take(1).uppercase(),
                color = TextSecondary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                option.title,
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                option.subtitle,
                color = TextMuted,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (selected) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Selected",
                tint = Accent,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Language detail is no longer a download gate.
 * Kept as a thin redirect target for older nav routes — opens nothing heavy.
 */
@Composable
fun QuranLanguageDetailScreen(
    language: String,
    onBack: () -> Unit,
    onSelectTranslation: (editionId: String) -> Unit = {}
) {
    val option = TranslationEditionsCompat.optionForLanguageName(language)
    LaunchedEffect(option) {
        if (option != null) {
            onSelectTranslation(option.edition.id)
        }
    }
    // Immediate back if somehow shown; selection handled by parent.
    Column(
        Modifier
            .fillMaxSize()
            .background(Bg)
            .statusBarsPadding()
            .padding(20.dp)
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
        }
        Text("Opening $language…", color = TextSecondary, fontSize = 14.sp)
    }
}

/** Small bridge so language-detail route can resolve a preferred edition without Fawaz ids. */
private object TranslationEditionsCompat {
    fun optionForLanguageName(name: String): LanguageTranslationOption? =
        com.suhood.core.quran.TranslationEditions.preferredLanguages()
            .find { it.language.equals(name, ignoreCase = true) }
}

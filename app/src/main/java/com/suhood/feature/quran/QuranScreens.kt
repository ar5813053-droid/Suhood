package com.suhood.feature.quran

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.suhood.core.data.DemoData
import com.suhood.core.designsystem.components.*
import com.suhood.core.designsystem.theme.SuhoodSpacing
import com.suhood.core.designsystem.theme.SuhoodTypography
import com.suhood.core.designsystem.theme.suhoodColors

object QuranScreens {

    @Composable
    fun Home(onBack: () -> Unit, onSurahList: () -> Unit, onBookmarks: () -> Unit) {
        val colors = suhoodColors
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            ScreenHeader(title = "Quran", onBack = onBack)
            Column(modifier = Modifier.padding(horizontal = SuhoodSpacing.screenHorizontal)) {
                GlassCard {
                    Text(
                        "Quran text is loaded from a trusted online source when available. The interface is ready for a trusted content source. Placeholders are clearly labelled and are not presented as scripture.",
                        style = SuhoodTypography.bodyMedium,
                        color = colors.textSecondary
                    )
                }
                Spacer(Modifier.height(16.dp))
                GlassCard(modifier = Modifier.fillMaxWidth(), onClick = onSurahList) {
                    Text("Surah list", style = SuhoodTypography.titleMedium, color = colors.textPrimary)
                    Text("Browse by surah (metadata)", style = SuhoodTypography.bodySmall, color = colors.textMuted)
                }
                Spacer(Modifier.height(8.dp))
                GlassCard(modifier = Modifier.fillMaxWidth(), onClick = onBookmarks) {
                    Text("Bookmarks", style = SuhoodTypography.titleMedium, color = colors.textPrimary)
                    Text("Local bookmarks", style = SuhoodTypography.bodySmall, color = colors.textMuted)
                }
            }
        }
    }

    @Composable
    fun SurahList(onBack: () -> Unit, onSurah: (Int) -> Unit) {
        val colors = suhoodColors
        var query by remember { mutableStateOf("") }
        val filtered = remember(query) {
            DemoData.surahList.filter {
                it.nameTransliterated.contains(query, true) ||
                    it.nameEnglish.contains(query, true) ||
                    it.id.toString() == query
            }
        }
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            ScreenHeader(title = "Surahs", onBack = onBack)
            GlassSearchField(
                query = query,
                onQueryChange = { query = it },
                placeholder = "Search surah",
                modifier = Modifier.padding(horizontal = SuhoodSpacing.screenHorizontal)
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(
                contentPadding = PaddingValues(horizontal = SuhoodSpacing.screenHorizontal, vertical = 8.dp)
            ) {
                items(filtered, key = { it.id }) { surah ->
                    GlassCard(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        onClick = { onSurah(surah.id) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("${surah.id}. ${surah.nameTransliterated}", style = SuhoodTypography.titleSmall, color = colors.textPrimary)
                                Text(surah.nameEnglish, style = SuhoodTypography.bodySmall, color = colors.textMuted)
                            }
                            Text(surah.nameArabic, style = SuhoodTypography.titleMedium, color = colors.silver)
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun SurahDetail(surahId: Int, onBack: () -> Unit, onRead: () -> Unit) {
        val colors = suhoodColors
        val surah = DemoData.surahList.find { it.id == surahId }
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            ScreenHeader(title = surah?.nameTransliterated ?: "Surah", onBack = onBack)
            Column(modifier = Modifier.padding(SuhoodSpacing.screenHorizontal)) {
                if (surah != null) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(surah.nameArabic, style = SuhoodTypography.headlineMedium, color = colors.textPrimary)
                        Spacer(Modifier.height(8.dp))
                        Text(surah.nameEnglish, style = SuhoodTypography.bodyMedium, color = colors.textSecondary)
                        Text("${surah.ayahCount} ayat · ${surah.revelationType}", style = SuhoodTypography.bodySmall, color = colors.textMuted)
                    }
                    Spacer(Modifier.height(16.dp))
                    GlassButton(text = "Open reader", onClick = onRead, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }

    @Composable
    fun Reader(surahId: Int, onBack: () -> Unit) {
        val colors = suhoodColors
        val surah = DemoData.surahList.find { it.id == surahId }
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            ScreenHeader(title = surah?.nameTransliterated ?: "Reader", onBack = onBack)
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(SuhoodSpacing.screenHorizontal)
            ) {
                DemoStatusLabel("Source · AlQuran.Cloud")
                Spacer(Modifier.height(16.dp))
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Connect to the internet to load verified Arabic text from AlQuran.Cloud (Islamic Network). Suhood does not invent verses.",
                        style = SuhoodTypography.bodyLarge,
                        color = colors.textSecondary
                    )
                }
            }
        }
    }

    @Composable
    fun Bookmarks(onBack: () -> Unit) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            ScreenHeader(title = "Bookmarks", onBack = onBack)
            EmptyState(
                title = "No bookmarks yet",
                message = "Bookmarks you save while reading will appear here.",
                modifier = Modifier.padding(top = 40.dp)
            )
        }
    }
}

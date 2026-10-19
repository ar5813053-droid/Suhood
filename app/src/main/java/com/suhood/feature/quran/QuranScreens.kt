package com.suhood.feature.quran

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.suhood.app.SuhoodApplication
import com.suhood.core.designsystem.components.DemoStatusLabel
import com.suhood.core.designsystem.components.GlassButton
import com.suhood.core.designsystem.components.GlassCard
import com.suhood.core.designsystem.components.GlassSearchField
import com.suhood.core.designsystem.components.ScreenHeader
import com.suhood.core.designsystem.theme.SuhoodSpacing
import com.suhood.core.designsystem.theme.SuhoodTypography
import com.suhood.core.designsystem.theme.suhoodColors
import com.suhood.core.quran.VerifiedQaris

object QuranScreens {

    @Composable
    private fun rememberVm(): QuranViewModel {
        val app = LocalContext.current.applicationContext as SuhoodApplication
        return viewModel(factory = QuranViewModel.Factory(app.database.dao(), app.playbackController))
    }

    @Composable
    fun Home(onBack: () -> Unit, onSurahList: () -> Unit, onBookmarks: () -> Unit) {
        val colors = suhoodColors
        Column(
            Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = SuhoodSpacing.screenHorizontal)
        ) {
            ScreenHeader(title = "القرآن الكريم", onBack = onBack)
            Text("Al-Quran", style = SuhoodTypography.bodyMedium, color = colors.textSecondary)
            Spacer(Modifier.height(8.dp))
            DemoStatusLabel("Text & audio · AlQuran.Cloud")
            Spacer(Modifier.height(16.dp))
            GlassCard(modifier = Modifier.fillMaxWidth(), onClick = onSurahList) {
                Text("Surah list", style = SuhoodTypography.titleMedium, color = colors.textPrimary)
                Text("114 surahs · Arabic + Urdu", style = SuhoodTypography.bodySmall, color = colors.textMuted)
            }
            Spacer(Modifier.height(10.dp))
            GlassCard(modifier = Modifier.fillMaxWidth(), onClick = onBookmarks) {
                Text("Bookmarks", style = SuhoodTypography.titleMedium, color = colors.textPrimary)
                Text("Saved on this device", style = SuhoodTypography.bodySmall, color = colors.textMuted)
            }
        }
    }

    @Composable
    fun SurahList(onBack: () -> Unit, onSurah: (Int) -> Unit) {
        val vm = rememberVm()
        val surahs by vm.surahs.collectAsStateWithLifecycle()
        val loading by vm.loading.collectAsStateWithLifecycle()
        val error by vm.error.collectAsStateWithLifecycle()
        val colors = suhoodColors
        var query by remember { mutableStateOf("") }

        LaunchedEffect(Unit) { if (surahs.isEmpty()) vm.refreshSurahList() }

        val filtered = remember(query, surahs) {
            if (query.isBlank()) surahs
            else surahs.filter {
                it.nameEnglish.contains(query, true) ||
                    it.nameArabic.contains(query) ||
                    it.number.toString() == query.trim()
            }
        }

        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            ScreenHeader(title = "Surahs", onBack = onBack)
            GlassSearchField(
                query = query,
                onQueryChange = { query = it },
                placeholder = "Search name or number",
                modifier = Modifier.padding(horizontal = SuhoodSpacing.screenHorizontal)
            )
            when {
                loading && surahs.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colors.silverBright)
                }
                error != null && surahs.isEmpty() -> Column(
                    Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(error ?: "Error", color = colors.textSecondary)
                    Spacer(Modifier.height(12.dp))
                    GlassButton(text = "Retry", onClick = { vm.refreshSurahList() })
                }
                else -> LazyColumn(contentPadding = PaddingValues(SuhoodSpacing.screenHorizontal, 12.dp)) {
                    items(filtered, key = { it.number }) { s ->
                        GlassCard(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            onClick = { onSurah(s.number) }
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        "${s.number}. ${s.nameEnglish}",
                                        style = SuhoodTypography.titleSmall,
                                        color = colors.textPrimary
                                    )
                                    Text(
                                        "${s.ayahCount} ayahs · ${s.revelationType}",
                                        style = SuhoodTypography.labelSmall,
                                        color = colors.textMuted
                                    )
                                }
                                Text(s.nameArabic, style = SuhoodTypography.arabicBody, color = colors.textPrimary)
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun SurahDetail(surahId: Int, onBack: () -> Unit, onRead: () -> Unit) {
        val colors = suhoodColors
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = SuhoodSpacing.screenHorizontal)) {
            ScreenHeader(title = "Surah $surahId", onBack = onBack)
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Open the reader for Arabic text, optional Urdu translation, and recitation.",
                    color = colors.textSecondary
                )
            }
            Spacer(Modifier.height(16.dp))
            GlassButton(text = "Open reader", onClick = onRead, modifier = Modifier.fillMaxWidth())
        }
    }

    @Composable
    fun Reader(surahId: Int, onBack: () -> Unit) {
        val vm = rememberVm()
        val content by vm.content.collectAsStateWithLifecycle()
        val loading by vm.loading.collectAsStateWithLifecycle()
        val error by vm.error.collectAsStateWithLifecycle()
        val showUrdu by vm.showUrdu.collectAsStateWithLifecycle()
        val qariId by vm.qariId.collectAsStateWithLifecycle()
        val colors = suhoodColors
        var showQari by remember { mutableStateOf(false) }

        LaunchedEffect(surahId) { vm.openSurah(surahId) }

        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            ScreenHeader(
                title = content?.meta?.nameEnglish ?: "Surah $surahId",
                onBack = onBack
            )
            Row(
                Modifier.fillMaxWidth().padding(horizontal = SuhoodSpacing.screenHorizontal),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlassButton(
                    text = if (showUrdu) "Urdu on" else "Urdu off",
                    onClick = { vm.toggleUrdu() },
                    primary = false
                )
                GlassButton(text = "Qari", onClick = { showQari = !showQari }, primary = false)
                GlassButton(text = "Play", onClick = { vm.playSurahFrom(0) })
            }
            if (showQari) {
                Column(Modifier.padding(horizontal = SuhoodSpacing.screenHorizontal, vertical = 8.dp)) {
                    VerifiedQaris.list.forEach { q ->
                        GlassCard(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            selected = q.id == qariId,
                            onClick = { vm.setQari(q.id); showQari = false }
                        ) {
                            Text(q.englishName, style = SuhoodTypography.titleSmall, color = colors.textPrimary)
                            Text(q.name, style = SuhoodTypography.arabicBody, color = colors.textSecondary)
                        }
                    }
                }
            }
            Text(
                vm.translatorNote(),
                style = SuhoodTypography.labelSmall,
                color = colors.textMuted,
                modifier = Modifier.padding(horizontal = SuhoodSpacing.screenHorizontal, vertical = 4.dp)
            )
            when {
                loading && content == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colors.silverBright)
                }
                error != null && content == null -> Text(
                    error ?: "",
                    color = colors.textSecondary,
                    modifier = Modifier.padding(24.dp)
                )
                content != null -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    itemsIndexed(content!!.ayahs) { _, ayah ->
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clickable {
                                    vm.playAyah(ayah.globalNumber, surahId, ayah.numberInSurah)
                                }
                        ) {
                            Text(
                                "﴿ ${ayah.numberInSurah} ﴾",
                                style = SuhoodTypography.labelMedium,
                                color = colors.silverBright
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                ayah.arabic,
                                style = TextStyle(
                                    fontSize = 22.sp,
                                    lineHeight = 40.sp,
                                    textAlign = TextAlign.End,
                                    color = colors.textPrimary,
                                    textDirection = TextDirection.Rtl
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (showUrdu && ayah.translation.isNotBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    ayah.translation,
                                    style = TextStyle(
                                        fontSize = 15.sp,
                                        lineHeight = 26.sp,
                                        textAlign = TextAlign.Start,
                                        color = colors.textSecondary,
                                        textDirection = TextDirection.Rtl
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    "Play",
                                    style = SuhoodTypography.labelSmall,
                                    color = colors.silverBright,
                                    modifier = Modifier.clickable {
                                        vm.playAyah(ayah.globalNumber, surahId, ayah.numberInSurah)
                                    }
                                )
                                Text(
                                    "Bookmark",
                                    style = SuhoodTypography.labelSmall,
                                    color = colors.silver,
                                    modifier = Modifier.clickable {
                                        vm.bookmark(surahId, ayah.numberInSurah)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun Bookmarks(onBack: () -> Unit) {
        val vm = rememberVm()
        val bookmarks by vm.bookmarks.collectAsStateWithLifecycle()
        val colors = suhoodColors
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            ScreenHeader(title = "Bookmarks", onBack = onBack)
            if (bookmarks.isEmpty()) {
                Text("No bookmarks yet", modifier = Modifier.padding(24.dp), color = colors.textMuted)
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp)) {
                    items(bookmarks, key = { it.id }) { b ->
                        GlassCard(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text(
                                "Surah ${b.surah} · Ayah ${b.ayah}",
                                style = SuhoodTypography.titleSmall,
                                color = colors.textPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

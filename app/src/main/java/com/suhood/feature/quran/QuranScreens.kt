package com.suhood.feature.quran

import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.filled.Pause
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import com.suhood.core.designsystem.components.*
import com.suhood.core.designsystem.theme.SuhoodSpacing
import com.suhood.core.designsystem.theme.SuhoodTypography
import com.suhood.core.designsystem.theme.suhoodColors
import com.suhood.core.quran.JuzIndex
import com.suhood.core.quran.VerifiedQaris

object QuranScreens {

    @Composable
    private fun rememberVm(): QuranViewModel {
        val app = LocalContext.current.applicationContext as SuhoodApplication
        return viewModel(factory = QuranViewModel.Factory(app.database.dao(), app.playbackController))
    }

    @Composable
    fun Home(onBack: () -> Unit, onSurahList: () -> Unit, onBookmarks: () -> Unit) {
        // Treat Home as Parah list entry
        ParahList(onBack = onBack, onParah = { /* handled via nav in graph */ }, onBookmarks = onBookmarks, onAllSurahs = onSurahList)
    }

    @Composable
    fun ParahList(
        onBack: () -> Unit,
        onParah: (Int) -> Unit,
        onBookmarks: () -> Unit = {},
        onAllSurahs: () -> Unit = {}
    ) {
        val colors = suhoodColors
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            ScreenHeader(title = "القرآن · Parah", onBack = onBack)
            Row(
                Modifier.padding(horizontal = SuhoodSpacing.screenHorizontal),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlassButton(text = "All surahs", onClick = onAllSurahs, primary = false)
                GlassButton(text = "Bookmarks", onClick = onBookmarks, primary = false)
            }
            Spacer(Modifier.height(8.dp))
            LazyColumn(
                contentPadding = PaddingValues(horizontal = SuhoodSpacing.screenHorizontal, vertical = 8.dp)
            ) {
                items(JuzIndex.list, key = { it.number }) { juz ->
                    GlassCard(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        onClick = { onParah(juz.number) }
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "Parah ${juz.number}",
                                    style = SuhoodTypography.titleMedium,
                                    color = colors.textPrimary
                                )
                                Text(
                                    "Starts Surah ${juz.startSurah}:${juz.startAyah}",
                                    style = SuhoodTypography.labelSmall,
                                    color = colors.textMuted
                                )
                            }
                            Text(juz.labelAr, style = SuhoodTypography.arabicBody, color = colors.textPrimary)
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun ParahDetail(parahId: Int, onBack: () -> Unit, onSurah: (Int) -> Unit) {
        val vm = rememberVm()
        val surahs by vm.surahs.collectAsStateWithLifecycle()
        val colors = suhoodColors
        LaunchedEffect(Unit) { if (surahs.isEmpty()) vm.refreshSurahList() }
        val numbers = remember(parahId) { JuzIndex.surahNumbersInJuz(parahId) }
        val filtered = surahs.filter { it.number in numbers }.ifEmpty {
            numbers.map { n ->
                com.suhood.core.quran.SurahMeta(
                    number = n, nameArabic = "", nameEnglish = "Surah $n",
                    nameTranslated = "", ayahCount = 0, revelationType = ""
                )
            }
        }

        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            ScreenHeader(title = "Parah $parahId", onBack = onBack)
            LazyColumn(contentPadding = PaddingValues(SuhoodSpacing.screenHorizontal, 8.dp)) {
                items(filtered, key = { it.number }) { s ->
                    GlassCard(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        onClick = { onSurah(s.number) }
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    "${s.number}. ${s.nameEnglish}",
                                    style = SuhoodTypography.titleSmall,
                                    color = colors.textPrimary
                                )
                                if (s.ayahCount > 0) {
                                    Text(
                                        "${s.ayahCount} ayahs",
                                        style = SuhoodTypography.labelSmall,
                                        color = colors.textMuted
                                    )
                                }
                            }
                            Text(s.nameArabic, style = SuhoodTypography.arabicBody, color = colors.textPrimary)
                        }
                    }
                }
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
                it.nameEnglish.contains(query, true) || it.nameArabic.contains(query) ||
                    it.number.toString() == query.trim()
            }
        }
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            ScreenHeader(title = "Surahs", onBack = onBack)
            GlassSearchField(
                query = query,
                onQueryChange = { query = it },
                placeholder = "Search",
                modifier = Modifier.padding(horizontal = SuhoodSpacing.screenHorizontal)
            )
            when {
                loading && surahs.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colors.silverBright)
                }
                error != null && surahs.isEmpty() -> Column(Modifier.padding(24.dp)) {
                    Text(error ?: "", color = colors.textSecondary)
                    GlassButton(text = "Retry", onClick = { vm.refreshSurahList() })
                }
                else -> LazyColumn(contentPadding = PaddingValues(16.dp)) {
                    items(filtered, key = { it.number }) { s ->
                        GlassCard(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            onClick = { onSurah(s.number) }
                        ) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${s.number}. ${s.nameEnglish}", color = colors.textPrimary)
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
        LaunchedEffect(surahId) { onRead() }
    }

    @Composable
    fun Reader(surahId: Int, onBack: () -> Unit) {
        val vm = rememberVm()
        val content by vm.content.collectAsStateWithLifecycle()
        val loading by vm.loading.collectAsStateWithLifecycle()
        val error by vm.error.collectAsStateWithLifecycle()
        val showUrdu by vm.showUrdu.collectAsStateWithLifecycle()
        val qariId by vm.qariId.collectAsStateWithLifecycle()
        val app = LocalContext.current.applicationContext as SuhoodApplication
        val playback by app.playbackController.state.collectAsStateWithLifecycle()
        val colors = suhoodColors
        var showQari by remember { mutableStateOf(false) }
        var currentAyahIndex by remember { mutableIntStateOf(0) }

        LaunchedEffect(surahId) { vm.openSurah(surahId) }

        // Keep current ayah index in sync with the track that is actually playing
        LaunchedEffect(playback.currentTrack?.id, content) {
            val id = playback.currentTrack?.id ?: return@LaunchedEffect
            val ayahs = content?.ayahs ?: return@LaunchedEffect
            val idx = ayahs.indexOfFirst { "quran:${it.globalNumber}" == id }
            if (idx >= 0) currentAyahIndex = idx
        }

        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().statusBarsPadding()) {
                // Liquid glass top navbar
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xCC0B1220),
                                    Color(0x990B1220),
                                    Color.Transparent
                                )
                            )
                        )
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(colors.surfaceGlass.copy(alpha = 0.55f))
                            .border(1.dp, colors.borderSubtle.copy(alpha = 0.5f), RoundedCornerShape(22.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GlassIconButton(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            onClick = onBack,
                            size = 40.dp,
                            iconSize = 20.dp
                        )
                        Text(
                            text = content?.meta?.nameEnglish ?: "Surah $surahId",
                            style = SuhoodTypography.titleMedium,
                            color = colors.textPrimary,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                            maxLines = 1
                        )
                        GlassButton(
                            text = if (showUrdu) "Urdu" else "AR",
                            onClick = { vm.toggleUrdu() },
                            primary = false
                        )
                    }
                }

                when {
                    loading && content == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = colors.silverBright)
                    }
                    error != null && content == null -> Text(
                        error ?: "",
                        modifier = Modifier.padding(24.dp),
                        color = colors.textSecondary
                    )
                    content != null -> LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 140.dp, top = 8.dp)
                    ) {
                        itemsIndexed(content!!.ayahs) { index, ayah ->
                            val playingThis = playback.currentTrack?.id == "quran:${ayah.globalNumber}" && playback.isPlaying
                            GlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .clickable {
                                        currentAyahIndex = index
                                        vm.playAyah(ayah.globalNumber, surahId, ayah.numberInSurah)
                                    },
                                selected = playingThis || index == currentAyahIndex
                            ) {
                                Text(
                                    "﴾ ${ayah.numberInSurah} ﴿",
                                    style = SuhoodTypography.labelMedium,
                                    color = colors.textMuted
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    ayah.arabic,
                                    style = TextStyle(
                                        fontSize = 24.sp,
                                        textAlign = TextAlign.Right,
                                        textDirection = TextDirection.Rtl,
                                        color = colors.textPrimary,
                                        lineHeight = 40.sp
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                if (showUrdu && ayah.translation.isNotBlank()) {
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        ayah.translation,
                                        style = SuhoodTypography.bodyMedium,
                                        color = colors.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Liquid glass bottom player bar
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color(0xE60B1220), Color(0xF20B1220))
                        )
                    )
                    .padding(start = 12.dp, end = 12.dp, bottom = 16.dp, top = 24.dp)
            ) {
                if (showQari) {
                    GlassCard(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                        Text("Select Qari", style = SuhoodTypography.titleSmall, color = colors.textPrimary)
                        Spacer(Modifier.height(8.dp))
                        VerifiedQaris.list.forEach { q ->
                            val selected = q.id == qariId
                            Text(
                                q.englishName,
                                style = SuhoodTypography.bodyMedium,
                                color = if (selected) colors.silverBright else colors.textSecondary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        vm.setQari(q.id)
                                        showQari = false
                                        // Re-start from current ayah with new qari when user presses play
                                    }
                                    .padding(vertical = 8.dp)
                            )
                        }
                    }
                }

                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    val progress = if (playback.positionMs > 0) {
                        (playback.positionMs % 60_000L) / 60_000f
                    } else 0f
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                        color = colors.silverBright,
                        trackColor = colors.borderSubtle
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GlassIconButton(
                            icon = Icons.Filled.SkipPrevious,
                            contentDescription = "Previous",
                            onClick = {
                                content?.ayahs?.let { ayahs ->
                                    if (ayahs.isEmpty()) return@GlassIconButton
                                    currentAyahIndex = (currentAyahIndex - 1).coerceAtLeast(0)
                                    val a = ayahs[currentAyahIndex]
                                    runCatching {
                                        vm.playAyah(a.globalNumber, surahId, a.numberInSurah)
                                    }
                                }
                            }
                        )

                        // Animated Play / Pause
                        val isPlaying = playback.isPlaying
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(colors.silverBright.copy(alpha = 0.18f))
                                .border(1.dp, colors.silverBright.copy(alpha = 0.45f), CircleShape)
                                .clickable {
                                    runCatching {
                                        if (isPlaying) {
                                            app.playbackController.togglePlayPause()
                                        } else {
                                            val ayahs = content?.ayahs
                                            if (ayahs != null && ayahs.isNotEmpty()) {
                                                // If current track already in queue, just resume; else start from index
                                                val currentId = playback.currentTrack?.id
                                                val matches = currentId != null && ayahs.any { "quran:${it.globalNumber}" == currentId }
                                                if (matches && !playback.isPlaying) {
                                                    app.playbackController.togglePlayPause()
                                                } else {
                                                    vm.playSurahFrom(currentAyahIndex.coerceIn(0, ayahs.lastIndex))
                                                }
                                            }
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            AnimatedContent(
                                targetState = isPlaying,
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                                label = "playPause"
                            ) { playing ->
                                Icon(
                                    imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = if (playing) "Pause" else "Play",
                                    tint = colors.silverBright,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }

                        GlassIconButton(
                            icon = Icons.Filled.SkipNext,
                            contentDescription = "Next",
                            onClick = {
                                content?.ayahs?.let { ayahs ->
                                    if (ayahs.isEmpty()) return@GlassIconButton
                                    currentAyahIndex = (currentAyahIndex + 1).coerceAtMost(ayahs.lastIndex)
                                    val a = ayahs[currentAyahIndex]
                                    runCatching {
                                        vm.playAyah(a.globalNumber, surahId, a.numberInSurah)
                                    }
                                }
                            }
                        )
                        GlassIconButton(
                            icon = Icons.Filled.Person,
                            contentDescription = "Qari",
                            onClick = { showQari = !showQari }
                        )
                    }
                }
            }
        }
    }

    fun Bookmarks(onBack: () -> Unit) {
        val vm = rememberVm()
        val bookmarks by vm.bookmarks.collectAsStateWithLifecycle()
        val colors = suhoodColors
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            ScreenHeader(title = "Bookmarks", onBack = onBack)
            if (bookmarks.isEmpty()) {
                Text("No bookmarks", modifier = Modifier.padding(24.dp), color = colors.textMuted)
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

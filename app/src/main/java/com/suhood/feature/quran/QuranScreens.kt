package com.suhood.feature.quran
import com.suhood.app.SuhoodApplication
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontWeight

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.IconButton
import androidx.compose.ui.text.style.TextOverflow
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
import com.suhood.core.designsystem.components.*
import com.suhood.core.designsystem.theme.SuhoodSpacing
import com.suhood.core.designsystem.theme.SuhoodTypography
import com.suhood.core.designsystem.theme.suhoodColors
import com.suhood.core.navigation.Routes
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
        onAllSurahs: () -> Unit = {},
        onMushaf: () -> Unit = {}
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
                GlassButton(text = "Mushaf", onClick = onMushaf, primary = true)
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
    fun SurahList(
        onBack: () -> Unit,
        onSurah: (Int) -> Unit,
        onBookmarks: () -> Unit = {},
        onJuz: () -> Unit = {},
        onMushaf: () -> Unit = {},
        onLastRead: () -> Unit = {}
    ) {
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
                    it.nameTranslated.contains(query, true) ||
                    it.number.toString() == query.trim()
            }
        }
        val appCtx = LocalContext.current.applicationContext as SuhoodApplication
        val prefsRepo = remember { com.suhood.core.datastore.PreferencesRepository(appCtx) }
        val dailyMin by prefsRepo.quranDailyMinutes.collectAsStateWithLifecycle(10)
        val todayPages by prefsRepo.quranTodayPages.collectAsStateWithLifecycle(0)

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .background(colors.background)
        ) {
            // Header
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "SUHOOD · QURAN",
                    style = SuhoodTypography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.accentLavender
                )
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.textSecondary)
                }
            }

            // Hero
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                    style = SuhoodTypography.arabicBody,
                    color = colors.accentLavender,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "The Holy Quran",
                    style = SuhoodTypography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary
                )
                Text(
                    "114 Surahs · Authentic Arabic text",
                    style = SuhoodTypography.labelSmall,
                    color = colors.textMuted
                )
                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier
                        .width(160.dp)
                        .height(1.dp)
                        .background(colors.accentLavender.copy(alpha = 0.35f))
                )
            }
            Spacer(Modifier.height(14.dp))

            GlassSearchField(
                query = query,
                onQueryChange = { query = it },
                placeholder = "Search by surah name or number…",
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(14.dp))

            // Companion card
            GlassCard(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                Text(
                    "DAILY READING",
                    style = SuhoodTypography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = colors.accentLavender
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Today · $todayPages pages · ${dailyMin} min goal",
                    style = SuhoodTypography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.textPrimary
                )
                Text(
                    "Change goal in Settings · Quran Companion",
                    style = SuhoodTypography.labelSmall,
                    color = colors.textMuted
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassButton(
                        text = "Continue",
                        onClick = onLastRead,
                        primary = true,
                        modifier = Modifier.weight(1f)
                    )
                    GlassButton(
                        text = "Mushaf",
                        onClick = onMushaf,
                        primary = false,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(Modifier.height(14.dp))

            // Quick nav 2x2
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickNavTile(
                    title = "Last Read",
                    subtitle = "Resume reading",
                    icon = Icons.Filled.Bookmark,
                    onClick = onLastRead,
                    modifier = Modifier.weight(1f)
                )
                QuickNavTile(
                    title = "Mushaf",
                    subtitle = "Page view",
                    icon = Icons.Filled.MenuBook,
                    onClick = onMushaf,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickNavTile(
                    title = "Bookmarks",
                    subtitle = "Saved ayahs",
                    icon = Icons.Filled.BookmarkBorder,
                    onClick = onBookmarks,
                    modifier = Modifier.weight(1f)
                )
                QuickNavTile(
                    title = "Juz / Para",
                    subtitle = "30 parts",
                    icon = Icons.Filled.ViewModule,
                    onClick = onJuz,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(16.dp))

            Text(
                "ALL SURAHS",
                style = SuhoodTypography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = colors.textMuted,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(8.dp))

            when {
                loading && surahs.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colors.accentLavender)
                }
                error != null && surahs.isEmpty() -> Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(error ?: "Error", color = colors.error)
                    Spacer(Modifier.height(12.dp))
                    GlassButton(text = "Retry", onClick = { vm.refreshSurahList() })
                }
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filtered, key = { it.number }) { s ->
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                            onClick = { onSurah(s.number) }
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(colors.accentLavender.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "${s.number}",
                                        style = SuhoodTypography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = colors.accentLavender
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        s.nameArabic,
                                        style = SuhoodTypography.arabicBody,
                                        color = colors.textPrimary
                                    )
                                    Text(
                                        buildString {
                                            append(s.nameEnglish)
                                            if (s.nameTranslated.isNotBlank()) append(" · ${s.nameTranslated}")
                                        },
                                        style = SuhoodTypography.bodySmall,
                                        color = colors.textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "${s.ayahCount} verses",
                                        style = SuhoodTypography.labelSmall,
                                        color = colors.textMuted
                                    )
                                    if (s.revelationType.isNotBlank()) {
                                        Text(
                                            s.revelationType,
                                            style = SuhoodTypography.labelSmall,
                                            color = colors.textMuted
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun QuickNavTile(
        title: String,
        subtitle: String,
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        onClick: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        val colors = suhoodColors
        GlassCard(
            modifier = modifier,
            contentPadding = PaddingValues(14.dp),
            onClick = onClick
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.accentLavender.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = colors.accentLavender, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(title, style = SuhoodTypography.labelLarge.copy(fontWeight = FontWeight.SemiBold), color = colors.textPrimary)
                    Text(subtitle, style = SuhoodTypography.labelSmall, color = colors.textMuted)
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
        val listState = rememberLazyListState()
        LaunchedEffect(playback.currentTrack?.id, content) {
            val id = playback.currentTrack?.id ?: return@LaunchedEffect
            val ayahs = content?.ayahs ?: return@LaunchedEffect
            val idx = ayahs.indexOfFirst { "quran:${it.globalNumber}" == id }
            if (idx >= 0) {
                currentAyahIndex = idx
                runCatching { listState.animateScrollToItem(idx) }
            }
        }

        Box(Modifier.fillMaxSize().background(colors.background)) {
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
                                        color = if (playingThis) colors.accentLavender else colors.textPrimary,
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

    
    /** Page-based Mushaf reader (Madinah pages 1–604 via AlQuran.Cloud quran-uthmani). */
    @Composable
    fun MushafReader(startPage: Int = 1, onBack: () -> Unit) {
        val vm = rememberVm()
        val page by vm.page.collectAsStateWithLifecycle()
        val pageNumber by vm.pageNumber.collectAsStateWithLifecycle()
        val companionApp = LocalContext.current.applicationContext as SuhoodApplication
        val companionPrefs = remember { com.suhood.core.datastore.PreferencesRepository(companionApp) }
        var lastTrackedPage by remember { mutableIntStateOf(-1) }
        LaunchedEffect(pageNumber) {
            if (page != null && pageNumber != lastTrackedPage && lastTrackedPage != -1) {
                companionPrefs.addQuranPagesRead(1)
                companionPrefs.setQuranLastPage(pageNumber)
            }
            lastTrackedPage = pageNumber
            companionPrefs.setQuranLastPage(pageNumber)
        }
        val loading by vm.loading.collectAsStateWithLifecycle()
        val error by vm.error.collectAsStateWithLifecycle()
        val colors = suhoodColors
        LaunchedEffect(startPage) { vm.openPage(startPage) }

        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    onClick = onBack
                )
                Column(Modifier.weight(1f)) {
                    Text("Mushaf · Madinah pages", style = SuhoodTypography.titleMedium, color = colors.textPrimary)
                    Text(
                        "Page $pageNumber / 604 · Uthmani (AlQuran.Cloud)",
                        style = SuhoodTypography.labelSmall,
                        color = colors.textMuted
                    )
                }
            }
            when {
                loading && page == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colors.accentLavender)
                }
                error != null && page == null -> Column(Modifier.padding(24.dp)) {
                    Text(error ?: "", color = colors.textSecondary)
                    GlassButton(text = "Retry", onClick = { vm.openPage(pageNumber) })
                }
                else -> {
                    val ayahs = page?.ayahs.orEmpty()
                    val surahLabel = ayahs.firstOrNull()?.surahName.orEmpty()
                    Column(Modifier.weight(1f)) {
                        if (surahLabel.isNotBlank()) {
                            Text(
                                surahLabel,
                                style = SuhoodTypography.titleSmall,
                                color = colors.accentLavender,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                        LazyColumn(
                            Modifier
                                .weight(1f)
                                .padding(horizontal = 20.dp),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            items(ayahs, key = { it.globalNumber }) { a ->
                                Text(
                                    text = a.arabic + " \u06DD${a.numberInSurah}",
                                    style = TextStyle(
                                        fontSize = 26.sp,
                                        lineHeight = 48.sp,
                                        textAlign = TextAlign.Center,
                                        textDirection = TextDirection.Rtl
                                    ),
                                    color = colors.textPrimary,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                )
                            }
                        }
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            GlassButton(
                                text = "Previous",
                                onClick = { vm.previousPage() },
                                primary = false,
                                enabled = pageNumber > 1
                            )
                            Text(
                                "$pageNumber",
                                style = SuhoodTypography.titleMedium,
                                color = colors.textPrimary,
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                            GlassButton(
                                text = "Next",
                                onClick = { vm.nextPage() },
                                primary = true,
                                enabled = pageNumber < 604
                            )
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

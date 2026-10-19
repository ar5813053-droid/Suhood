package com.suhood.feature.nasheedify

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suhood.app.SuhoodApplication
import com.suhood.core.data.DemoData
import com.suhood.core.designsystem.components.DemoStatusLabel
import com.suhood.core.designsystem.components.GlassCard
import com.suhood.core.designsystem.components.GlassIconButton
import com.suhood.core.designsystem.components.GlassSearchField
import com.suhood.core.designsystem.components.GlassSegmentedControl
import com.suhood.core.designsystem.components.ScreenHeader
import com.suhood.core.designsystem.theme.SuhoodShapes
import com.suhood.core.designsystem.theme.SuhoodSpacing
import com.suhood.core.designsystem.theme.SuhoodTypography
import com.suhood.core.designsystem.theme.suhoodColors
import com.suhood.core.model.AudioTrack
import com.suhood.core.model.PlaybackState
import com.suhood.core.navigation.Routes

object NasheedifyScreens {

    @Composable
    private fun rememberNasheedifyVm(): NasheedifyViewModel {
        val app = LocalContext.current.applicationContext as SuhoodApplication
        return viewModel(
            factory = NasheedifyViewModel.Factory(
                app.playbackController,
                app.libraryRepository,
                app.audioCatalog
            )
        )
    }


    private val moodTiles = listOf(
        "سكينة" to "Calm",
        "تركيز" to "Focus",
        "صباح" to "Morning",
        "مساء" to "Evening",
        "ترتيل" to "Tilawat",
        "ذكر" to "Dhikr"
    )

    private val artworkColors = listOf(
        Color(0xFF2A3340), Color(0xFF3A2F45), Color(0xFF2F3D3A),
        Color(0xFF40352A), Color(0xFF2A3540), Color(0xFF3A2A35)
    )

    @Composable
    fun Home(
        onNavigate: (String) -> Unit,
        playbackState: PlaybackState,
        onPlaybackChange: (PlaybackState) -> Unit,
        bottomContentPadding: Dp
    ) {
        val vm = rememberNasheedifyVm()
        val sharedState by vm.playbackState.collectAsStateWithLifecycle()
        val favorites by vm.favorites.collectAsStateWithLifecycle()
        val error by vm.error.collectAsStateWithLifecycle()
        val colors = suhoodColors
        // Prefer shared controller state over local nav-held copy
        val activeState = sharedState.currentTrack?.let { sharedState } ?: playbackState
        val allTracks = remember { vm.allTracks() }
        val madeForYou = allTracks.take(6)
        val popular = allTracks.reversed()

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .padding(bottom = bottomContentPadding + if (activeState.currentTrack != null) 72.dp else 24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = SuhoodSpacing.screenHorizontal, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "السلام عليكم",
                            style = SuhoodTypography.arabicTitle,
                            color = colors.textPrimary
                        )
                        Text(
                            "Nasheedify · Islamic audio",
                            style = SuhoodTypography.bodySmall,
                            color = colors.textMuted
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassIconButton(
                            icon = Icons.Filled.Search,
                            contentDescription = "Search",
                            onClick = { onNavigate(Routes.NASHEEDIFY_SEARCH) },
                            size = 42.dp,
                            iconSize = 20.dp
                        )
                        GlassIconButton(
                            icon = Icons.Filled.LibraryMusic,
                            contentDescription = "Library",
                            onClick = { onNavigate(Routes.NASHEEDIFY_FAVORITES) },
                            size = 42.dp,
                            iconSize = 20.dp
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Quick category chips (Spotify-style top tiles)
                Text(
                    "تصفح · Browse",
                    style = SuhoodTypography.titleMedium,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(horizontal = SuhoodSpacing.screenHorizontal)
                )
                Spacer(Modifier.height(10.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = SuhoodSpacing.screenHorizontal),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(DemoData.audioCategories) { cat ->
                        val idx = DemoData.audioCategories.indexOf(cat) % artworkColors.size
                        Box(
                            modifier = Modifier
                                .width(148.dp)
                                .height(56.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(artworkColors[idx])
                                .clickable { onNavigate("audio/category/${cat.id}") }
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                cat.name,
                                style = SuhoodTypography.titleSmall,
                                color = Color.White.copy(alpha = 0.92f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(Modifier.height(28.dp))

                // Made for you — horizontal cards
                SectionTitle("لك · Made for you")
                Spacer(Modifier.height(12.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = SuhoodSpacing.screenHorizontal),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(madeForYou) { track ->
                        ArtworkCard(
                            track = track,
                            onClick = {
                                vm.play(track)
                                onNavigate(Routes.NASHEEDIFY_PLAYER)
                            }
                        )
                    }
                }

                Spacer(Modifier.height(28.dp))

                // Recommended list
                SectionTitle("موصى به · Recommended")
                Spacer(Modifier.height(8.dp))
                Column(modifier = Modifier.padding(horizontal = SuhoodSpacing.screenHorizontal)) {
                    popular.take(6).forEach { track ->
                        TrackRow(
                            track = track,
                            isFavorite = track.id in favorites,
                            onFavorite = {
                                vm.toggleFavorite(track.id)
                            },
                            onClick = {
                                vm.play(track)
                                onNavigate(Routes.NASHEEDIFY_PLAYER)
                            }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                DemoStatusLabel(
                    "Your library · sample tones only if no local music",
                    modifier = Modifier.padding(horizontal = SuhoodSpacing.screenHorizontal)
                )
            }

            // Mini player bar
            activeState.currentTrack?.let { track ->
                MiniPlayerBar(
                    track = track,
                    isPlaying = activeState.isPlaying,
                    onPlayPause = {
                        vm.togglePlayPause()
                    },
                    onOpen = { onNavigate(Routes.NASHEEDIFY_PLAYER) },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = bottomContentPadding + 8.dp)
                        .padding(horizontal = 16.dp)
                )
            }
        }
    }

    @Composable
    fun Search(onBack: () -> Unit, onTrack: (AudioTrack) -> Unit) {
        val vm = rememberNasheedifyVm()
        val favorites by vm.favorites.collectAsStateWithLifecycle()
        val colors = suhoodColors
        var query by remember { mutableStateOf("") }
        var tab by remember { mutableStateOf(0) }
        val filtered = remember(query) { vm.search(query) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            ScreenHeader(title = "بحث · Search", onBack = onBack)
            Column(modifier = Modifier.padding(horizontal = SuhoodSpacing.screenHorizontal)) {
                GlassSearchField(
                    query = query,
                    onQueryChange = { query = it },
                    placeholder = "Search nasheeds, tilawat…",
                    leadingIcon = Icons.Filled.Search
                )
                Spacer(Modifier.height(14.dp))
                GlassSegmentedControl(
                    options = listOf("Explore", "Suggestions", "Album"),
                    selectedIndex = tab,
                    onSelect = { tab = it }
                )
            }
            Spacer(Modifier.height(18.dp))

            if (query.isNotBlank()) {
                LazyColumn(
                    contentPadding = PaddingValues(
                        horizontal = SuhoodSpacing.screenHorizontal,
                        vertical = 8.dp
                    )
                ) {
                    items(filtered, key = { it.id }) { track ->
                        TrackRow(
                            track = track,
                            isFavorite = track.id in favorites,
                            onFavorite = {
                                vm.toggleFavorite(track.id)
                            },
                            onClick = { onTrack(track) }
                        )
                    }
                    if (filtered.isEmpty()) {
                        item {
                            Text(
                                "لا نتائج · No results",
                                style = SuhoodTypography.bodyMedium,
                                color = colors.textMuted,
                                modifier = Modifier.padding(24.dp)
                            )
                        }
                    }
                }
            } else {
                Text(
                    "أمزجة ولحظات · Moods & moments",
                    style = SuhoodTypography.titleMedium,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(horizontal = SuhoodSpacing.screenHorizontal)
                )
                Spacer(Modifier.height(12.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(horizontal = SuhoodSpacing.screenHorizontal),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(moodTiles.size) { i ->
                        val (ar, en) = moodTiles[i]
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(72.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(artworkColors[i % artworkColors.size])
                                .clickable { query = en }
                                .padding(16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Column {
                                Text(ar, style = SuhoodTypography.arabicBody, color = Color.White.copy(0.95f))
                                Text(en, style = SuhoodTypography.labelSmall, color = Color.White.copy(0.7f))
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun CategoryDetail(categoryId: String, onBack: () -> Unit, onTrack: (AudioTrack) -> Unit) {
        val vm = rememberNasheedifyVm()
        val favorites by vm.favorites.collectAsStateWithLifecycle()
        val cat = DemoData.audioCategories.find { it.id == categoryId }
        val tracks = vm.allTracks().filter { it.categoryId == categoryId }
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            ScreenHeader(title = cat?.name ?: "Category", onBack = onBack)
            LazyColumn(
                contentPadding = PaddingValues(
                    horizontal = SuhoodSpacing.screenHorizontal,
                    vertical = 8.dp
                )
            ) {
                items(tracks, key = { it.id }) { track ->
                    TrackRow(
                        track = track,
                        isFavorite = track.id in favorites,
                        onFavorite = {
                            vm.toggleFavorite(track.id)
                        },
                        onClick = { onTrack(track) }
                    )
                }
            }
        }
    }

    @Composable
    fun Favorites(onBack: () -> Unit, onTrack: (AudioTrack) -> Unit) {
        val vm = rememberNasheedifyVm()
        val favorites by vm.favorites.collectAsStateWithLifecycle()
        val recentIds by vm.recent.collectAsStateWithLifecycle()
        val colors = suhoodColors
        var tab by remember { mutableStateOf(0) }
        val liked = vm.allTracks().filter { it.id in favorites }

        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            ScreenHeader(title = "مكتبتي · Library", onBack = onBack)
            Column(Modifier.padding(horizontal = SuhoodSpacing.screenHorizontal)) {
                GlassSegmentedControl(
                    options = listOf("Playlists", "Songs", "Albums"),
                    selectedIndex = tab,
                    onSelect = { tab = it }
                )
            }
            Spacer(Modifier.height(16.dp))

            when (tab) {
                0 -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(horizontal = SuhoodSpacing.screenHorizontal),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val tiles = listOf(
                            "Liked" to Icons.Filled.FavoriteBorder,
                            "Favorites" to Icons.Filled.Favorite,
                            "Tilawat" to Icons.Filled.LibraryMusic,
                            "Adhkar" to Icons.Filled.LibraryMusic,
                            "Nasheeds" to Icons.Filled.LibraryMusic,
                            "Local" to Icons.Filled.LibraryMusic
                        )
                        items(tiles.size) { i ->
                            val (label, icon) = tiles[i]
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colors.surfaceElevated)
                                        .clickable {
                                            if (i == 0 || i == 1) tab = 1
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(icon, null, tint = colors.silver, modifier = Modifier.size(28.dp))
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(label, style = SuhoodTypography.labelSmall, color = colors.textSecondary)
                            }
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = SuhoodSpacing.screenHorizontal)
                    ) {
                        val list = if (liked.isEmpty()) vm.allTracks().take(8) else liked
                        items(list, key = { it.id }) { track ->
                            TrackRow(
                                track = track,
                                isFavorite = track.id in favorites,
                                onFavorite = {
                                    vm.toggleFavorite(track.id)
                                },
                                onClick = { onTrack(track) }
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun FullPlayer(
        state: PlaybackState,
        onStateChange: (PlaybackState) -> Unit,
        onBack: () -> Unit
    ) {
        val vm = rememberNasheedifyVm()
        val shared by vm.playbackState.collectAsStateWithLifecycle()
        val favorites by vm.favorites.collectAsStateWithLifecycle()
        val colors = suhoodColors
        val active = if (shared.currentTrack != null) shared else state
        val track = active.currentTrack ?: return
        val duration = track.durationSeconds * 1000L
        val progress = if (duration > 0) (active.positionMs.toFloat() / duration).coerceIn(0f, 1f) else 0f
        val artColor = artworkColors[track.id.hashCode().mod(artworkColors.size).let { if (it < 0) it + artworkColors.size else it }]

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(artColor.copy(alpha = 0.55f), colors.background, colors.background)
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        onClick = onBack,
                        size = 40.dp,
                        iconSize = 20.dp
                    )
                    Text("Now playing", style = SuhoodTypography.labelMedium, color = colors.textMuted)
                    GlassIconButton(
                        icon = if (track.id in favorites) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Favorite",
                        onClick = {
                            vm.toggleFavorite(track.id)
                        },
                        size = 40.dp,
                        iconSize = 20.dp
                    )
                }

                Spacer(Modifier.height(24.dp))

                // Large artwork
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.82f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(artColor, artColor.copy(alpha = 0.6f), colors.surfaceElevated)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        track.titleAr.ifBlank { track.title.take(1) },
                        style = SuhoodTypography.displaySmall,
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(Modifier.height(28.dp))

                Text(
                    track.title,
                    style = SuhoodTypography.headlineMedium,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (track.titleAr.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        track.titleAr,
                        style = SuhoodTypography.arabicBody,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    track.creator + if (track.creatorAr.isNotBlank()) " · ${track.creatorAr}" else "",
                    style = SuhoodTypography.bodyMedium,
                    color = colors.textMuted
                )

                Spacer(Modifier.height(28.dp))

                // Progress (demo)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(CircleShape),
                    color = colors.silverBright,
                    trackColor = colors.surfaceSelected
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("0:00", style = SuhoodTypography.labelSmall, color = colors.textMuted)
                    Text(formatDur(track.durationSeconds), style = SuhoodTypography.labelSmall, color = colors.textMuted)
                }

                Spacer(Modifier.height(28.dp))

                // Transport
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassIconButton(
                        icon = Icons.Filled.SkipPrevious,
                        contentDescription = "Previous",
                        onClick = { vm.previous() },
                        size = 52.dp,
                        iconSize = 28.dp
                    )
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(colors.silverBright)
                            .clickable {
                                vm.togglePlayPause()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (active.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (active.isPlaying) "Pause" else "Play",
                            tint = colors.background,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    GlassIconButton(
                        icon = Icons.Filled.SkipNext,
                        contentDescription = "Next",
                        onClick = { vm.next() },
                        size = 52.dp,
                        iconSize = 28.dp
                    )
                }

                Spacer(Modifier.height(20.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Filled.VolumeUp, null, tint = colors.textMuted, modifier = Modifier.size(18.dp))
                    LinearProgressIndicator(
                        progress = { 0.65f },
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(CircleShape),
                        color = colors.silver,
                        trackColor = colors.surfaceSelected
                    )
                }

                Spacer(Modifier.height(16.dp))
                DemoStatusLabel("Playing · Media3")
            }
        }
    }

    @Composable
    fun Queue(state: PlaybackState, onBack: () -> Unit, onTrack: (AudioTrack) -> Unit) {
        val vm = rememberNasheedifyVm()
        val favorites by vm.favorites.collectAsStateWithLifecycle()

        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            ScreenHeader(title = "Queue", onBack = onBack)
            LazyColumn(contentPadding = PaddingValues(horizontal = SuhoodSpacing.screenHorizontal)) {
                items(state.queue.ifEmpty { vm.allTracks() }, key = { it.id }) { track ->
                    TrackRow(
                        track = track,
                        isFavorite = track.id in favorites,
                        onFavorite = { vm.toggleFavorite(track.id) },
                        onClick = { onTrack(track) }
                    )
                }
            }
        }
    }

    @Composable
    fun CreatePlaylist(onBack: () -> Unit) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            ScreenHeader(title = "New playlist", onBack = onBack)
            Text(
                "Playlists are saved on this device.",
                style = SuhoodTypography.bodyMedium,
                color = suhoodColors.textSecondary,
                modifier = Modifier.padding(SuhoodSpacing.screenHorizontal)
            )
        }
    }

    /* ── Shared pieces ── */

    @Composable
    private fun SectionTitle(text: String) {
        Text(
            text,
            style = SuhoodTypography.titleLarge,
            color = suhoodColors.textPrimary,
            modifier = Modifier.padding(horizontal = SuhoodSpacing.screenHorizontal)
        )
    }

    @Composable
    private fun ArtworkCard(track: AudioTrack, onClick: () -> Unit) {
        val colors = suhoodColors
        val art = artworkColors[track.id.hashCode().mod(artworkColors.size).let { if (it < 0) it + artworkColors.size else it }]
        Column(
            modifier = Modifier
                .width(148.dp)
                .clickable(onClick = onClick)
        ) {
            Box(
                modifier = Modifier
                    .size(148.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(listOf(art, art.copy(alpha = 0.7f)))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    track.titleAr.ifBlank { track.title.take(2) },
                    style = SuhoodTypography.titleMedium,
                    color = Color.White.copy(0.9f),
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                track.title,
                style = SuhoodTypography.titleSmall,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                track.creator,
                style = SuhoodTypography.bodySmall,
                color = colors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    @Composable
    private fun TrackRow(
        track: AudioTrack,
        isFavorite: Boolean,
        onFavorite: () -> Unit,
        onClick: () -> Unit
    ) {
        val colors = suhoodColors
        val art = artworkColors[track.id.hashCode().mod(artworkColors.size).let { if (it < 0) it + artworkColors.size else it }]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(art),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    track.title.take(1),
                    style = SuhoodTypography.titleMedium,
                    color = Color.White.copy(0.9f)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    track.title,
                    style = SuhoodTypography.titleSmall,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (track.titleAr.isNotBlank()) {
                    Text(
                        track.titleAr,
                        style = SuhoodTypography.bodySmall,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    "${track.creator} · ${formatDur(track.durationSeconds)}",
                    style = SuhoodTypography.labelSmall,
                    color = colors.textMuted,
                    maxLines = 1
                )
            }
            Icon(
                if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = "Favorite",
                tint = if (isFavorite) colors.silverBright else colors.textMuted,
                modifier = Modifier
                    .size(22.dp)
                    .clickable(onClick = onFavorite)
            )
        }
    }

    @Composable
    private fun MiniPlayerBar(
        track: AudioTrack,
        isPlaying: Boolean,
        onPlayPause: () -> Unit,
        onOpen: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        val colors = suhoodColors
        Row(
            modifier = modifier
                .fillMaxWidth()
                .clip(SuhoodShapes.pill)
                .background(colors.surfaceElevated.copy(alpha = 0.96f))
                .clickable(onClick = onOpen)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(artworkColors[0]),
                contentAlignment = Alignment.Center
            ) {
                Text(track.title.take(1), style = SuhoodTypography.labelMedium, color = Color.White)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(track.title, style = SuhoodTypography.titleSmall, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(track.creator, style = SuhoodTypography.labelSmall, color = colors.textMuted, maxLines = 1)
            }
            Icon(
                if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = "Play pause",
                tint = colors.silverBright,
                modifier = Modifier
                    .size(28.dp)
                    .clickable(onClick = onPlayPause)
            )
        }
    }

    private fun formatDur(sec: Int): String {
        val m = sec / 60
        val s = sec % 60
        return "%d:%02d".format(m, s)
    }
}

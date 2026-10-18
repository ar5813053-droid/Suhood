package com.suhood.feature.nasheedify

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.suhood.core.data.DemoData
import com.suhood.core.designsystem.components.*
import com.suhood.core.designsystem.theme.SuhoodShapes
import com.suhood.core.designsystem.theme.SuhoodSpacing
import com.suhood.core.designsystem.theme.SuhoodTypography
import com.suhood.core.designsystem.theme.suhoodColors
import com.suhood.core.model.AudioTrack
import com.suhood.core.model.PlaybackState
import com.suhood.core.navigation.Routes

object NasheedifyScreens {

    private val favorites = mutableStateListOf<String>()

    @Composable
    fun Home(
        onNavigate: (String) -> Unit,
        playbackState: PlaybackState,
        onPlaybackChange: (PlaybackState) -> Unit,
        bottomContentPadding: Dp
    ) {
        val colors = suhoodColors
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                .padding(bottom = bottomContentPadding)
                    .padding(bottom = if (playbackState.currentTrack != null) 80.dp else 32.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = SuhoodSpacing.screenHorizontal),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Nasheedify", style = SuhoodTypography.headlineLarge, color = colors.textPrimary)
                        Text("Islamic audio · Demo catalog", style = SuhoodTypography.bodySmall, color = colors.textMuted)
                    }
                    GlassIconButton(
                        icon = Icons.Filled.Search,
                        contentDescription = "Search",
                        onClick = { onNavigate(Routes.NASHEEDIFY_SEARCH) }
                    )
                }
                Spacer(Modifier.height(16.dp))

                Text(
                    "Categories",
                    style = SuhoodTypography.titleMedium,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(horizontal = SuhoodSpacing.screenHorizontal)
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = SuhoodSpacing.screenHorizontal),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(DemoData.audioCategories) { cat ->
                        GlassCard(
                            onClick = { onNavigate("audio/category/${cat.id}") },
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            Text(cat.name, style = SuhoodTypography.titleSmall, color = colors.textPrimary)
                            Text("${cat.trackCount} tracks", style = SuhoodTypography.labelSmall, color = colors.textMuted)
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                Text(
                    "Recommended",
                    style = SuhoodTypography.titleMedium,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(horizontal = SuhoodSpacing.screenHorizontal)
                )
                Spacer(Modifier.height(8.dp))
                DemoData.audioTracks.take(6).forEach { track ->
                    TrackRowItem(
                        track = track,
                        isFavorite = track.id in favorites,
                        onClick = {
                            onPlaybackChange(
                                playbackState.copy(
                                    currentTrack = track,
                                    isPlaying = true,
                                    isDemoPlayback = true,
                                    queue = DemoData.audioTracks,
                                    queueIndex = DemoData.audioTracks.indexOf(track)
                                )
                            )
                        },
                        onFavorite = {
                            if (track.id in favorites) favorites.remove(track.id)
                            else favorites.add(track.id)
                        },
                        modifier = Modifier.padding(horizontal = SuhoodSpacing.screenHorizontal, vertical = 3.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.padding(horizontal = SuhoodSpacing.screenHorizontal),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GlassPill(onClick = { onNavigate(Routes.NASHEEDIFY_FAVORITES) }) {
                        Text("Favorites", style = SuhoodTypography.labelMedium, color = colors.textSecondary)
                    }
                    GlassPill(onClick = { onNavigate(Routes.NASHEEDIFY_CREATE_PLAYLIST) }) {
                        Text("Playlists", style = SuhoodTypography.labelMedium, color = colors.textSecondary)
                    }
                    GlassPill(onClick = { onNavigate(Routes.NASHEEDIFY_QUEUE) }) {
                        Text("Queue", style = SuhoodTypography.labelMedium, color = colors.textSecondary)
                    }
                }
            }

            if (playbackState.currentTrack != null) {
                MiniPlayerBar(
                    state = playbackState,
                    onToggle = {
                        onPlaybackChange(playbackState.copy(isPlaying = !playbackState.isPlaying))
                    },
                    onOpen = { onNavigate(Routes.NASHEEDIFY_PLAYER) },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }

    @Composable
    fun Search(onBack: () -> Unit, onTrack: (AudioTrack) -> Unit) {
        var query by remember { mutableStateOf("") }
        val results = remember(query) {
            if (query.isBlank()) emptyList()
            else DemoData.audioTracks.filter {
                it.title.contains(query, true) || it.creator.contains(query, true) || it.collection.contains(query, true)
            }
        }
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            ScreenHeader(title = "Search", onBack = onBack)
            GlassSearchField(
                query = query,
                onQueryChange = { query = it },
                placeholder = "Tracks, creators…",
                leadingIcon = Icons.Filled.Search,
                modifier = Modifier.padding(horizontal = SuhoodSpacing.screenHorizontal)
            )
            Spacer(Modifier.height(12.dp))
            if (query.isNotBlank() && results.isEmpty()) {
                EmptyState("No results", "Try a different search term.")
            } else {
                LazyColumn(contentPadding = PaddingValues(horizontal = SuhoodSpacing.screenHorizontal)) {
                    items(results, key = { it.id }) { track ->
                        TrackRowItem(track = track, isFavorite = track.id in favorites, onClick = { onTrack(track) }, onFavorite = {})
                    }
                }
            }
        }
    }

    @Composable
    fun CategoryDetail(categoryId: String, onBack: () -> Unit, onTrack: (AudioTrack) -> Unit) {
        val cat = DemoData.audioCategories.find { it.id == categoryId }
        val tracks = DemoData.audioTracks.filter { it.categoryId == categoryId }
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            ScreenHeader(title = cat?.name ?: "Category", onBack = onBack)
            LazyColumn(contentPadding = PaddingValues(horizontal = SuhoodSpacing.screenHorizontal, vertical = 8.dp)) {
                items(tracks, key = { it.id }) { track ->
                    TrackRowItem(
                        track = track,
                        isFavorite = track.id in favorites,
                        onClick = { onTrack(track) },
                        onFavorite = {
                            if (track.id in favorites) favorites.remove(track.id) else favorites.add(track.id)
                        }
                    )
                }
            }
        }
    }

    @Composable
    fun Favorites(onBack: () -> Unit, onTrack: (AudioTrack) -> Unit) {
        val tracks = DemoData.audioTracks.filter { it.id in favorites }
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            ScreenHeader(title = "Favorites", onBack = onBack)
            if (tracks.isEmpty()) {
                EmptyState("No favorites", "Tap the heart on a track to save it here.")
            } else {
                LazyColumn(contentPadding = PaddingValues(horizontal = SuhoodSpacing.screenHorizontal)) {
                    items(tracks, key = { it.id }) { track ->
                        TrackRowItem(track = track, isFavorite = true, onClick = { onTrack(track) }, onFavorite = {
                            favorites.remove(track.id)
                        })
                    }
                }
            }
        }
    }

    @Composable
    fun FullPlayer(state: PlaybackState, onStateChange: (PlaybackState) -> Unit, onBack: () -> Unit) {
        val colors = suhoodColors
        val track = state.currentTrack
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(SuhoodSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ScreenHeader(title = "Now playing", onBack = onBack)
            Spacer(Modifier.height(24.dp))
            GlassCard(modifier = Modifier.size(220.dp)) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("♪", style = SuhoodTypography.displayLarge, color = colors.silver)
                }
            }
            Spacer(Modifier.height(24.dp))
            if (track != null) {
                Text(track.title, style = SuhoodTypography.headlineSmall, color = colors.textPrimary)
                Text(track.creator, style = SuhoodTypography.bodyMedium, color = colors.textSecondary)
                Spacer(Modifier.height(8.dp))
                if (state.isDemoPlayback) DemoStatusLabel("Demo playback — no audio stream")
            }
            Spacer(Modifier.height(32.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassIconButton(
                    icon = Icons.Filled.SkipPrevious,
                    contentDescription = "Previous",
                    onClick = {
                        val idx = (state.queueIndex - 1).coerceAtLeast(0)
                        val t = state.queue.getOrNull(idx)
                        if (t != null) onStateChange(state.copy(currentTrack = t, queueIndex = idx))
                    }
                )
                GlassIconButton(
                    icon = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                    onClick = { onStateChange(state.copy(isPlaying = !state.isPlaying)) },
                    size = 64.dp,
                    iconSize = 32.dp
                )
                GlassIconButton(
                    icon = Icons.Filled.SkipNext,
                    contentDescription = "Next",
                    onClick = {
                        val idx = (state.queueIndex + 1).coerceAtMost(state.queue.lastIndex.coerceAtLeast(0))
                        val t = state.queue.getOrNull(idx)
                        if (t != null) onStateChange(state.copy(currentTrack = t, queueIndex = idx))
                    }
                )
            }
        }
    }

    @Composable
    fun Queue(state: PlaybackState, onBack: () -> Unit) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            ScreenHeader(title = "Queue", onBack = onBack)
            if (state.queue.isEmpty()) {
                EmptyState("Queue empty", "Play a track to build a queue.")
            } else {
                LazyColumn(contentPadding = PaddingValues(horizontal = SuhoodSpacing.screenHorizontal)) {
                    items(state.queue, key = { it.id }) { track ->
                        TrackRowItem(track = track, isFavorite = track.id in favorites, onClick = {}, onFavorite = {})
                    }
                }
            }
        }
    }

    @Composable
    fun CreatePlaylist(onBack: () -> Unit) {
        var name by remember { mutableStateOf("") }
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = SuhoodSpacing.screenHorizontal)
        ) {
            ScreenHeader(title = "New playlist", onBack = onBack)
            Spacer(Modifier.height(16.dp))
            GlassSearchField(query = name, onQueryChange = { name = it }, placeholder = "Playlist name")
            Spacer(Modifier.height(24.dp))
            GlassButton(
                text = "Create",
                onClick = onBack,
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    @Composable
    private fun TrackRowItem(
        track: AudioTrack,
        isFavorite: Boolean,
        onClick: () -> Unit,
        onFavorite: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        val colors = suhoodColors
        GlassCard(modifier = modifier.fillMaxWidth(), onClick = onClick, contentPadding = PaddingValues(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(track.title, style = SuhoodTypography.titleSmall, color = colors.textPrimary)
                    Text(
                        "${track.creator} · ${formatDuration(track.durationSeconds)}",
                        style = SuhoodTypography.bodySmall,
                        color = colors.textMuted
                    )
                }
                GlassIconButton(
                    icon = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = if (isFavorite) "Unfavorite" else "Favorite",
                    onClick = onFavorite,
                    size = 40.dp,
                    iconSize = 18.dp
                )
            }
        }
    }

    @Composable
    private fun MiniPlayerBar(
        state: PlaybackState,
        onToggle: () -> Unit,
        onOpen: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        val colors = suhoodColors
        val track = state.currentTrack ?: return
        GlassSurface(
            modifier = modifier
                .fillMaxWidth()
                .padding(12.dp),
            shape = SuhoodShapes.cardSmall,
            elevated = true
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .height(48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onOpen)
                ) {
                    Text(track.title, style = SuhoodTypography.titleSmall, color = colors.textPrimary, maxLines = 1)
                    Text(
                        if (state.isDemoPlayback) "Demo" else track.creator,
                        style = SuhoodTypography.labelSmall,
                        color = colors.textMuted
                    )
                }
                GlassIconButton(
                    icon = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = "Play/Pause",
                    onClick = onToggle,
                    size = 40.dp
                )
            }
        }
    }

    private fun formatDuration(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return "%d:%02d".format(m, s)
    }
}

package com.suhood.feature.books

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.suhood.core.designsystem.components.SkeletonBookCard
import com.suhood.core.designsystem.components.SkeletonCategoryRow
import com.suhood.core.designsystem.components.SkeletonReaderPage
import com.suhood.core.designsystem.components.SkeletonTopPicksRow
import com.suhood.core.designsystem.theme.SuhoodTypography
import com.suhood.core.designsystem.theme.suhoodColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

private val Bg = Color(0xFF08090B)
private val Surface = Color(0xFF141519)
private val Elevated = Color(0xFF1C1E24)
private val TextPrimary = Color(0xFFF5F6F7)
private val TextSecondary = Color(0xFFB8BDC6)
private val TextMuted = Color(0xFF8B919A)
private val Border = Color.White.copy(alpha = 0.08f)
private val Accent = Color(0xFFC9CDD4)

/**
 * Premium Islamic bookstore home.
 * Hadith collections are integrated here — no separate Hadith destination required.
 */
@Composable
fun BooksDiscoverScreen(
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
    onOpenHadith: (String) -> Unit = {},
    onOpenArchive: (String) -> Unit = {}
) {
    val colors = suhoodColors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var activeCategory by remember { mutableStateOf("All") }
    var loading by remember { mutableStateOf(true) }
    var loadingMore by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var page by remember { mutableIntStateOf(1) }
    var archiveBooks by remember { mutableStateOf<List<CatalogBook>>(emptyList()) }
    var showOffline by remember { mutableStateOf(false) }

    val hadith = remember { BooksCatalog.hadithBooks() }

    fun loadPage(reset: Boolean) {
        scope.launch {
            if (reset) {
                loading = true
                page = 1
                error = null
            } else {
                loadingMore = true
            }
            try {
                val needsNet = activeCategory != "Hadith"
                if (needsNet && !BooksCatalog.isNetworkAvailable(context)) {
                    if (reset) {
                        archiveBooks = emptyList()
                        showOffline = true
                    }
                    return@launch
                }
                if (activeCategory == "Hadith" && query.isBlank()) {
                    archiveBooks = emptyList()
                    return@launch
                }
                val q = when {
                    query.isNotBlank() -> query
                    else -> BooksCatalog.archiveQueryForCategory(activeCategory)
                }
                val nextPage = if (reset) 1 else page + 1
                val result = withContext(Dispatchers.IO) {
                    InternetArchiveBooks.search(q, rows = 18, page = nextPage)
                        .map { BooksCatalog.toCatalog(it, activeCategory) }
                }
                archiveBooks = if (reset) result else archiveBooks + result
                page = nextPage
            } catch (e: Exception) {
                if (!BooksCatalog.isNetworkAvailable(context)) showOffline = true
                else error = e.message ?: "Could not load books"
            } finally {
                loading = false
                loadingMore = false
            }
        }
    }

    LaunchedEffect(activeCategory) { loadPage(reset = true) }

    val displayed: List<CatalogBook> = remember(activeCategory, query, archiveBooks, hadith) {
        val base = when {
            activeCategory == "Hadith" && query.isBlank() -> hadith
            activeCategory == "Hadith" -> hadith.filter {
                it.title.contains(query, true) || it.author.contains(query, true) || it.description.contains(query, true)
            }
            activeCategory == "All" && query.isBlank() -> hadith.take(4) + archiveBooks
            else -> archiveBooks
        }
        if (query.isBlank() || activeCategory == "Hadith") base
        else base.filter {
            it.title.contains(query, true) || it.author.contains(query, true) || it.description.contains(query, true)
        }
    }

    val topPicks = remember(hadith, archiveBooks) {
        (hadith.take(3) + archiveBooks.take(5)).distinctBy { it.id }.take(8)
    }

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
                Text("Books", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "Explore Islamic knowledge, one book at a time.",
                    color = TextMuted,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search books, authors, topics", color = TextMuted, fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = TextMuted) },
            trailingIcon = {
                if (query.isNotBlank()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextMuted)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedBorderColor = Border.copy(alpha = 0.4f),
                unfocusedBorderColor = Border,
                focusedContainerColor = Surface,
                unfocusedContainerColor = Surface,
                cursorColor = Accent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 40.dp)) {
            if (query.isBlank()) {
                item {
                    SectionTitle("Top Picks")
                }
                item {
                    if (loading && topPicks.isEmpty()) {
                        SkeletonTopPicksRow()
                    } else {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(topPicks, key = { it.id }) { book ->
                                TopPickCard(book) {
                                    openBook(book, onOpenHadith, onOpenArchive) { showOffline = true }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                }

                item { SectionTitle("Browse Categories") }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(BooksCatalog.categories.filter { it != "All" }) { cat ->
                            CategoryChip(
                                label = cat,
                                selected = activeCategory == cat,
                                onClick = { activeCategory = cat }
                            )
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                }
            }

            item {
                SectionTitle(
                    when {
                        query.isNotBlank() -> "Results"
                        activeCategory == "All" -> "Popular & Library"
                        else -> activeCategory
                    }
                )
            }

            if (loading && displayed.isEmpty()) {
                items(6) { SkeletonBookCard() }
            } else if (displayed.isEmpty()) {
                item {
                    Text(
                        error ?: "No books found. Try another category or search.",
                        color = TextMuted,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)
                    )
                    if (error != null) {
                        TextButton(onClick = { loadPage(true) }, modifier = Modifier.padding(horizontal = 12.dp)) {
                            Text("Try again", color = Accent)
                        }
                    }
                }
            } else {
                items(displayed, key = { it.id }) { book ->
                    BookRowCard(book) {
                        openBook(book, onOpenHadith, onOpenArchive) { showOffline = true }
                    }
                }
                if (activeCategory != "Hadith") {
                    item {
                        Spacer(Modifier.height(8.dp))
                        TextButton(
                            onClick = { if (!loadingMore) loadPage(false) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Text(if (loadingMore) "Loading…" else "Load more", color = Accent)
                        }
                    }
                }
            }
        }
    }

    if (showOffline) {
        NoInternetDialog(
            onRetry = {
                showOffline = false
                loadPage(true)
            },
            onClose = { showOffline = false }
        )
    }
}

private fun openBook(
    book: CatalogBook,
    onOpenHadith: (String) -> Unit,
    onOpenArchive: (String) -> Unit,
    onNeedNetwork: () -> Unit
) {
    when (book.sourceType) {
        BookSourceType.HADITH -> onOpenHadith(book.openKey)
        BookSourceType.ARCHIVE -> onOpenArchive(book.openKey)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        color = TextSecondary,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.3.sp,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 10.dp)
    )
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) Elevated else Surface
    val border = if (selected) Color(0xFF6B9EFF).copy(alpha = 0.5f) else Border
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(label, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun TopPickCard(book: CatalogBook, onClick: () -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .width(132.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Surface)
            .border(1.dp, Border, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(10.dp)
    ) {
        if (book.coverUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(book.coverUrl).crossfade(true).build(),
                contentDescription = book.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Elevated),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Elevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.MenuBook, null, tint = TextMuted, modifier = Modifier.size(36.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(book.title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(book.author, color = TextMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(book.category, color = TextSecondary, fontSize = 11.sp, maxLines = 1)
    }
}

@Composable
private fun BookRowCard(book: CatalogBook, onClick: () -> Unit) {
    val context = LocalContext.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Surface)
            .border(1.dp, Border, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (book.coverUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(book.coverUrl).crossfade(true).build(),
                contentDescription = book.title,
                modifier = Modifier
                    .width(64.dp)
                    .height(90.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Elevated),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                Modifier
                    .width(64.dp)
                    .height(90.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Elevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.MenuBook, null, tint = TextMuted)
            }
        }
        Column(Modifier.weight(1f)) {
            Text(book.title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(book.author, color = TextMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(book.category, color = TextSecondary, fontSize = 11.sp)
            if (book.description.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(book.description, color = TextMuted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 16.sp)
            }
        }
    }
}

@Composable
fun NoInternetDialog(onRetry: () -> Unit, onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        containerColor = Surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.WifiOff, null, tint = TextSecondary)
                Spacer(Modifier.width(8.dp))
                Text("No Internet Connection", color = TextPrimary, fontWeight = FontWeight.SemiBold)
            }
        },
        text = {
            Text(
                "Connect to the internet to access this book. Please check your connection and try again.",
                color = TextSecondary,
                fontSize = 14.sp
            )
        },
        confirmButton = {
            TextButton(onClick = onRetry) { Text("Try Again", color = Accent) }
        },
        dismissButton = {
            TextButton(onClick = onClose) { Text("Close", color = TextMuted) }
        }
    )
}

/**
 * Detail is no longer a download gate — opens the reader path immediately.
 */
@Composable
fun BookDetailScreen(identifier: String, onBack: () -> Unit, onRead: (String) -> Unit) {
    LaunchedEffect(identifier) {
        onRead(identifier)
    }
    Box(Modifier.fillMaxSize().background(Bg), contentAlignment = Alignment.Center) {
        SkeletonReaderPage()
    }
}


/**
 * Multi-format reader: Archive.org BookReader (PDF / DjVu / images) in WebView,
 * plus optional cleaned text tab for TTS.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookReaderScreen(identifier: String, onBack: () -> Unit) {
    val colors = suhoodColors
    val context = LocalContext.current
    var tab by remember { mutableIntStateOf(0) } // 0 = pages (WebView), 1 = text
    var title by remember { mutableStateOf("Reader") }
    var plainText by remember { mutableStateOf("") }
    var textLoading by remember { mutableStateOf(false) }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var speaking by remember { mutableStateOf(false) }
    var webLoading by remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                engine?.language = Locale.US
                tts = engine
            }
        }
        onDispose {
            engine?.stop()
            engine?.shutdown()
        }
    }

    LaunchedEffect(identifier) {
        title = withContext(Dispatchers.IO) {
            runCatching { InternetArchiveBooks.metadata(identifier).title }.getOrDefault("Reader")
        }
    }

    LaunchedEffect(tab, identifier) {
        if (tab == 1 && plainText.isBlank()) {
            textLoading = true
            plainText = withContext(Dispatchers.IO) {
                runCatching {
                    InternetArchiveBooks.loadReadableText(identifier).plainText
                        .let { InternetArchiveBooks.cleanOcrNoise(it) }
                }.getOrElse { "Text layer unavailable: ${it.message}" }
            }
            textLoading = false
        }
    }

    val streamUrl = "https://archive.org/stream/$identifier"
    val embedUrl = "https://archive.org/embed/$identifier"

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = colors.accentLavender)
                    }
                },
                actions = {
                    if (tab == 1) {
                        IconButton(onClick = {
                            val e = tts ?: return@IconButton
                            if (speaking) {
                                e.stop(); speaking = false
                            } else {
                                val chunk = plainText.take(3900)
                                if (chunk.isNotBlank()) {
                                    e.speak(chunk, TextToSpeech.QUEUE_FLUSH, null, "suhood-book")
                                    speaking = true
                                }
                            }
                        }) {
                            Icon(if (speaking) Icons.Filled.Stop else Icons.Filled.RecordVoiceOver, null, tint = colors.accentLavender)
                        }
                    }
                    IconButton(onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://archive.org/details/$identifier")))
                    }) {
                        Icon(Icons.Filled.OpenInBrowser, null, tint = colors.textSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background)
            )
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            TabRow(
                selectedTabIndex = tab,
                containerColor = colors.surface,
                contentColor = colors.accentLavender
            ) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Pages") }, icon = { Icon(Icons.Filled.AutoStories, null) })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Text / TTS") }, icon = { Icon(Icons.Filled.TextFields, null) })
            }
            when (tab) {
                0 -> {
                    Box(Modifier.fillMaxSize()) {
                        ArchiveBookWebView(
                            url = streamUrl,
                            fallbackUrl = embedUrl,
                            onLoading = { webLoading = it }
                        )
                        if (webLoading) {
                            Box(Modifier.fillMaxSize().background(colors.background.copy(alpha = 0.92f))) {
                                SkeletonReaderPage()
                            }
                        }
                    }
                }
                else -> {
                    if (textLoading) {
                        SkeletonReaderPage()
                    } else {
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                            Text(
                                "Cleaned text for TTS. OCR scans may still contain noise. Prefer Pages tab for PDF/DjVu layout.",
                                style = SuhoodTypography.labelSmall,
                                color = colors.textMuted
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(plainText, color = colors.textPrimary, fontSize = 16.sp, lineHeight = 24.sp)
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun ArchiveBookWebView(url: String, fallbackUrl: String, onLoading: (Boolean) -> Unit) {
    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                settings.userAgentString = settings.userAgentString + " Suhood/1.0"
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, u: String?) {
                        onLoading(false)
                    }
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        return false
                    }
                }
                onLoading(true)
                loadUrl(url)
            }
        },
        modifier = Modifier.fillMaxSize(),
        update = { /* keep */ }
    )
}

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
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.suhood.core.designsystem.theme.SuhoodTypography
import com.suhood.core.designsystem.theme.suhoodColors
import com.suhood.core.navigation.Routes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BooksDiscoverScreen(onNavigate: (String) -> Unit, onBack: () -> Unit) {
    val colors = suhoodColors
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("Islam") }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var books by remember { mutableStateOf<List<IaBook>>(emptyList()) }
    var activeChip by remember { mutableStateOf("All") }

    fun load(q: String) {
        scope.launch {
            loading = true
            error = null
            try {
                books = withContext(Dispatchers.IO) { InternetArchiveBooks.search(q) }
            } catch (e: Exception) {
                error = e.message ?: "Could not load books"
                books = emptyList()
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) { load(query) }
    val chips = listOf("All", "Arabic", "English", "Urdu", "Quran", "Hadith")

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Suhood Books", fontWeight = FontWeight.Bold, color = colors.textPrimary)
                        Text("Islamic Knowledge Library", style = SuhoodTypography.labelSmall, color = colors.textMuted)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = colors.accentLavender)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background)
            )
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text("Discover Islamic Books", style = SuhoodTypography.titleLarge.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary)
                Text("Free public texts from the Internet Archive", style = SuhoodTypography.bodySmall, color = colors.textMuted)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search books, authors, topics…", color = colors.textMuted) },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.Search, null, tint = colors.accentLavender) },
                    trailingIcon = {
                        IconButton(onClick = { load(query) }) {
                            Icon(Icons.Filled.ArrowForward, null, tint = colors.accentLavender)
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.accentLavender,
                        unfocusedBorderColor = colors.borderSubtle,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        cursorColor = colors.accentLavender,
                        focusedContainerColor = colors.surface,
                        unfocusedContainerColor = colors.surface
                    )
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Source: Internet Archive · Public listings only. Restricted items are not force-downloaded.",
                    style = SuhoodTypography.labelSmall,
                    color = colors.textMuted
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                chips.forEach { chip ->
                    val active = activeChip == chip
                    FilterChip(
                        selected = active,
                        onClick = {
                            activeChip = chip
                            val q = when (chip) {
                                "All" -> query.ifBlank { "Islam" }
                                "Arabic" -> "language:Arabic OR language:ara"
                                "English" -> "language:English OR language:eng"
                                "Urdu" -> "language:Urdu OR language:urd"
                                else -> chip
                            }
                            load(q)
                        },
                        label = { Text(chip) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.accentLavender.copy(alpha = 0.2f),
                            selectedLabelColor = colors.accentLavender,
                            containerColor = colors.surface,
                            labelColor = colors.textSecondary
                        )
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                if (loading) "Searching…" else "${books.size} books found",
                Modifier.padding(horizontal = 16.dp),
                style = SuhoodTypography.labelSmall,
                color = colors.textMuted
            )
            Spacer(Modifier.height(8.dp))
            when {
                loading -> BooksLoadingShimmer()
                error != null -> Text(error!!, Modifier.padding(16.dp), color = colors.error)
                books.isEmpty() -> Text("No books found.", Modifier.padding(16.dp), color = colors.textSecondary)
                else -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(books, key = { _, b -> b.identifier }) { index, book ->
                        BookListCard(book, index == 0) {
                            onNavigate(Routes.BOOKS_DETAIL.replace("{identifier}", Uri.encode(book.identifier)))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BooksLoadingShimmer() {
    val colors = suhoodColors
    val infinite = rememberInfiniteTransition(label = "sh")
    val alpha by infinite.animateFloat(0.35f, 0.8f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "a")
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(5) {
            Box(Modifier.fillMaxWidth().height(88.dp).clip(RoundedCornerShape(16.dp)).background(colors.surface.copy(alpha = alpha)))
        }
    }
}

@Composable
private fun BookListCard(book: IaBook, featured: Boolean, onClick: () -> Unit) {
    val colors = suhoodColors
    val context = LocalContext.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .border(1.dp, colors.borderSubtle, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context).data(book.coverUrl).crossfade(true).build(),
            contentDescription = book.title,
            modifier = Modifier.width(52.dp).height(68.dp).clip(RoundedCornerShape(8.dp)).background(colors.surfaceElevated),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            if (featured) {
                Text("FEATURED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = colors.accentLavender)
            }
            Text(book.title, maxLines = 2, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold, color = colors.textPrimary, fontSize = 14.sp)
            Spacer(Modifier.height(4.dp))
            Text(book.creator, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 11.sp, color = colors.textSecondary)
        }
        Icon(Icons.Filled.ChevronRight, null, tint = colors.textMuted)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(identifier: String, onBack: () -> Unit, onRead: (String) -> Unit) {
    val colors = suhoodColors
    val context = LocalContext.current
    var book by remember { mutableStateOf<IaBook?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(identifier) {
        loading = true
        try {
            book = withContext(Dispatchers.IO) { InternetArchiveBooks.metadata(identifier) }
        } catch (e: Exception) {
            error = e.message
        } finally {
            loading = false
        }
    }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = { Text("Book", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = colors.accentLavender)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background)
            )
        }
    ) { pad ->
        when {
            loading -> Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.accentLavender)
            }
            book == null -> Text(error ?: "Not found", Modifier.padding(pad).padding(16.dp), color = colors.textSecondary)
            else -> {
                val b = book!!
                val cleanDesc = remember(b.description) {
                    InternetArchiveBooks.stripToReadableText(b.description).ifBlank {
                        "No description available."
                    }
                }
                Column(
                    Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(b.coverUrl).crossfade(true).build(),
                        contentDescription = b.title,
                        modifier = Modifier.width(160.dp).height(220.dp).clip(RoundedCornerShape(12.dp)).background(colors.surfaceElevated),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.height(20.dp))
                    Text(b.title, style = SuhoodTypography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Text(b.creator, color = colors.textSecondary, modifier = Modifier.fillMaxWidth())
                    Text("${b.language} · ${b.year}", style = SuhoodTypography.labelSmall, color = colors.textMuted, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(14.dp))
                    Text(cleanDesc, color = colors.textSecondary, lineHeight = 20.sp, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Formats: ${b.formats.take(12).joinToString().ifBlank { "See Internet Archive" }}",
                        style = SuhoodTypography.labelSmall,
                        color = colors.textMuted,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = { onRead(b.identifier) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accentLavender, contentColor = colors.background)
                    ) {
                        Icon(Icons.Filled.MenuBook, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Read in Suhood", fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(b.itemUrl))) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.accentLavender)
                    ) {
                        Text("Open on Internet Archive")
                    }
                }
            }
        }
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
                            Box(Modifier.fillMaxSize().background(colors.background.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = colors.accentLavender)
                                    Spacer(Modifier.height(8.dp))
                                    Text("Loading book pages…", color = colors.textSecondary)
                                }
                            }
                        }
                    }
                }
                else -> {
                    if (textLoading) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = colors.accentLavender)
                        }
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

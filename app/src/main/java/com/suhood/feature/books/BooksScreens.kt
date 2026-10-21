package com.suhood.feature.books

import android.content.Intent
import android.net.Uri
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suhood.core.designsystem.components.GlassCard
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

    LaunchedEffect(Unit) { load("subject:(Islam OR Quran OR Seerah)") }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = { Text("Suhood Books", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = colors.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background)
            )
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Search title, author, subject…") },
                trailingIcon = {
                    IconButton(onClick = { load(query) }) {
                        Icon(Icons.Filled.Search, null)
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary
                )
            )
            Text(
                "Source: Internet Archive · free/public listings only. Restricted or lending items are not force-downloaded.",
                style = SuhoodTypography.labelSmall,
                color = colors.textMuted,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colors.accentLavender)
                }
                error != null -> Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(error ?: "", color = colors.textSecondary)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { load(query) }) { Text("Retry") }
                }
                books.isEmpty() -> Text("No books found for this query.", color = colors.textMuted)
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(books, key = { it.identifier }) { book ->
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onNavigate(Routes.BOOKS_DETAIL.replace("{identifier}", book.identifier)) },
                            contentPadding = PaddingValues(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier
                                        .size(56.dp, 72.dp)
                                        .background(colors.surfaceElevated, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.MenuBook, null, tint = colors.accentLavender)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        book.title,
                                        style = SuhoodTypography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = colors.textPrimary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(book.creator, style = SuhoodTypography.labelSmall, color = colors.textMuted, maxLines = 1)
                                    Text(book.language, style = SuhoodTypography.labelSmall, color = colors.textSecondary)
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(identifier: String, onBack: () -> Unit, onRead: (String) -> Unit) {
    val colors = suhoodColors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
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
                title = { Text("Book", color = colors.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = colors.textPrimary)
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
                Column(
                    Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp)
                ) {
                    Text(b.title, style = SuhoodTypography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary)
                    Spacer(Modifier.height(6.dp))
                    Text(b.creator, color = colors.textSecondary)
                    Text("${b.language} · ${b.year}", style = SuhoodTypography.labelSmall, color = colors.textMuted)
                    Spacer(Modifier.height(12.dp))
                    Text(b.description.ifBlank { "No description provided by the source." }, color = colors.textSecondary, lineHeight = 20.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("Formats: ${b.formats.take(8).joinToString().ifBlank { "See Internet Archive page" }}",
                        style = SuhoodTypography.labelSmall, color = colors.textMuted)
                    Spacer(Modifier.height(8.dp))
                    Text("Source: Internet Archive (archive.org). Suhood does not own this content.",
                        style = SuhoodTypography.labelSmall, color = colors.textMuted)
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            val textUrl = InternetArchiveBooks.preferredTextUrl(b.identifier, b.formats)
                            if (textUrl != null) onRead(b.identifier)
                            else context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(b.itemUrl)))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Read / Open") }
                    OutlinedButton(
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(b.itemUrl))) },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) { Text("Open on Internet Archive") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookReaderScreen(identifier: String, onBack: () -> Unit) {
    val colors = suhoodColors
    val context = LocalContext.current
    var text by remember { mutableStateOf("Loading text…") }
    var fontSp by remember { mutableFloatStateOf(18f) }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var speaking by remember { mutableStateOf(false) }

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
        text = withContext(Dispatchers.IO) {
            try {
                val url = "https://archive.org/stream/$identifier/${identifier}_djvu.txt"
                HttpGetPlain(url).take(80_000).ifBlank {
                    "Plain text is not available for this item. Open it on Internet Archive for other formats."
                }
            } catch (e: Exception) {
                "Could not load text layer: ${e.message}\n\nOpen the item on Internet Archive for PDF/EPUB if available."
            }
        }
    }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = { Text("Reader", color = colors.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = colors.textPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { fontSp = (fontSp - 2).coerceAtLeast(14f) }) {
                        Icon(Icons.Filled.TextDecrease, null, tint = colors.textPrimary)
                    }
                    IconButton(onClick = { fontSp = (fontSp + 2).coerceAtMost(28f) }) {
                        Icon(Icons.Filled.TextIncrease, null, tint = colors.textPrimary)
                    }
                    IconButton(onClick = {
                        val e = tts
                        if (e == null) return@IconButton
                        if (speaking) {
                            e.stop(); speaking = false
                        } else {
                            e.speak(text.take(3500), TextToSpeech.QUEUE_FLUSH, null, "suhood-book")
                            speaking = true
                        }
                    }) {
                        Icon(
                            if (speaking) Icons.Filled.Stop else Icons.Filled.RecordVoiceOver,
                            null,
                            tint = colors.accentLavender
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background)
            )
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text(
                "TTS uses your device Text-to-Speech engine. Voice depends on installed language packs.",
                style = SuhoodTypography.labelSmall,
                color = colors.textMuted
            )
            Spacer(Modifier.height(12.dp))
            Text(text, color = colors.textPrimary, fontSize = fontSp.sp, lineHeight = (fontSp * 1.45f).sp)
        }
    }
}

private fun HttpGetPlain(url: String): String {
    val conn = (java.net.URL(url).openConnection() as java.net.HttpURLConnection).apply {
        connectTimeout = 20000
        readTimeout = 25000
        setRequestProperty("User-Agent", "Suhood/1.0")
    }
    return try {
        if (conn.responseCode !in 200..299) return ""
        conn.inputStream.bufferedReader().use { it.readText() }
    } finally {
        conn.disconnect()
    }
}

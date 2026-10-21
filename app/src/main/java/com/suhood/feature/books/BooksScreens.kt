package com.suhood.feature.books

import android.content.Intent
import android.net.Uri
import android.speech.tts.TextToSpeech
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.suhood.core.designsystem.theme.SuhoodTypography
import com.suhood.core.designsystem.theme.suhoodColors
import com.suhood.core.navigation.Routes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

private val Gold = Color(0xFFC9A84C)
private val GoldDim = Color(0xFF8B6F2E)
private val CardBg = Color(0xFF141414)
private val ChipBg = Color(0xFF1A1A1A)

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
        containerColor = Color(0xFF0D0D0D),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Suhood Books", fontWeight = FontWeight.Bold, color = Color(0xFFF5F0E8))
                        Text("Islamic Knowledge Library", style = SuhoodTypography.labelSmall, color = Color(0xFF665A4A))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Gold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0D0D0D))
            )
        }
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
        ) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text("مكتبة سهود", color = Gold, fontSize = 13.sp)
                Text(
                    "Discover Islamic Books",
                    style = SuhoodTypography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFFF5F0E8)
                )
                Text(
                    "Free & public Islamic texts from the Internet Archive",
                    style = SuhoodTypography.bodySmall,
                    color = Color(0xFF665A4A)
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search books, authors, topics…", color = Color(0xFF665A4A)) },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.Search, null, tint = Gold) },
                    trailingIcon = {
                        IconButton(onClick = { load(query) }) {
                            Icon(Icons.Filled.ArrowForward, null, tint = Gold)
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Gold,
                        unfocusedBorderColor = Gold.copy(alpha = 0.25f),
                        focusedTextColor = Color(0xFFF5F0E8),
                        unfocusedTextColor = Color(0xFFF5F0E8),
                        cursorColor = Gold,
                        focusedContainerColor = ChipBg,
                        unfocusedContainerColor = ChipBg
                    )
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(ChipBg)
                        .border(0.5.dp, Gold.copy(alpha = 0.18f), RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Info, null, tint = Gold, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Source: Internet Archive · Free & public listings. Restricted items are not force-downloaded.",
                        style = SuhoodTypography.labelSmall,
                        color = Color(0xFF665A4A),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
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
                            selectedContainerColor = Gold.copy(alpha = 0.15f),
                            selectedLabelColor = Gold,
                            containerColor = ChipBg,
                            labelColor = Color(0xFFA89880)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = active,
                            borderColor = Gold.copy(alpha = 0.2f),
                            selectedBorderColor = Gold
                        )
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    if (loading) "Searching…" else "${books.size} books found",
                    style = SuhoodTypography.labelSmall,
                    color = if (loading) Gold else Color(0xFF665A4A)
                )
                Text("Sorted by popularity", style = SuhoodTypography.labelSmall, color = Color(0xFF665A4A))
            }
            Spacer(Modifier.height(8.dp))
            when {
                loading -> BooksLoadingShimmer()
                error != null -> Text(
                    error!!,
                    Modifier.padding(16.dp),
                    color = Color(0xFFE07A7A)
                )
                books.isEmpty() -> Text(
                    "No books found. Try another search.",
                    Modifier.padding(16.dp),
                    color = Color(0xFFA89880)
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(books, key = { _, b -> b.identifier }) { index, book ->
                        BookListCard(
                            book = book,
                            featured = index == 0,
                            onClick = { onNavigate(Routes.BOOKS_DETAIL.replace("{identifier}", android.net.Uri.encode(book.identifier))) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BooksLoadingShimmer() {
    val infinite = rememberInfiniteTransition(label = "shimmer")
    val alpha by infinite.animateFloat(
        0.35f, 0.85f,
        infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "a"
    )
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(5) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(88.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1A1A1A).copy(alpha = alpha))
            )
        }
    }
}

@Composable
private fun BookListCard(book: IaBook, featured: Boolean, onClick: () -> Unit) {
    val context = LocalContext.current
    Box {
        if (featured) {
            Text(
                "★ TOP PICK",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 12.dp)
                    .background(Gold, RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                color = Color(0xFF0D0D0D),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardBg)
                .border(0.5.dp, Gold.copy(alpha = 0.18f), RoundedCornerShape(16.dp))
                .clickable(onClick = onClick)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(book.coverUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = book.title,
                modifier = Modifier
                    .width(52.dp)
                    .height(68.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.linearGradient(listOf(GoldDim, Gold.copy(alpha = 0.5f)))
                    ),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    book.title,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = Color(0xFFF5F0E8)
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        book.creator,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 11.sp,
                        color = Color(0xFFA89880),
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        book.language.take(3).uppercase(),
                        modifier = Modifier
                            .background(Gold.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Gold
                    )
                }
            }
            Icon(Icons.Filled.ChevronRight, null, tint = Color(0xFF665A4A))
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
        containerColor = Color(0xFF0D0D0D),
        topBar = {
            TopAppBar(
                title = { Text("Book", color = Color(0xFFF5F0E8), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Gold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0D0D0D))
            )
        }
    ) { pad ->
        when {
            loading -> Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Gold)
            }
            book == null -> Text(
                error ?: "Not found",
                Modifier.padding(pad).padding(16.dp),
                color = Color(0xFFA89880)
            )
            else -> {
                val b = book!!
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(pad)
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(b.coverUrl).crossfade(true).build(),
                        contentDescription = b.title,
                        modifier = Modifier
                            .width(160.dp)
                            .height(220.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.linearGradient(listOf(GoldDim, Color(0xFF3A2E10)))),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.height(20.dp))
                    Text(
                        b.title,
                        style = SuhoodTypography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFF5F0E8),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(b.creator, color = Color(0xFFA89880), modifier = Modifier.fillMaxWidth())
                    Text(
                        "${b.language} · ${b.year}",
                        style = SuhoodTypography.labelSmall,
                        color = Color(0xFF665A4A),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        b.description.ifBlank { "No description provided by the source." },
                        color = Color(0xFFA89880),
                        lineHeight = 20.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Formats: ${b.formats.take(10).joinToString().ifBlank { "See Internet Archive" }}",
                        style = SuhoodTypography.labelSmall,
                        color = Color(0xFF665A4A),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Source: Internet Archive. Suhood does not own this content.",
                        style = SuhoodTypography.labelSmall,
                        color = Color(0xFF665A4A),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = { onRead(b.identifier) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color(0xFF0D0D0D))
                    ) {
                        Icon(Icons.Filled.MenuBook, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Read in Suhood", fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(b.itemUrl)))
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Gold),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.linearGradient(listOf(Gold.copy(0.5f), Gold))
                        )
                    ) {
                        Text("Open on Internet Archive")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookReaderScreen(identifier: String, onBack: () -> Unit) {
    val context = LocalContext.current
    var text by remember { mutableStateOf("") }
    var sourceLabel by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var fontSp by remember { mutableFloatStateOf(18f) }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var speaking by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("Reader") }

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
        loading = true
        withContext(Dispatchers.IO) {
            val meta = runCatching { InternetArchiveBooks.metadata(identifier) }.getOrNull()
            title = meta?.title ?: "Reader"
            val result = runCatching { InternetArchiveBooks.loadReadableText(identifier) }
                .getOrElse {
                    IaTextResult("Could not load text: ${it.message}", "error")
                }
            text = result.plainText
            sourceLabel = result.sourceLabel
        }
        loading = false
    }

    // TTS text: never HTML
    val ttsText = remember(text) { InternetArchiveBooks.stripToReadableText(text).ifBlank { text } }

    Scaffold(
        containerColor = Color(0xFF0D0D0D),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color(0xFFF5F0E8),
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Gold)
                    }
                },
                actions = {
                    IconButton(onClick = { fontSp = (fontSp - 2).coerceAtLeast(14f) }) {
                        Icon(Icons.Filled.TextDecrease, null, tint = Color(0xFFF5F0E8))
                    }
                    IconButton(onClick = { fontSp = (fontSp + 2).coerceAtMost(30f) }) {
                        Icon(Icons.Filled.TextIncrease, null, tint = Color(0xFFF5F0E8))
                    }
                    IconButton(onClick = {
                        val e = tts ?: return@IconButton
                        if (speaking) {
                            e.stop(); speaking = false
                        } else {
                            // Chunk long text for TTS
                            val chunk = ttsText.take(3900)
                            if (chunk.isNotBlank()) {
                                e.speak(chunk, TextToSpeech.QUEUE_FLUSH, null, "suhood-book")
                                speaking = true
                            }
                        }
                    }) {
                        Icon(
                            if (speaking) Icons.Filled.Stop else Icons.Filled.RecordVoiceOver,
                            null,
                            tint = Gold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0D0D0D))
            )
        }
    ) { pad ->
        when {
            loading -> Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Gold)
                    Spacer(Modifier.height(12.dp))
                    Text("Loading book text…", color = Color(0xFFA89880))
                }
            }
            else -> Column(
                Modifier
                    .fillMaxSize()
                    .padding(pad)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Text(
                    "TTS reads cleaned book text only (HTML stripped). Engine: device TTS · Source: $sourceLabel",
                    style = SuhoodTypography.labelSmall,
                    color = Color(0xFF665A4A)
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    ttsText,
                    color = Color(0xFFF5F0E8),
                    fontSize = fontSp.sp,
                    lineHeight = (fontSp * 1.5f).sp
                )
            }
        }
    }
}

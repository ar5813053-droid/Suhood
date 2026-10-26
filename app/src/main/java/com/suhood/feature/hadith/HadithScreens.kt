package com.suhood.feature.hadith

import android.content.Context
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suhood.core.designsystem.theme.suhoodColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Offline Hadith library with professional book-style paging.
 * Data from open hadith-json datasets (Arabic + English). Not a redistribution of third-party PDFs.
 */
object HadithScreens {

    data class BookMeta(
        val id: String,
        val asset: String,
        val title: String,
        val arabicTitle: String,
        val subtitle: String
    )

    data class HadithPage(
        val index: Int,
        val ref: String,
        val arabic: String,
        val english: String,
        val grade: String = ""
    )

    val catalog = listOf(
        BookMeta("nawawi", "hadith/nawawi40.json", "40 Hadith Nawawi", "الأربعون النووية", "Complete Imam al-Nawawi collection"),
        BookMeta("qudsi", "hadith/qudsi40.json", "40 Hadith Qudsi", "الأربعون القدسية", "Complete Forty Hadith Qudsi"),
        BookMeta("bukhari", "hadith/bukhari.json", "Sahih al-Bukhari", "صحيح البخاري", "Opening narrations (offline selection)"),
        BookMeta("muslim", "hadith/muslim.json", "Sahih Muslim", "صحيح مسلم", "Opening narrations (offline selection)"),
        BookMeta("riyad", "hadith/riyad_assalihin.json", "Riyad as-Salihin", "رياض الصالحين", "Gardens of the Righteous"),
        BookMeta("abudawud", "hadith/abudawud.json", "Sunan Abu Dawud", "سنن أبي داود", "Opening narrations (offline selection)"),
        BookMeta("tirmidhi", "hadith/tirmidhi.json", "Jami' at-Tirmidhi", "جامع الترمذي", "Opening narrations (offline selection)"),
        BookMeta("nasai", "hadith/nasai.json", "Sunan an-Nasa'i", "سنن النسائي", "Opening narrations (offline selection)"),
        BookMeta("ibnmajah", "hadith/ibnmajah.json", "Sunan Ibn Majah", "سنن ابن ماجه", "Opening narrations (offline selection)"),
        BookMeta("malik", "hadith/malik.json", "Muwatta Malik", "موطأ مالك", "Opening narrations (offline selection)")
    )

    fun loadBook(context: Context, assetPath: String): List<HadithPage> {
        return try {
            val raw = context.assets.open(assetPath).bufferedReader().use { it.readText() }
            val root = JSONObject(raw)
            val arr = root.optJSONArray("hadiths") ?: return emptyList()
            val meta = root.optJSONObject("metadata")
            val bookTitle = meta?.optJSONObject("english")?.optString("title")
                ?: meta?.optString("english") ?: ""
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val idInBook = o.optInt("idInBook", i + 1)
                    val arabic = o.optString("arabic").trim()
                    val engObj = o.optJSONObject("english")
                    val english = when {
                        engObj != null -> {
                            val narrator = engObj.optString("narrator").trim()
                            val text = engObj.optString("text").trim()
                            if (narrator.isNotBlank()) "$narrator\n\n$text" else text
                        }
                        else -> o.optString("english").trim()
                    }
                    if (arabic.isBlank() && english.isBlank()) continue
                    add(
                        HadithPage(
                            index = i,
                            ref = "$bookTitle · $idInBook".trim(' ', '·'),
                            arabic = arabic,
                            english = english,
                            grade = o.optString("grade")
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    @Composable
    fun Home(onBack: () -> Unit, onBook: (String) -> Unit) {
        val colors = suhoodColors
        Column(
            Modifier
                .fillMaxSize()
                .background(colors.background)
                .statusBarsPadding()
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(colors.surfaceElevated)
                        .border(1.dp, colors.borderSubtle, CircleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Hadith", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    Text("Offline library · Arabic & English", color = colors.textSecondary, fontSize = 13.sp)
                }
            }
            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        "Swipe through narrations like pages of a book. Collections are offline open datasets — not scanned PDFs.",
                        color = colors.textMuted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
                items(catalog, key = { it.id }) { b ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(colors.surfaceElevated)
                            .border(1.dp, colors.borderSubtle, RoundedCornerShape(18.dp))
                            .clickable { onBook(b.id) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF8B7CF8).copy(alpha = 0.25f), Color(0xFF5DE0A0).copy(alpha = 0.12f))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.AutoMirrored.Filled.MenuBook, null, tint = Color(0xFF8B7CF8))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(b.title, color = colors.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            Text(b.arabicTitle, color = colors.accentLavender, fontSize = 14.sp)
                            Text(b.subtitle, color = colors.textSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                        }
                        Icon(Icons.Filled.ChevronRight, null, tint = colors.textMuted)
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }

    @Composable
    fun Book(bookId: String, onBack: () -> Unit) {
        val colors = suhoodColors
        val context = LocalContext.current
        val meta = catalog.find { it.id == bookId }
        var pages by remember { mutableStateOf<List<HadithPage>?>(null) }
        var error by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()

        LaunchedEffect(bookId) {
            pages = null
            error = null
            val loaded = withContext(Dispatchers.IO) {
                meta?.let { loadBook(context, it.asset) }.orEmpty()
            }
            if (loaded.isEmpty()) error = "Could not load this book offline."
            pages = loaded
        }

        val list = pages
        Column(
            Modifier
                .fillMaxSize()
                .background(colors.background)
                .statusBarsPadding()
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(colors.surfaceElevated)
                        .border(1.dp, colors.borderSubtle, CircleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(meta?.title ?: "Hadith", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(meta?.arabicTitle.orEmpty(), color = colors.textSecondary, fontSize = 12.sp)
                }
            }

            when {
                list == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colors.accentLavender)
                }
                error != null -> Text(error ?: "", color = colors.textSecondary, modifier = Modifier.padding(24.dp))
                list.isEmpty() -> Text("No narrations found.", color = colors.textSecondary, modifier = Modifier.padding(24.dp))
                else -> {
                    val pagerState = rememberPagerState(pageCount = { list.size })
                    Box(Modifier.weight(1f)) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                            beyondViewportPageCount = 1,
                            key = { list[it].index }
                        ) { page ->
                            HadithBookPage(
                                page = list[page],
                                pageNumber = page + 1,
                                total = list.size,
                                bookTitle = meta?.title.orEmpty()
                            )
                        }
                        // Bottom chrome overlay
                        Row(
                            Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, colors.background.copy(alpha = 0.92f), colors.background)
                                    )
                                )
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(colors.surfaceElevated)
                                    .border(1.dp, colors.borderSubtle, CircleShape)
                                    .clickable(enabled = pagerState.currentPage > 0) {
                                        scope.launch {
                                            pagerState.animateScrollToPage(
                                                pagerState.currentPage - 1,
                                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                            )
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.ChevronLeft, "Previous", tint = colors.textPrimary)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "${pagerState.currentPage + 1} / ${list.size}",
                                    color = colors.textPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Text("Swipe left for next · right for previous", color = colors.textMuted, fontSize = 11.sp)
                            }
                            Box(
                                Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(colors.surfaceElevated)
                                    .border(1.dp, colors.borderSubtle, CircleShape)
                                    .clickable(enabled = pagerState.currentPage < list.lastIndex) {
                                        scope.launch {
                                            pagerState.animateScrollToPage(
                                                pagerState.currentPage + 1,
                                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                            )
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.ChevronRight, "Next", tint = colors.textPrimary)
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun HadithBookPage(
        page: HadithPage,
        pageNumber: Int,
        total: Int,
        bookTitle: String
    ) {
        val colors = suhoodColors
        val pageBg = Color(0xFF16181C)
        val ink = Color(0xFFF2F0EA)
        val muted = Color(0xFFA8A49A)
        Box(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .padding(bottom = 72.dp)
        ) {
            // Book page surface
            Column(
                Modifier
                    .fillMaxSize()
                    .shadow(12.dp, RoundedCornerShape(18.dp))
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(pageBg, Color(0xFF12141A))
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
                    .padding(horizontal = 20.dp, vertical = 22.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Ornamental top
                Text(
                    bookTitle.uppercase(),
                    color = Color(0xFF8B7CF8).copy(alpha = 0.85f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(48.dp)
                        .height(2.dp)
                        .background(Color(0xFF8B7CF8).copy(alpha = 0.45f), RoundedCornerShape(2.dp))
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    page.ref.ifBlank { "Hadith $pageNumber" },
                    color = muted,
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(18.dp))
                if (page.arabic.isNotBlank()) {
                    Text(
                        page.arabic,
                        color = ink,
                        fontSize = 22.sp,
                        lineHeight = 40.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        style = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.Rtl)
                    )
                    Spacer(Modifier.height(18.dp))
                }
                if (page.english.isNotBlank()) {
                    Text(
                        page.english,
                        color = muted,
                        fontSize = 15.sp,
                        lineHeight = 24.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (page.grade.isNotBlank()) {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        page.grade,
                        color = Color(0xFF5DE0A0).copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    "— $pageNumber —",
                    color = muted.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
            // Soft side edge (book spine feel)
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .width(6.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Black.copy(alpha = 0.25f), Color.Transparent)
                        )
                    )
            )
        }
    }
}

package com.suhood.feature.worship

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import com.suhood.app.SuhoodApplication
import com.suhood.core.database.TasbeehStateEntity
import androidx.compose.ui.unit.dp
import com.suhood.core.data.DemoData
import com.suhood.core.designsystem.components.*
import com.suhood.core.designsystem.theme.SuhoodSpacing
import com.suhood.core.designsystem.theme.SuhoodTypography
import com.suhood.core.designsystem.theme.suhoodColors
import com.suhood.core.model.QuizDifficulty
import com.suhood.core.navigation.Routes
import com.suhood.core.dua.DuaSearch

object WorshipScreens {

    @Composable
    fun Dashboard(onNavigate: (String) -> Unit, bottomContentPadding: Dp) {
        val colors = suhoodColors
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(bottom = bottomContentPadding)
                .padding(horizontal = SuhoodSpacing.screenHorizontal)
                .padding(bottom = 32.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            // Hero glass card
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(22.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.accentLavender.copy(alpha = 0.18f))
                            .border(1.dp, colors.accentLavender.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.SelfImprovement,
                            contentDescription = null,
                            tint = colors.accentLavender,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "عبادة  Worship",
                            style = SuhoodTypography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = colors.textPrimary
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "Dhikr, adhkar, duas, and learning — in one quiet place.",
                    style = SuhoodTypography.bodyMedium,
                    color = colors.textSecondary
                )
            }

            Spacer(Modifier.height(22.dp))
            Text(
                "Remembrance",
                style = SuhoodTypography.labelSmall.copy(
                    letterSpacing = 0.8.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = colors.textMuted
            )
            Spacer(Modifier.height(10.dp))
            WorshipRow(
                title = "Tasbeeh",
                subtitle = "Digital prayer beads",
                icon = Icons.Filled.SelfImprovement,
                accent = colors.accentLavender,
                onClick = { onNavigate(Routes.TASBEEH) }
            )
            WorshipRow(
                title = "Daily Adhkar",
                subtitle = "Morning & evening remembrance",
                icon = Icons.Filled.WbSunny,
                accent = colors.silver,
                onClick = { onNavigate(Routes.ADHKAR) }
            )
            WorshipRow(
                title = "Find Duas",
                subtitle = "Search supplications by topic",
                icon = Icons.Filled.Search,
                accent = colors.accentCoral,
                onClick = { onNavigate(Routes.DUA_LIBRARY) }
            )

            Spacer(Modifier.height(22.dp))
            Text(
                "Learn & track",
                style = SuhoodTypography.labelSmall.copy(
                    letterSpacing = 0.8.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = colors.textMuted
            )
            Spacer(Modifier.height(10.dp))
            WorshipRow(
                title = "Islamic quiz",
                subtitle = "Test your knowledge",
                icon = Icons.Filled.Lightbulb,
                accent = colors.accentLavender,
                onClick = { onNavigate(Routes.QUIZ) }
            )
            WorshipRow(
                title = "Daily checklist",
                subtitle = "Track today's worship",
                icon = Icons.AutoMirrored.Filled.List,
                accent = colors.silverBright,
                onClick = { onNavigate(Routes.WORSHIP_CHECKLIST) }
            )
            WorshipRow(
                title = "Quran",
                subtitle = "Read & listen",
                icon = Icons.Filled.Book,
                accent = colors.accentLavender,
                onClick = { onNavigate(Routes.QURAN) }
            )
        }
    }

    @Composable
    private fun WorshipRow(
        title: String,
        subtitle: String,
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        accent: androidx.compose.ui.graphics.Color,
        onClick: () -> Unit
    ) {
        val colors = suhoodColors
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            onClick = onClick,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(accent.copy(alpha = 0.16f))
                        .border(1.dp, accent.copy(alpha = 0.28f), RoundedCornerShape(13.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = accent, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        title,
                        style = SuhoodTypography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )
                    Text(subtitle, style = SuhoodTypography.bodySmall, color = colors.textMuted)
                }
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = colors.textMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }

    @Composable
    fun Tasbeeh(onBack: () -> Unit) {
        val colors = suhoodColors
        val context = LocalContext.current
        val dao = remember { (context.applicationContext as SuhoodApplication).database.dao() }
        val scope = rememberCoroutineScope()
        val state by dao.observeTasbeeh().collectAsState(initial = null)
        var count by remember { mutableIntStateOf(0) }
        var target by remember { mutableIntStateOf(33) }
        var dhikrIndex by remember { mutableIntStateOf(0) }
        var loop by remember { mutableIntStateOf(1) }
        var beadPulse by remember { mutableStateOf(false) }
        LaunchedEffect(state) {
            state?.let {
                count = it.count
                target = it.goal
                dhikrIndex = it.dhikrIndex
            }
        }
        fun persist(c: Int = count, g: Int = target, di: Int = dhikrIndex) {
            scope.launch {
                dao.upsertTasbeeh(TasbeehStateEntity(id = 1, count = c, goal = g, dhikrIndex = di))
            }
        }
        fun bump() {
            if (count + 1 >= target) {
                count = 0
                loop++
                persist(0)
            } else {
                count++
                persist(count)
            }
            beadPulse = !beadPulse
        }
        fun undoCount() {
            if (count > 0) {
                count--
                persist(count)
            }
        }
        val dhikr = DemoData.dhikrItems[dhikrIndex % DemoData.dhikrItems.size]
        val scale by animateFloatAsState(if (beadPulse) 1.04f else 1f, label = "countScale")

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .background(colors.background)
        ) {
            ScreenHeader(title = "Tasbeeh", onBack = onBack)

            // Full interactive area — tap to count, swipe left to undo
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(count, target) {
                        detectTapGestures(onTap = { bump() })
                    }
                    .pointerInput(count) {
                        var totalDrag = 0f
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (totalDrag < -80f) undoCount()
                                totalDrag = 0f
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                totalDrag += dragAmount
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Loop $loop",
                        style = SuhoodTypography.labelLarge,
                        color = colors.textMuted
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "$count",
                        style = SuhoodTypography.timer.copy(fontSize = 64.sp),
                        color = colors.success,
                        modifier = Modifier.graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                    )
                    Text(
                        "/ $target",
                        style = SuhoodTypography.titleMedium,
                        color = colors.textMuted
                    )
                    Spacer(Modifier.height(28.dp))

                    // Bead string (visual like Muslim Pro)
                    Canvas(Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 12.dp)) {
                        val n = minOf(target, 18)
                        val lit = ((count.toFloat() / target.coerceAtLeast(1)) * n).toInt().coerceIn(0, n)
                        val y = size.height / 2
                        val step = size.width / (n + 1)
                        // string
                        drawLine(
                            color = colors.borderStrong,
                            start = Offset(step, y),
                            end = Offset(size.width - step, y),
                            strokeWidth = 3f
                        )
                        for (i in 0 until n) {
                            val x = step * (i + 1)
                            val r = if (i < lit) 14.dp.toPx() else 11.dp.toPx()
                            val col = if (i < lit) colors.accentLavender else colors.surfaceElevated
                            drawCircle(color = colors.borderSubtle, radius = r + 2f, center = Offset(x, y))
                            drawCircle(color = col, radius = r, center = Offset(x, y))
                            if (i < lit) {
                                drawCircle(
                                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.25f),
                                    radius = r * 0.35f,
                                    center = Offset(x - r * 0.25f, y - r * 0.25f)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                    Text(
                        "Tap anywhere to count",
                        style = SuhoodTypography.bodyMedium,
                        color = colors.textSecondary
                    )
                    Text(
                        "(swipe left to decrease)",
                        style = SuhoodTypography.bodySmall,
                        color = colors.textMuted
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        dhikr.arabic,
                        style = SuhoodTypography.arabicTitle.copy(fontSize = 22.sp),
                        color = colors.accentLavender
                    )
                    Text(dhikr.transliteration, style = SuhoodTypography.bodySmall, color = colors.textMuted)
                }
            }

            // Bottom goal + next
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(33, 34, 99, 200).forEach { g ->
                        GlassPill(selected = target == g, onClick = {
                            target = g
                            if (count > g) count = g
                            persist(count, g)
                        }) {
                            Text("$g", style = SuhoodTypography.labelLarge, color = colors.textPrimary)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                GlassButton(
                    text = "Next dhikr",
                    onClick = {
                        dhikrIndex = (dhikrIndex + 1) % DemoData.dhikrItems.size
                        count = 0
                        loop = 1
                        target = DemoData.dhikrItems[dhikrIndex].defaultTarget
                        persist(0, target, dhikrIndex)
                    },
                    primary = false,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    @Composable
    fun Adhkar(onBack: () -> Unit, onMorning: () -> Unit, onEvening: () -> Unit) {
        val colors = suhoodColors
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            ScreenHeader(title = "Adhkar", onBack = onBack)
            Column(modifier = Modifier.padding(horizontal = SuhoodSpacing.screenHorizontal)) {
                GlassCard(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), onClick = onMorning) {
                    Text("Morning Adhkar", style = SuhoodTypography.titleMedium, color = colors.textPrimary)
                    Text("After Fajr", style = SuhoodTypography.bodySmall, color = colors.textMuted)
                }
                GlassCard(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), onClick = onEvening) {
                    Text("Evening Adhkar", style = SuhoodTypography.titleMedium, color = colors.textPrimary)
                    Text("After Asr / Maghrib", style = SuhoodTypography.bodySmall, color = colors.textMuted)
                }
            }
        }
    }

    @Composable
    fun AdhkarDetail(title: String, onBack: () -> Unit) {
        val colors = suhoodColors
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            ScreenHeader(title = title, onBack = onBack)
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = SuhoodSpacing.screenHorizontal)
                    .padding(bottom = 24.dp)
            ) {
                DemoData.dhikrItems.take(4).forEach { item ->
                    GlassCard(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Text(item.arabic, style = SuhoodTypography.titleLarge, color = colors.textPrimary)
                        Spacer(Modifier.height(4.dp))
                        Text(item.transliteration, style = SuhoodTypography.bodyMedium, color = colors.textSecondary)
                        Text(item.translation, style = SuhoodTypography.bodySmall, color = colors.textMuted)
                    }
                }
            }
        }
    }

    @Composable
    fun DuaLibrary(onBack: () -> Unit, onDua: (String) -> Unit) {
        val colors = suhoodColors
        var query by remember { mutableStateOf("") }
        val results = remember(query) {
            DuaSearch.search(query, DemoData.duaItems)
        }
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            ScreenHeader(title = "Find Duas", onBack = onBack)
            GlassSearchField(
                query = query,
                onQueryChange = { query = it },
                placeholder = "Search duas by name or purpose in any language...",
                modifier = Modifier.padding(horizontal = SuhoodSpacing.screenHorizontal)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Offline · verified everyday adhkar",
                style = SuhoodTypography.labelSmall,
                color = colors.textMuted,
                modifier = Modifier.padding(horizontal = SuhoodSpacing.screenHorizontal)
            )
            Spacer(Modifier.height(8.dp))
            if (results.isEmpty()) {
                Text(
                    "No dua matched. Try English or Roman Urdu keywords.",
                    color = colors.textMuted,
                    modifier = Modifier.padding(24.dp)
                )
            } else {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = SuhoodSpacing.screenHorizontal)
                        .padding(bottom = 32.dp)
                ) {
                    results.forEach { dua ->
                        GlassCard(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            onClick = { onDua(dua.id) }
                        ) {
                            Text(dua.title, style = SuhoodTypography.titleMedium, color = colors.textPrimary)
                            Text(dua.category, style = SuhoodTypography.bodySmall, color = colors.textMuted)
                            if (dua.reference.isNotBlank()) {
                                Text(dua.reference, style = SuhoodTypography.labelSmall, color = colors.accentLavender)
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun DuaDetail(duaId: String, onBack: () -> Unit) {
        val colors = suhoodColors
        val dua = DemoData.duaItems.find { it.id == duaId } ?: DemoData.duaItems.first()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            ScreenHeader(title = dua.title, onBack = onBack)
            Column(modifier = Modifier.padding(SuhoodSpacing.screenHorizontal).padding(bottom = 32.dp)) {
                Text(dua.category, style = SuhoodTypography.labelMedium, color = colors.accentLavender)
                Spacer(Modifier.height(12.dp))
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        dua.arabic,
                        style = SuhoodTypography.arabicBody,
                        color = colors.textPrimary
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("Transliteration", style = SuhoodTypography.labelSmall, color = colors.textMuted)
                    Spacer(Modifier.height(4.dp))
                    Text(dua.transliteration, style = SuhoodTypography.bodyMedium, color = colors.textSecondary)
                    Spacer(Modifier.height(12.dp))
                    Text("Meaning", style = SuhoodTypography.labelSmall, color = colors.textMuted)
                    Spacer(Modifier.height(4.dp))
                    Text(dua.translation, style = SuhoodTypography.bodyLarge, color = colors.textPrimary)
                    if (dua.reference.isNotBlank()) {
                        Spacer(Modifier.height(16.dp))
                        Text("Source", style = SuhoodTypography.labelSmall, color = colors.textMuted)
                        Spacer(Modifier.height(4.dp))
                        Text(dua.reference, style = SuhoodTypography.bodySmall, color = colors.accentLavender)
                    }
                }
            }
        }
    }

    @Composable
    fun QuizHome(onBack: () -> Unit, onStart: () -> Unit) {
        val colors = suhoodColors
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().padding(SuhoodSpacing.screenHorizontal)
        ) {
            ScreenHeader(title = "Islamic quiz", onBack = onBack)
            Spacer(Modifier.height(24.dp))
            Text(
                "A small offline quiz with respectful, checked questions. No fabricated citations.",
                style = SuhoodTypography.bodyMedium,
                color = colors.textSecondary
            )
            Spacer(Modifier.height(24.dp))
            GlassButton(text = "Start quiz", onClick = onStart, modifier = Modifier.fillMaxWidth())
        }
    }

    @Composable
    fun QuizDifficulty(onBack: () -> Unit, onSelect: (QuizDifficulty) -> Unit) {
        val colors = suhoodColors
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = SuhoodSpacing.screenHorizontal)) {
            ScreenHeader(title = "Difficulty", onBack = onBack)
            Spacer(Modifier.height(16.dp))
            QuizDifficulty.entries.forEach { diff ->
                GlassCard(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    onClick = { onSelect(diff) }
                ) {
                    Text(diff.name.lowercase().replaceFirstChar { it.uppercase() }, style = SuhoodTypography.titleMedium, color = colors.textPrimary)
                }
            }
        }
    }

    @Composable
    fun QuizPlay(onFinish: (com.suhood.core.model.QuizResult) -> Unit) {
        val colors = suhoodColors
        val questions = DemoData.quizQuestions
        var index by remember { mutableIntStateOf(0) }
        var correct by remember { mutableIntStateOf(0) }
        var selected by remember { mutableStateOf<Int?>(null) }
        var answered by remember { mutableStateOf(false) }
        val q = questions[index]

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(SuhoodSpacing.screenHorizontal)
        ) {
            Text("Question ${index + 1} of ${questions.size}", style = SuhoodTypography.labelMedium, color = colors.textMuted)
            Spacer(Modifier.height(12.dp))
            Text(q.question, style = SuhoodTypography.titleLarge, color = colors.textPrimary)
            Spacer(Modifier.height(20.dp))
            q.options.forEachIndexed { i, opt ->
                val isCorrect = i == q.correctIndex
                val showResult = answered
                GlassCard(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    selected = selected == i,
                    onClick = {
                        if (!answered) {
                            selected = i
                            answered = true
                            if (isCorrect) correct++
                        }
                    }
                ) {
                    Text(
                        opt,
                        style = SuhoodTypography.bodyMedium,
                        color = when {
                            showResult && isCorrect -> colors.success
                            showResult && selected == i && !isCorrect -> colors.error
                            else -> colors.textPrimary
                        }
                    )
                }
            }
            if (answered) {
                Spacer(Modifier.height(12.dp))
                Text(q.explanation, style = SuhoodTypography.bodySmall, color = colors.textSecondary)
                Spacer(Modifier.height(20.dp))
                GlassButton(
                    text = if (index < questions.size - 1) "Next" else "See results",
                    onClick = {
                        if (index < questions.size - 1) {
                            index++
                            selected = null
                            answered = false
                        } else {
                            onFinish(
                                com.suhood.core.model.QuizResult(
                                    totalQuestions = questions.size,
                                    correctAnswers = correct,
                                    answers = emptyList()
                                )
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    @Composable
    fun QuizResults(onRetry: () -> Unit, onDone: () -> Unit) {
        val colors = suhoodColors
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().padding(SuhoodSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Quiz complete", style = SuhoodTypography.headlineLarge, color = colors.textPrimary)
            Spacer(Modifier.height(12.dp))
            Text("Review and keep learning.", style = SuhoodTypography.bodyMedium, color = colors.textSecondary)
            Spacer(Modifier.height(32.dp))
            GlassButton(text = "Try again", onClick = onRetry, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            GlassButton(text = "Done", onClick = onDone, primary = false, modifier = Modifier.fillMaxWidth())
        }
    }

    @Composable
    fun Checklist(onBack: () -> Unit) {
        val colors = suhoodColors
        var items by remember {
            mutableStateOf(
                listOf(
                    "Fajr" to false,
                    "Dhuhr" to false,
                    "Asr" to false,
                    "Maghrib" to false,
                    "Isha" to false,
                    "Morning Adhkar" to false,
                    "Evening Adhkar" to false
                )
            )
        }
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            ScreenHeader(title = "Daily checklist", onBack = onBack)
            Column(modifier = Modifier.padding(horizontal = SuhoodSpacing.screenHorizontal)) {
                items.forEachIndexed { i, (name, done) ->
                    GlassCard(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        selected = done,
                        onClick = {
                            items = items.mapIndexed { j, pair ->
                                if (j == i) pair.first to !pair.second else pair
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(name, style = SuhoodTypography.titleSmall, color = colors.textPrimary)
                            Text(if (done) "Done" else "—", style = SuhoodTypography.labelMedium, color = if (done) colors.success else colors.textMuted)
                        }
                    }
                }
            }
        }
    }
}

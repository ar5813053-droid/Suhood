package com.suhood.feature.worship

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.suhood.core.data.DemoData
import com.suhood.core.designsystem.components.*
import com.suhood.core.designsystem.theme.SuhoodSpacing
import com.suhood.core.designsystem.theme.SuhoodTypography
import com.suhood.core.designsystem.theme.suhoodColors
import com.suhood.core.model.QuizDifficulty
import com.suhood.core.navigation.Routes

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
            Text("عبادة · Worship", style = SuhoodTypography.headlineLarge, color = colors.textPrimary)
            Spacer(Modifier.height(4.dp))
            Text("Dhikr, Adhkar, Duas, and learning", style = SuhoodTypography.bodyMedium, color = colors.textSecondary)
            Spacer(Modifier.height(20.dp))

            listOf(
                "Tasbeeh" to Routes.TASBEEH,
                "Daily Adhkar" to Routes.ADHKAR,
                "Dua library" to Routes.DUA_LIBRARY,
                "Islamic quiz" to Routes.QUIZ,
                "Daily checklist" to Routes.WORSHIP_CHECKLIST,
                "Quran" to Routes.QURAN
            ).forEach { (label, route) ->
                GlassCard(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    onClick = { onNavigate(route) }
                ) {
                    Text(label, style = SuhoodTypography.titleMedium, color = colors.textPrimary)
                }
            }
        }
    }

    @Composable
    fun Tasbeeh(onBack: () -> Unit) {
        val colors = suhoodColors
        var count by remember { mutableIntStateOf(0) }
        var target by remember { mutableIntStateOf(33) }
        var dhikrIndex by remember { mutableIntStateOf(0) }
        val dhikr = DemoData.dhikrItems[dhikrIndex]

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = SuhoodSpacing.screenHorizontal),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ScreenHeader(title = "Tasbeeh", onBack = onBack)
            Spacer(Modifier.height(24.dp))
            Text(dhikr.arabic, style = SuhoodTypography.headlineLarge, color = colors.textPrimary)
            Text(dhikr.transliteration, style = SuhoodTypography.bodyMedium, color = colors.textSecondary)
            Text(dhikr.translation, style = SuhoodTypography.bodySmall, color = colors.textMuted)
            Spacer(Modifier.height(32.dp))
            Text("$count", style = SuhoodTypography.timer, color = colors.silverBright)
            Text("of $target", style = SuhoodTypography.bodyMedium, color = colors.textSecondary)
            Spacer(Modifier.height(32.dp))
            GlassButton(
                text = "Count",
                onClick = { if (count < target) count++ },
                modifier = Modifier.fillMaxWidth().height(64.dp)
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GlassButton(text = "Undo", onClick = { if (count > 0) count-- }, primary = false)
                GlassButton(text = "Reset", onClick = { count = 0 }, primary = false)
            }
            Spacer(Modifier.height(20.dp))
            Text("Target", style = SuhoodTypography.labelMedium, color = colors.textMuted)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(33, 34, 100).forEach { t ->
                    GlassPill(selected = target == t, onClick = { target = t; if (count > t) count = t }) {
                        Text("$t", style = SuhoodTypography.labelLarge, color = colors.textPrimary)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            GlassButton(
                text = "Next dhikr",
                onClick = {
                    dhikrIndex = (dhikrIndex + 1) % DemoData.dhikrItems.size
                    count = 0
                    target = DemoData.dhikrItems[dhikrIndex].defaultTarget
                },
                primary = false,
                modifier = Modifier.fillMaxWidth()
            )
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
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            ScreenHeader(title = "Dua library", onBack = onBack)
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = SuhoodSpacing.screenHorizontal)
            ) {
                DemoData.duaItems.forEach { dua ->
                    GlassCard(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        onClick = { onDua(dua.id) }
                    ) {
                        Text(dua.title, style = SuhoodTypography.titleMedium, color = colors.textPrimary)
                        Text(dua.category, style = SuhoodTypography.bodySmall, color = colors.textMuted)
                    }
                }
            }
        }
    }

    @Composable
    fun DuaDetail(duaId: String, onBack: () -> Unit) {
        val colors = suhoodColors
        val dua = DemoData.duaItems.find { it.id == duaId } ?: DemoData.duaItems.first()
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            ScreenHeader(title = dua.title, onBack = onBack)
            Column(modifier = Modifier.padding(SuhoodSpacing.screenHorizontal)) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(dua.arabic, style = SuhoodTypography.headlineSmall, color = colors.textPrimary)
                    Spacer(Modifier.height(12.dp))
                    Text(dua.transliteration, style = SuhoodTypography.bodyMedium, color = colors.textSecondary)
                    Spacer(Modifier.height(8.dp))
                    Text(dua.translation, style = SuhoodTypography.bodyMedium, color = colors.textPrimary)
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

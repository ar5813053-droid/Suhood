package com.suhood.feature.alarms

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.suhood.core.data.DemoData
import com.suhood.core.designsystem.components.*
import com.suhood.core.designsystem.theme.SuhoodSpacing
import com.suhood.core.designsystem.theme.SuhoodTypography
import com.suhood.core.designsystem.theme.suhoodColors
import com.suhood.core.model.AlarmConfig
import com.suhood.core.model.ChallengeType

object AlarmScreens {

    @Composable
    fun Dashboard(
        onBack: () -> Unit,
        onCreate: () -> Unit,
        onEdit: (String) -> Unit,
        onSimulateRing: (String) -> Unit
    ) {
        val colors = suhoodColors
        var alarms by remember { mutableStateOf(DemoData.sampleAlarms) }

        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            ScreenHeader(
                title = "Alarms",
                onBack = onBack,
                actions = {
                    GlassButton(text = "Add", onClick = onCreate)
                }
            )
            Text(
                "Demo alarms only. OS scheduling arrives in Phase 2.",
                style = SuhoodTypography.bodySmall,
                color = colors.textMuted,
                modifier = Modifier.padding(horizontal = SuhoodSpacing.screenHorizontal)
            )
            Spacer(Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = SuhoodSpacing.screenHorizontal)
                    .padding(bottom = 24.dp)
            ) {
                alarms.forEach { alarm ->
                    GlassCard(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        onClick = { onEdit(alarm.id) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "%02d:%02d".format(alarm.hour, alarm.minute),
                                    style = SuhoodTypography.headlineSmall,
                                    color = colors.textPrimary
                                )
                                Text(alarm.label, style = SuhoodTypography.bodySmall, color = colors.textSecondary)
                                Text(
                                    "Challenge: ${alarm.challengeType.name}",
                                    style = SuhoodTypography.labelSmall,
                                    color = colors.textMuted
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                GlassPill(
                                    selected = alarm.enabled,
                                    onClick = {
                                        alarms = alarms.map {
                                            if (it.id == alarm.id) it.copy(enabled = !it.enabled) else it
                                        }
                                    }
                                ) {
                                    Text(
                                        if (alarm.enabled) "On" else "Off",
                                        style = SuhoodTypography.labelMedium,
                                        color = colors.textPrimary
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                GlassButton(
                                    text = "Test ring",
                                    onClick = { onSimulateRing(alarm.id) },
                                    primary = false
                                )
                            }
                        }
                    }
                }
                if (alarms.isEmpty()) {
                    EmptyState("No alarms", "Create a demo alarm to explore the flow.")
                }
            }
        }
    }

    @Composable
    fun CreateEdit(alarmId: String?, onBack: () -> Unit, onSaved: () -> Unit) {
        val colors = suhoodColors
        var hour by remember { mutableIntStateOf(5) }
        var minute by remember { mutableIntStateOf(0) }
        var label by remember { mutableStateOf("Fajr") }
        var challenge by remember { mutableStateOf(ChallengeType.QUIZ) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = SuhoodSpacing.screenHorizontal)
        ) {
            ScreenHeader(title = if (alarmId == null) "New alarm" else "Edit alarm", onBack = onBack)
            Spacer(Modifier.height(16.dp))
            Text("Time (demo)", style = SuhoodTypography.titleSmall, color = colors.textMuted)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(4, 5, 6).forEach { h ->
                    GlassPill(selected = hour == h, onClick = { hour = h }) {
                        Text("%02d".format(h), style = SuhoodTypography.titleMedium, color = colors.textPrimary)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(0, 15, 30, 45).forEach { m ->
                    GlassPill(selected = minute == m, onClick = { minute = m }) {
                        Text("%02d".format(m), style = SuhoodTypography.titleMedium, color = colors.textPrimary)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Text("Challenge", style = SuhoodTypography.titleSmall, color = colors.textMuted)
            Spacer(Modifier.height(8.dp))
            GlassSegmentedControl(
                options = listOf("None", "Quiz", "Dhikr"),
                selectedIndex = when (challenge) {
                    ChallengeType.NONE -> 0
                    ChallengeType.QUIZ -> 1
                    ChallengeType.DHIKR -> 2
                },
                onSelect = {
                    challenge = when (it) {
                        1 -> ChallengeType.QUIZ
                        2 -> ChallengeType.DHIKR
                        else -> ChallengeType.NONE
                    }
                }
            )
            Spacer(Modifier.height(32.dp))
            GlassButton(text = "Save", onClick = onSaved, modifier = Modifier.fillMaxWidth())
        }
    }

    @Composable
    fun Ringing(onQuiz: () -> Unit, onDhikr: () -> Unit, onDismiss: () -> Unit) {
        val colors = suhoodColors
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().padding(SuhoodSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            DemoStatusLabel("Simulated alarm")
            Spacer(Modifier.height(16.dp))
            Text("05:00", style = SuhoodTypography.timer, color = colors.silverBright)
            Text("Fajr (Demo)", style = SuhoodTypography.titleMedium, color = colors.textPrimary)
            Spacer(Modifier.height(40.dp))
            GlassButton(text = "Dismiss with Quiz", onClick = onQuiz, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            GlassButton(text = "Dismiss with Dhikr", onClick = onDhikr, modifier = Modifier.fillMaxWidth(), primary = false)
            Spacer(Modifier.height(8.dp))
            GlassButton(text = "Dismiss", onClick = onDismiss, primary = false, modifier = Modifier.fillMaxWidth())
        }
    }

    @Composable
    fun QuizChallenge(onComplete: () -> Unit) {
        val colors = suhoodColors
        val q = DemoData.quizQuestions.first()
        var selected by remember { mutableStateOf<Int?>(null) }
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().padding(SuhoodSpacing.xl)
        ) {
            Text("Answer to dismiss", style = SuhoodTypography.headlineSmall, color = colors.textPrimary)
            Spacer(Modifier.height(16.dp))
            Text(q.question, style = SuhoodTypography.bodyLarge, color = colors.textPrimary)
            Spacer(Modifier.height(20.dp))
            q.options.forEachIndexed { i, opt ->
                GlassCard(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    selected = selected == i,
                    onClick = { selected = i }
                ) {
                    Text(opt, style = SuhoodTypography.bodyMedium, color = colors.textPrimary)
                }
            }
            Spacer(Modifier.height(24.dp))
            GlassButton(
                text = "Submit",
                onClick = { if (selected == q.correctIndex) onComplete() },
                enabled = selected != null,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    @Composable
    fun DhikrChallenge(onComplete: () -> Unit) {
        val colors = suhoodColors
        var count by remember { mutableIntStateOf(0) }
        val target = 33
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().padding(SuhoodSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("سُبْحَانَ اللَّهِ", style = SuhoodTypography.headlineLarge, color = colors.textPrimary)
            Text("SubhanAllah", style = SuhoodTypography.bodyMedium, color = colors.textSecondary)
            Spacer(Modifier.height(24.dp))
            Text("$count / $target", style = SuhoodTypography.timerSmall, color = colors.silverBright)
            Spacer(Modifier.height(24.dp))
            GlassButton(
                text = "Tap",
                onClick = {
                    count++
                    if (count >= target) onComplete()
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    @Composable
    fun Dismissed(onDone: () -> Unit) {
        val colors = suhoodColors
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().padding(SuhoodSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Alarm dismissed", style = SuhoodTypography.headlineLarge, color = colors.textPrimary)
            Spacer(Modifier.height(12.dp))
            Text("Barakallahu feek. Start your day with intention.", style = SuhoodTypography.bodyMedium, color = colors.textSecondary)
            Spacer(Modifier.height(32.dp))
            GlassButton(text = "Done", onClick = onDone, modifier = Modifier.fillMaxWidth())
        }
    }
}

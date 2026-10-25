package com.suhood.feature.focus

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suhood.core.designsystem.theme.SuhoodTheme
import com.suhood.core.designsystem.theme.ThemeMode
import com.suhood.core.designsystem.theme.suhoodColors
import kotlin.random.Random

/**
 * Prayer-Pause style gate: complete a short Islamic mini-game to continue
 * (max 3 successful unlocks per day via [FocusChallengeStore]).
 */
class FocusChallengeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        val pkg = intent.getStringExtra(EXTRA_PACKAGE).orEmpty()
        val remaining = FocusChallengeStore.get(this).challengesRemaining()

        setContent {
            SuhoodTheme(themeMode = ThemeMode.DARK) {
                BackHandler {
                    goHome()
                }
                FocusChallengeScreen(
                    challengesLeft = remaining,
                    onCompleted = {
                        FocusChallengeStore.get(this).recordChallengeCompleted()
                        finish()
                    },
                    onExit = { goHome() }
                )
            }
        }
    }

    private fun goHome() {
        runCatching {
            startActivity(
                Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
        }
        finish()
    }

    companion object {
        const val EXTRA_PACKAGE = "package"
    }
}

private enum class ChallengeType { TASBEEH, QUIZ, DHIKR, AYAH }

@Composable
private fun FocusChallengeScreen(
    challengesLeft: Int,
    onCompleted: () -> Unit,
    onExit: () -> Unit
) {
    val colors = suhoodColors
    val type = remember {
        listOf(ChallengeType.TASBEEH, ChallengeType.QUIZ, ChallengeType.DHIKR, ChallengeType.AYAH)
            .random()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF08090B))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Pause · Return to Allah",
                color = Color(0xFFC7CBD1),
                fontSize = 14.sp,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Complete this moment to continue",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Text(
                "$challengesLeft of 3 challenges left today",
                color = Color(0xFF9AA3AE),
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
            Spacer(Modifier.height(28.dp))

            when (type) {
                ChallengeType.TASBEEH -> TasbeehChallenge(onCompleted)
                ChallengeType.QUIZ -> QuizChallenge(onCompleted)
                ChallengeType.DHIKR -> DhikrChallenge(onCompleted)
                ChallengeType.AYAH -> AyahChallenge(onCompleted)
            }

            Spacer(Modifier.weight(1f))
            Text(
                "I Understand — Exit App",
                color = Color(0xFF9FE8C8),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(28.dp))
                    .border(1.dp, Color(0x669FE8C8), RoundedCornerShape(28.dp))
                    .clickable(onClick = onExit)
                    .padding(horizontal = 24.dp, vertical = 14.dp)
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun TasbeehChallenge(onCompleted: () -> Unit) {
    var count by remember { mutableIntStateOf(0) }
    val target = 33
    ChallengeCard {
        Text("Tasbeeh", color = Color(0xFFC7CBD1), fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        Text("سُبْحَانَ اللَّهِ", color = Color.White, fontSize = 28.sp, textAlign = TextAlign.Center)
        Text("SubhanAllah", color = Color(0xFF9AA3AE), fontSize = 14.sp)
        Spacer(Modifier.height(16.dp))
        Text("$count / $target", color = Color(0xFF9FE8C8), fontSize = 36.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        PrimaryChip(if (count >= target) "Continue" else "Tap") {
            if (count >= target) onCompleted()
            else count++
        }
    }
}

@Composable
private fun QuizChallenge(onCompleted: () -> Unit) {
    data class Q(val q: String, val options: List<String>, val answer: Int)
    val quiz = remember {
        listOf(
            Q("How many daily prayers are obligatory?", listOf("3", "4", "5", "7"), 2),
            Q("What is the first month of the Islamic calendar?", listOf("Ramadan", "Muharram", "Shawwal", "Rajab"), 1),
            Q("Qibla is the direction of:", listOf("Madinah", "Jerusalem", "Makkah", "Cairo"), 2)
        ).random()
    }
    var selected by remember { mutableIntStateOf(-1) }
    var wrong by remember { mutableStateOf(false) }
    ChallengeCard {
        Text("Quick quiz", color = Color(0xFFC7CBD1), fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        Text(quiz.q, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(16.dp))
        quiz.options.forEachIndexed { i, opt ->
            val bg = when {
                selected == i && i == quiz.answer -> Color(0x3322C55E)
                selected == i && i != quiz.answer -> Color(0x33EF4444)
                else -> Color(0x22FFFFFF)
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bg)
                    .clickable {
                        selected = i
                        wrong = i != quiz.answer
                        if (i == quiz.answer) onCompleted()
                    }
                    .padding(14.dp)
            ) {
                Text(opt, color = Color.White, fontSize = 16.sp)
            }
        }
        if (wrong) {
            Text("Try again", color = Color(0xFFEF4444), fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun DhikrChallenge(onCompleted: () -> Unit) {
    val options = listOf("Alhamdulillah", "SubhanAllah", "Allahu Akbar", "Astaghfirullah")
    val target = remember { options.random() }
    ChallengeCard {
        Text("Dhikr", color = Color(0xFFC7CBD1), fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        Text("Select: $target", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(16.dp))
        options.shuffled().forEach { opt ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x22FFFFFF))
                    .clickable { if (opt == target) onCompleted() }
                    .padding(14.dp)
            ) {
                Text(opt, color = Color.White, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun AyahChallenge(onCompleted: () -> Unit) {
    data class A(val ar: String, val en: String)
    val ayah = remember {
        listOf(
            A("بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ", "In the name of Allah, the Most Gracious, the Most Merciful."),
            A("إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ", "You alone we worship, and You alone we ask for help."),
            A("قُلْ هُوَ اللَّهُ أَحَدٌ", "Say: He is Allah, the One.")
        ).random()
    }
    ChallengeCard {
        Text("Quran · reflect", color = Color(0xFFC7CBD1), fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))
        Text(ayah.ar, color = Color.White, fontSize = 24.sp, textAlign = TextAlign.Center, lineHeight = 36.sp)
        Spacer(Modifier.height(12.dp))
        Text(ayah.en, color = Color(0xFFB8BEC6), fontSize = 15.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        PrimaryChip("I reflected — Continue", onCompleted)
    }
}

@Composable
private fun ChallengeCard(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0x331A2228))
            .border(1.dp, Color(0x44FFFFFF), RoundedCornerShape(22.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) { content() }
}

@Composable
private fun PrimaryChip(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xFF1A2E2A))
            .border(1.dp, Color(0x669FE8C8), RoundedCornerShape(28.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color(0xFF9FE8C8), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    }
}

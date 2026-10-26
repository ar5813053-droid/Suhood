package com.suhood.feature.hadith

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suhood.core.designsystem.components.ScreenHeader
import com.suhood.core.designsystem.theme.SuhoodSpacing
import com.suhood.core.designsystem.theme.suhoodColors
import kotlinx.coroutines.delay

/**
 * Offline Hadith library — curated selections for reading inside Suhood.
 */
object HadithScreens {

    data class Book(val id: String, val title: String, val subtitle: String)
    data class Entry(val ref: String, val arabic: String, val english: String)

    private val books = listOf(
        Book("bukhari", "Sahih al-Bukhari", "Selected narrations on faith, knowledge & Qur'an"),
        Book("muslim", "Sahih Muslim", "Selected narrations on purification & character"),
        Book("nawawi", "40 Hadith Nawawi", "Foundational guidance for daily life"),
        Book("riyad", "Riyad as-Salihin", "Selected chapters on manners & worship"),
        Book("tirmidhi", "Jami' at-Tirmidhi", "Selected narrations on manners & dua")
    )

    private val content: Map<String, List<Entry>> = mapOf(
        "bukhari" to listOf(
            Entry("Bukhari — Revelation", "إِنَّمَا الأَعْمَالُ بِالنِّيَّاتِ", "Actions are according to intentions, and every person will get the reward according to what they intended."),
            Entry("Bukhari — Faith", "الْمُسْلِمُ مَنْ سَلِمَ الْمُسْلِمُونَ مِنْ لِسَانِهِ وَيَدِهِ", "The Muslim is the one from whose tongue and hand the Muslims are safe."),
            Entry("Bukhari — Knowledge", "مَنْ سَلَكَ طَرِيقًا يَلْتَمِسُ فِيهِ عِلْمًا سَهَّلَ اللَّهُ لَهُ طَرِيقًا إِلَى الْجَنَّةِ", "Whoever travels a path seeking knowledge, Allah makes easy for them a path to Paradise."),
            Entry("Bukhari — Qur'an", "خَيْرُكُمْ مَنْ تَعَلَّمَ الْقُرْآنَ وَعَلَّمَهُ", "The best among you are those who learn the Qur'an and teach it."),
            Entry("Bukhari — Prayer", "صَلُّوا كَمَا رَأَيْتُمُونِي أُصَلِّي", "Pray as you have seen me pray."),
            Entry("Bukhari — Mercy", "لَا يَرْحَمُ اللَّهُ مَنْ لَا يَرْحَمُ النَّاسَ", "Allah does not show mercy to those who do not show mercy to people.")
        ),
        "muslim" to listOf(
            Entry("Muslim — Purification", "الطُّهُورُ شَطْرُ الْإِيمَانِ", "Purity is half of faith."),
            Entry("Muslim — Mercy", "مَنْ لَا يَرْحَمْ لَا يُرْحَمْ", "He who does not show mercy will not be shown mercy."),
            Entry("Muslim — Brotherhood", "لَا يُؤْمِنُ أَحَدُكُمْ حَتَّى يُحِبَّ لِأَخِيهِ مَا يُحِبُّ لِنَفْسِهِ", "None of you truly believes until he loves for his brother what he loves for himself."),
            Entry("Muslim — Speech", "مَنْ كَانَ يُؤْمِنُ بِاللَّهِ وَالْيَوْمِ الْآخِرِ فَلْيَقُلْ خَيْرًا أَوْ لِيَصْمُتْ", "Whoever believes in Allah and the Last Day should speak good or remain silent."),
            Entry("Muslim — Smile", "تَبَسُّمُكَ فِي وَجْهِ أَخِيكَ لَكَ صَدَقَةٌ", "Your smile in the face of your brother is charity.")
        ),
        "nawawi" to listOf(
            Entry("Nawawi 1", "إِنَّمَا الأَعْمَالُ بِالنِّيَّاتِ", "Actions are but by intention."),
            Entry("Nawawi 5", "مَنْ أَحْدَثَ فِي أَمْرِنَا هَذَا مَا لَيْسَ مِنْهُ فَهُوَ رَدٌّ", "Whoever introduces into this matter of ours that which is not from it, it is rejected."),
            Entry("Nawawi 13", "لَا يُؤْمِنُ أَحَدُكُمْ حَتَّى يُحِبَّ لِأَخِيهِ مَا يُحِبُّ لِنَفْسِهِ", "None of you truly believes until he loves for his brother what he loves for himself."),
            Entry("Nawawi 18", "اتَّقِ اللَّهَ حَيْثُمَا كُنْتَ", "Fear Allah wherever you are."),
            Entry("Nawawi 19", "احْفَظِ اللَّهَ يَحْفَظْكَ", "Be mindful of Allah, and He will protect you."),
            Entry("Nawawi 40", "كُنْ فِي الدُّنْيَا كَأَنَّكَ غَرِيبٌ", "Be in this world as though you were a stranger.")
        ),
        "riyad" to listOf(
            Entry("Riyad — Sincerity", "إِنَّ اللَّهَ لَا يَنْظُرُ إِلَى أَجْسَامِكُمْ وَلَا إِلَى صُوَرِكُمْ وَلَكِنْ يَنْظُرُ إِلَى قُلُوبِكُمْ", "Allah does not look at your bodies or forms, but He looks at your hearts."),
            Entry("Riyad — Patience", "وَالصَّبْرُ ضِيَاءٌ", "And patience is light."),
            Entry("Riyad — Gratitude", "مَنْ لَا يَشْكُرِ النَّاسَ لَا يَشْكُرِ اللَّهَ", "Whoever does not thank people has not thanked Allah."),
            Entry("Riyad — Anger", "لَيْسَ الشَّدِيدُ بِالصُّرَعَةِ إِنَّمَا الشَّدِيدُ الَّذِي يَمْلِكُ نَفْسَهُ عِنْدَ الْغَضَبِ", "The strong is not the one who overcomes people by force, but the one who controls himself while angry.")
        ),
        "tirmidhi" to listOf(
            Entry("Tirmidhi — Character", "أَكْمَلُ الْمُؤْمِنِينَ إِيمَانًا أَحْسَنُهُمْ خُلُقًا", "The most complete of the believers in faith are those with the best character."),
            Entry("Tirmidhi — Dua", "الدُّعَاءُ هُوَ الْعِبَادَةُ", "Supplication is the essence of worship."),
            Entry("Tirmidhi — Manners", "مَنْ لَا يَرْحَمْ لَا يُرْحَمْ", "He who does not show mercy will not be shown mercy."),
            Entry("Tirmidhi — Quiet", "مَنْ صَمَتَ نَجَا", "Whoever is silent is saved.")
        )
    )

    @Composable
    fun Home(onBack: () -> Unit, onBook: (String) -> Unit) {
        val colors = suhoodColors
        var show by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { show = true }
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            ScreenHeader(title = "Hadith", onBack = onBack)
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = SuhoodSpacing.screenHorizontal)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Read curated authentic narrations offline — Arabic and English.",
                    color = colors.textSecondary,
                    fontSize = 14.sp
                )
                books.forEachIndexed { index, b ->
                    AnimatedVisibility(
                        visible = show,
                        enter = fadeIn() + slideInVertically { it / 6 }
                    ) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .background(colors.surfaceElevated, RoundedCornerShape(16.dp))
                                .border(1.dp, colors.borderSubtle, RoundedCornerShape(16.dp))
                                .clickable { onBook(b.id) }
                                .padding(16.dp)
                        ) {
                            Text(b.title, color = colors.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(b.subtitle, color = colors.textSecondary, fontSize = 13.sp)
                            Text(
                                "${content[b.id].orEmpty().size} selections",
                                color = colors.accentLavender,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                    LaunchedEffect(show) {
                        if (show) delay(40L * index)
                    }
                }
            }
        }
    }

    @Composable
    fun Book(bookId: String, onBack: () -> Unit) {
        val colors = suhoodColors
        val book = books.find { it.id == bookId }
        val entries = content[bookId].orEmpty()
        var show by remember { mutableStateOf(false) }
        LaunchedEffect(bookId) { show = true }
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            ScreenHeader(title = book?.title ?: "Hadith", onBack = onBack)
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = SuhoodSpacing.screenHorizontal)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                entries.forEachIndexed { i, e ->
                    AnimatedVisibility(
                        visible = show,
                        enter = fadeIn() + slideInVertically { it / 8 }
                    ) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .background(colors.surfaceElevated, RoundedCornerShape(16.dp))
                                .border(1.dp, colors.borderSubtle, RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            Text(e.ref, color = colors.accentLavender, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(10.dp))
                            Text(e.arabic, color = colors.textPrimary, fontSize = 20.sp, lineHeight = 34.sp)
                            Spacer(Modifier.height(10.dp))
                            Text(e.english, color = colors.textSecondary, fontSize = 14.sp, lineHeight = 22.sp)
                        }
                    }
                    LaunchedEffect(show, i) {
                        if (show) delay(50L * i)
                    }
                }
            }
        }
    }
}

package com.suhood.feature.more

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.suhood.core.designsystem.components.GlassCard
import com.suhood.core.designsystem.theme.SuhoodTypography
import com.suhood.core.designsystem.theme.suhoodColors
import com.suhood.core.navigation.Routes

private data class FeatureItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val route: String
)

private data class FeatureSection(val title: String, val items: List<FeatureItem>)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllFeaturesScreen(onNavigate: (String) -> Unit, onBack: () -> Unit) {
    val colors = suhoodColors
    val sections = listOf(
        FeatureSection(
            "Worship",
            listOf(
                FeatureItem("Salah", "Prayer times & tracker", Icons.Filled.Place, Routes.SALAH),
                FeatureItem("Quran", "Mushaf & reading", Icons.Filled.MenuBook, Routes.QURAN),
                FeatureItem("Quran Companion", "Daily reading goals", Icons.Filled.MenuBook, Routes.QURAN),
                FeatureItem("Tasbeeh", "Dhikr counter", Icons.Filled.SelfImprovement, Routes.TASBEEH),
                FeatureItem("Duas", "Find & read duas", Icons.Filled.Favorite, Routes.DUA_LIBRARY),
                FeatureItem("Qibla", "Direction to Makkah", Icons.Filled.Explore, Routes.QIBLA),
                FeatureItem("Worship Quest", "XP & monthly missions", Icons.Filled.Star, Routes.QUEST)
            )
        ),
        FeatureSection(
            "Explore",
            listOf(
                FeatureItem("Suhood Books", "Free Islamic library", Icons.Filled.LocalLibrary, Routes.BOOKS),
                FeatureItem("Find Mosque", "Nearby mosques", Icons.Filled.Place, Routes.MOSQUE)
            )
        ),
        FeatureSection(
            "Focus & Growth",
            listOf(
                FeatureItem("Focus Mode", "Block distractions", Icons.Filled.Timelapse, Routes.FOCUS),
                FeatureItem("My Journey", "Habits", Icons.Filled.Flag, Routes.JOURNEY),
                FeatureItem("Muhasabah", "Daily journal", Icons.Filled.EditNote, Routes.MUHASABAH)
            )
        ),
        FeatureSection(
            "Audio",
            listOf(
                FeatureItem("Nasheedify", "Nasheeds & recitation", Icons.Filled.MusicNote, Routes.AUDIO)
            )
        ),
        FeatureSection(
            "Phone",
            listOf(
                FeatureItem("Suhood Phone", "Dialer · contacts · call themes", Icons.Filled.Phone, Routes.PHONE),
                FeatureItem("Call Appearance", "Studio · themes · flags", Icons.Filled.Palette, Routes.PHONE_APPEARANCE)
            )
        ),
        FeatureSection(
            "Home screen",
            listOf(
                FeatureItem("Widgets", "Glass widgets · pin to home", Icons.Filled.Widgets, Routes.WIDGETS)
            )
        ),
        FeatureSection(
            "Personalisation",
            listOf(
                FeatureItem("Alarms", "Adhan & reminders", Icons.Filled.Alarm, Routes.ALARMS),
                FeatureItem("Settings", "Preferences", Icons.Filled.Settings, Routes.SETTINGS)
            )
        )
    )

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "All Features",
                        style = SuhoodTypography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = colors.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background)
            )
        }
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            sections.forEach { section ->
                Text(
                    section.title,
                    style = SuhoodTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary,
                    modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                )
                section.items.chunked(2).forEach { row ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        row.forEach { item ->
                            GlassCard(
                                modifier = Modifier.weight(1f),
                                onClick = { onNavigate(item.route) },
                                contentPadding = PaddingValues(14.dp)
                            ) {
                                Column {
                                    Icon(item.icon, null, tint = colors.accentLavender, modifier = Modifier.size(26.dp))
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        item.title,
                                        style = SuhoodTypography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = colors.textPrimary
                                    )
                                    Text(
                                        item.subtitle,
                                        style = SuhoodTypography.labelSmall,
                                        color = colors.textMuted
                                    )
                                }
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

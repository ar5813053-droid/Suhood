package com.suhood.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

enum class ThemeMode { Dark, Light, System }

data class SuhoodColorTokens(
    val background: Color = Color(0xFF0B0C0F),
    val surfaceElevated: Color = Color(0xFF1A1C22),
    val textPrimary: Color = Color(0xFFF2F4F8),
    val textSecondary: Color = Color(0xFFB0B6C3),
    val textMuted: Color = Color(0xFF7E8494),
    val borderSubtle: Color = Color(0x33FFFFFF),
    val accentLavender: Color = Color(0xFFC7B8FF),
    val accentMint: Color = Color(0xFF9FE8C8),
    val silverBright: Color = Color(0xFFE8ECF4)
)

val LocalSuhoodColors = staticCompositionLocalOf { SuhoodColorTokens() }
val suhoodColors: SuhoodColorTokens @Composable get() = LocalSuhoodColors.current

object SuhoodTypography {
    val titleLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold)
    val titleMedium = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    val titleSmall = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    val bodyMedium = TextStyle(fontSize = 14.sp)
    val bodySmall = TextStyle(fontSize = 12.sp)
    val labelLarge = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    val labelSmall = TextStyle(fontSize = 11.sp)
}

@Composable
fun SuhoodTheme(themeMode: ThemeMode = ThemeMode.Dark, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalSuhoodColors provides SuhoodColorTokens()) {
        MaterialTheme(
            colorScheme = darkColorScheme(
                background = Color(0xFF0B0C0F),
                surface = Color(0xFF1A1C22),
                primary = Color(0xFFC7B8FF)
            ),
            typography = Typography(),
            content = content
        )
    }
}

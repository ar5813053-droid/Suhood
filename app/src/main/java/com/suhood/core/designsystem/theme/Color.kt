package com.suhood.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * Suhood final identity: lavender × charcoal (no green branding).
 */

object SuhoodDarkColors {
    val background = Color(0xFF121212)
    val backgroundElevated = Color(0xFF1C1C1E)
    val surface = Color(0xFF1E1E1E)
    val surfaceElevated = Color(0xFF2C2C2E)
    val surfaceGlassBase = Color(0x14FFFFFF)
    val surfaceSelected = Color(0xFF3A3A3C)
    val borderSubtle = Color(0xFF3A3A3C)
    val borderStrong = Color(0xFF48484A)
    val textPrimary = Color(0xFFF6F4FB)
    val textSecondary = Color(0xFFAAA5B8)
    val textMuted = Color(0xFF7E798C)
    val silver = Color(0xFFC7CBD1)
    val silverBright = Color(0xFFE4E6EA)
    val disabled = Color(0xFF55525F)
    val error = Color(0xFFFF8A80)
    val success = Color(0xFFB9A7FF) // soft lavender for positive (not green)
    val accentLavender = Color(0xFFB9A7FF)
    val accentMint = Color(0xFFD8CFFF) // soft lavender (mint slot reused for soft lavender)
    val accentCoral = Color(0xFFFF8A80)
    val accentYellow = Color(0xFFFFD66B)
    val strongAccent = Color(0xFF9A82F5)
}

object SuhoodLightColors {
    val background = Color(0xFFF5F3FA)
    val backgroundElevated = Color(0xFFFFFFFF)
    val surface = Color(0xFFFFFFFF)
    val surfaceElevated = Color(0xFFF8F6FC)
    val surfaceGlassBase = Color(0xB8FFFFFF)
    val surfaceSelected = Color(0xFFEDE8FA)
    val borderSubtle = Color(0xFFE2DFEA)
    val borderStrong = Color(0xFFCBC5D8)
    val textPrimary = Color(0xFF211D2C)
    val textSecondary = Color(0xFF5C5768)
    val textMuted = Color(0xFF7A7588)
    val silver = Color(0xFF6A6578)
    val silverBright = Color(0xFF211D2C)
    val disabled = Color(0xFFA8A3B3)
    val error = Color(0xFFE57373)
    val success = Color(0xFF8066D9)
    val accentLavender = Color(0xFF8066D9)
    val accentMint = Color(0xFFB9A7FF)
    val accentCoral = Color(0xFFE57373)
    val accentYellow = Color(0xFFC9A227)
    val strongAccent = Color(0xFF8066D9)
}

data class SuhoodColorScheme(
    val background: Color,
    val backgroundElevated: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceGlass: Color,
    val surfaceSelected: Color,
    val borderSubtle: Color,
    val borderStrong: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val silver: Color,
    val silverBright: Color,
    val disabled: Color,
    val error: Color,
    val success: Color,
    val accentLavender: Color,
    val accentMint: Color,
    val accentCoral: Color,
    val accentYellow: Color,
    val isDark: Boolean
)

val DarkColorScheme = SuhoodColorScheme(
    background = SuhoodDarkColors.background,
    backgroundElevated = SuhoodDarkColors.backgroundElevated,
    surface = SuhoodDarkColors.surface,
    surfaceElevated = SuhoodDarkColors.surfaceElevated,
    surfaceGlass = SuhoodDarkColors.surfaceGlassBase,
    surfaceSelected = SuhoodDarkColors.surfaceSelected,
    borderSubtle = SuhoodDarkColors.borderSubtle,
    borderStrong = SuhoodDarkColors.borderStrong,
    textPrimary = SuhoodDarkColors.textPrimary,
    textSecondary = SuhoodDarkColors.textSecondary,
    textMuted = SuhoodDarkColors.textMuted,
    silver = SuhoodDarkColors.silver,
    silverBright = SuhoodDarkColors.silverBright,
    disabled = SuhoodDarkColors.disabled,
    error = SuhoodDarkColors.error,
    success = SuhoodDarkColors.success,
    accentLavender = SuhoodDarkColors.accentLavender,
    accentMint = SuhoodDarkColors.accentMint,
    accentCoral = SuhoodDarkColors.accentCoral,
    accentYellow = SuhoodDarkColors.accentYellow,
    isDark = true
)

val LightColorScheme = SuhoodColorScheme(
    background = SuhoodLightColors.background,
    backgroundElevated = SuhoodLightColors.backgroundElevated,
    surface = SuhoodLightColors.surface,
    surfaceElevated = SuhoodLightColors.surfaceElevated,
    surfaceGlass = SuhoodLightColors.surfaceGlassBase,
    surfaceSelected = SuhoodLightColors.surfaceSelected,
    borderSubtle = SuhoodLightColors.borderSubtle,
    borderStrong = SuhoodLightColors.borderStrong,
    textPrimary = SuhoodLightColors.textPrimary,
    textSecondary = SuhoodLightColors.textSecondary,
    textMuted = SuhoodLightColors.textMuted,
    silver = SuhoodLightColors.silver,
    silverBright = SuhoodLightColors.silverBright,
    disabled = SuhoodLightColors.disabled,
    error = SuhoodLightColors.error,
    success = SuhoodLightColors.success,
    accentLavender = SuhoodLightColors.accentLavender,
    accentMint = SuhoodLightColors.accentMint,
    accentCoral = SuhoodLightColors.accentCoral,
    accentYellow = SuhoodLightColors.accentYellow,
    isDark = false
)

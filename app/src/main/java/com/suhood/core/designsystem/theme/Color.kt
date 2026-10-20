package com.suhood.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * Suhood Gen-Z × Premium Islamic Wellness palette.
 * Dark is default. Accents are semantic — one dominant per screen.
 */

// ── Dark Theme ──────────────────────────────────────────────────────────────
object SuhoodDarkColors {
    val background = Color(0xFF08090B)
    val backgroundElevated = Color(0xFF111316)
    val surface = Color(0xFF17191F)
    val surfaceElevated = Color(0xFF1C1F26)
    val surfaceGlassBase = Color(0x14FFFFFF) // ~8% white
    val surfaceSelected = Color(0xFF2A2D36)
    val borderSubtle = Color(0xFF30333B)
    val borderStrong = Color(0xFF3E424C)
    val textPrimary = Color(0xFFF5F6F7)
    val textSecondary = Color(0xFFA5AAB4)
    val textMuted = Color(0xFF7A808C)
    val silver = Color(0xFFC7CBD1)
    val silverBright = Color(0xFFE4E6EA)
    val disabled = Color(0xFF555A63)
    val error = Color(0xFFFF8A80) // coral for warnings
    val success = Color(0xFFA7E8C1) // mint
    // Gen-Z accents
    val accentLavender = Color(0xFFB8A5FF)
    val accentMint = Color(0xFFA7E8C1)
    val accentCoral = Color(0xFFFF8A80)
    val accentYellow = Color(0xFFFFD66B)
}

// ── Light Theme ─────────────────────────────────────────────────────────────
object SuhoodLightColors {
    val background = Color(0xFFF3F4F6)
    val backgroundElevated = Color(0xFFFFFFFF)
    val surface = Color(0xFFFFFFFF)
    val surfaceElevated = Color(0xFFF8F9FB)
    val surfaceGlassBase = Color(0xB8FFFFFF)
    val surfaceSelected = Color(0xFFE8E4F7) // soft lavender tint
    val borderSubtle = Color(0xFFE2E4EA)
    val borderStrong = Color(0xFFC8CCD5)
    val textPrimary = Color(0xFF15171B)
    val textSecondary = Color(0xFF5C6370)
    val textMuted = Color(0xFF7A828F)
    val silver = Color(0xFF606773)
    val silverBright = Color(0xFF343941)
    val disabled = Color(0xFFA7ABB3)
    val error = Color(0xFFE57373)
    val success = Color(0xFF2E7D32)
    val accentLavender = Color(0xFF7C6BC4)
    val accentMint = Color(0xFF2E9B6A)
    val accentCoral = Color(0xFFE57373)
    val accentYellow = Color(0xFFC9A227)
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

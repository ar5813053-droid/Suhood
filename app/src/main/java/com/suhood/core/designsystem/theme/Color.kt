package com.suhood.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * Hybrid Glass design tokens — monochrome black / graphite / silver palette.
 * Apple-inspired Liquid Glass aesthetics without proprietary iOS frameworks.
 */

// ── Dark Theme ──────────────────────────────────────────────────────────────
object SuhoodDarkColors {
    val background = Color(0xFF08090B)
    val backgroundElevated = Color(0xFF111316)
    val surface = Color(0xFF141519)
    val surfaceElevated = Color(0xFF1A1C20)
    val surfaceGlassBase = Color(0x14FFFFFF) // ~8% white
    val surfaceSelected = Color(0xFF35383E)
    val borderSubtle = Color(0x1FFFFFFF) // ~12% white
    val borderStrong = Color(0x2EFFFFFF) // ~18% white
    val textPrimary = Color(0xFFF5F6F7)
    val textSecondary = Color(0xFFA4A8B0)
    val textMuted = Color(0xFF858B94)
    val silver = Color(0xFFC7CBD1)
    val silverBright = Color(0xFFE4E6EA)
    val disabled = Color(0xFF555A63)
    val error = Color(0xFFCF6679)
    val success = Color(0xFF81C784)
}

// ── Light Theme ─────────────────────────────────────────────────────────────
object SuhoodLightColors {
    val background = Color(0xFFF1F2F4)
    val backgroundElevated = Color(0xFFFFFFFF)
    val surface = Color(0xFFFFFFFF)
    val surfaceElevated = Color(0xFFFFFFFF)
    val surfaceGlassBase = Color(0xB8FFFFFF) // ~72% white
    val surfaceSelected = Color(0xFFE1E4E9)
    val borderSubtle = Color(0x17000000) // ~9% black
    val borderStrong = Color(0x24000000) // ~14% black
    val textPrimary = Color(0xFF15171B)
    val textSecondary = Color(0xFF606671)
    val textMuted = Color(0xFF777D87)
    val silver = Color(0xFF606773)
    val silverBright = Color(0xFF343941)
    val disabled = Color(0xFFA7ABB3)
    val error = Color(0xFFB00020)
    val success = Color(0xFF2E7D32)
}

/**
 * Semantic color scheme used throughout the app.
 * Always prefer these tokens over hard-coded colors.
 */
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
    isDark = false
)

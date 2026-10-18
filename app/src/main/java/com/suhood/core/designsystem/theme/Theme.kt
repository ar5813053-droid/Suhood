package com.suhood.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalSuhoodColors = staticCompositionLocalOf { DarkColorScheme }

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

@Composable
fun SuhoodTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colors = if (isDark) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalSuhoodColors provides colors) {
        content()
    }
}

object SuhoodTheme {
    val colors: SuhoodColorScheme
        @Composable
        @ReadOnlyComposable
        get() = LocalSuhoodColors.current
}

/** Convenience accessors */
val suhoodColors: SuhoodColorScheme
    @Composable
    @ReadOnlyComposable
    get() = LocalSuhoodColors.current

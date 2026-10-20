package com.suhood.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.suhood.core.designsystem.theme.suhoodColors

/**
 * Google Material–style solid grey background for all main screens.
 */
@Composable
fun AppBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {}
) {
    val colors = suhoodColors
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        content()
    }
}

/** Solid dark background for Nasheedify. */
@Composable
fun SolidDarkBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
    ) {
        content()
    }
}

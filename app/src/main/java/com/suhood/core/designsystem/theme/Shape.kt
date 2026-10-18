package com.suhood.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Consistent shape tokens for Hybrid Glass UI.
 */
object SuhoodShapes {
    /** Main content cards */
    val card = RoundedCornerShape(28.dp)

    /** Secondary / smaller cards */
    val cardSmall = RoundedCornerShape(20.dp)

    /** Buttons, pills, chips */
    val pill = RoundedCornerShape(50)

    /** Dialogs and sheets */
    val dialog = RoundedCornerShape(28.dp)

    /** Bottom sheets */
    val bottomSheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

    /** Search fields */
    val search = RoundedCornerShape(16.dp)

    /** Icon buttons */
    val iconButton = RoundedCornerShape(14.dp)

    /** Navigation capsule */
    val navPill = RoundedCornerShape(50)

    /** Album artwork */
    val artwork = RoundedCornerShape(16.dp)

    /** Tiny badges */
    val badge = RoundedCornerShape(8.dp)
}

object SuhoodSpacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 20.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 40.dp

    /** Standard horizontal screen margin */
    val screenHorizontal = 20.dp

    /**
     * Bottom content inset when floating bottom nav is visible.
     * Clears: nav height (60dp) + vertical padding (20dp) + system nav bars (handled separately by content).
     */
    val underNav = 96.dp

    /** Alias for bottom nav clearance */
    val aboveBottomNav = underNav
}

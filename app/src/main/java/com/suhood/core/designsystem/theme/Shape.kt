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
     * Top padding under floating nav.
     * Must clear: status bar (~24–48dp) + nav vertical insets (16dp) + bar height (52dp).
     * 72dp was too small — titles drew under the capsule on real devices.
     */
    val underNav = 112.dp
}

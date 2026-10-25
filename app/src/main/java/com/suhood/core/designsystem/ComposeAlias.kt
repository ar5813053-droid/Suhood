package com.suhood.core.designsystem

/**
 * Kotlin 2.0 can treat androidx.compose.ui.Modifier as a package (modifier subpackage).
 * Typealias forces classifier resolution so call-sites stay clean.
 */
typealias SuModifier = androidx.compose.ui.modifier

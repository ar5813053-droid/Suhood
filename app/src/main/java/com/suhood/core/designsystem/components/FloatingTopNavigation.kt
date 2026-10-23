package com.suhood.core.designsystem.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suhood.core.navigation.TopLevelDestination
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class NavItem(
    val destination: TopLevelDestination,
    val label: String,
    val icon: ImageVector,
    val contentDescription: String
)

val primaryNavItems = listOf(
    NavItem(TopLevelDestination.HOME, "Home", Icons.Filled.Home, "Home"),
    NavItem(TopLevelDestination.QURAN, "Quran", Icons.Filled.MenuBook, "Quran"),
    NavItem(TopLevelDestination.AUDIO, "Nasheedify", Icons.Filled.MusicNote, "Nasheedify")
)

/**
 * CONVEX-style floating glass bar:
 * - Soft dark capsule with subtle border
 * - Sliding circular glass indicator that morphs between tabs
 * - Long-press expands glass; drag to preview tabs; release to navigate
 * - Smooth spring physics matching reference video
 */
@Composable
fun FloatingTopNavigation(
    selected: TopLevelDestination,
    onSelect: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(false) }
    var barWidthPx by remember { mutableFloatStateOf(1f) }
    val selectedIndex = primaryNavItems.indexOfFirst { it.destination == selected }.coerceAtLeast(0)
    var hoverIndex by remember { mutableIntStateOf(selectedIndex) }

    val displayIndex = if (expanded) hoverIndex else selectedIndex
    val slotCount = primaryNavItems.size

    // Sliding indicator position (0f..1f fraction across bar)
    val indicatorFraction = remember { Animatable(selectedIndex / slotCount.toFloat()) }
    LaunchedEffect(displayIndex, barWidthPx) {
        val target = (displayIndex + 0.5f) / slotCount.toFloat()
        indicatorFraction.animateTo(
            target,
            spring(dampingRatio = 0.78f, stiffness = 280f)
        )
    }

    val expandScale by animateFloatAsState(
        targetValue = if (expanded) 1.06f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
        label = "expandScale"
    )
    val barHeight by animateDpAsState(
        targetValue = if (expanded) 70.dp else 60.dp,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 320f),
        label = "barH"
    )
    val indicatorSize by animateDpAsState(
        targetValue = if (expanded) 52.dp else 46.dp,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f),
        label = "indSize"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 28.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = expandScale
                    scaleY = expandScale
                }
                .onSizeChanged { barWidthPx = it.width.toFloat().coerceAtLeast(1f) }
                .shadow(
                    elevation = if (expanded) 26.dp else 14.dp,
                    shape = RoundedCornerShape(36.dp),
                    ambientColor = Color.Black.copy(alpha = 0.5f),
                    spotColor = Color.Black.copy(alpha = 0.35f)
                )
                .clip(RoundedCornerShape(36.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xD92C2C2E),
                            Color(0xF01C1C1E)
                        )
                    )
                )
                .border(
                    width = 0.8.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.28f),
                            Color.White.copy(alpha = 0.06f)
                        )
                    ),
                    shape = RoundedCornerShape(36.dp)
                )
                .height(barHeight)
                .fillMaxWidth()
                .pointerInput(barWidthPx, selectedIndex) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            expanded = true
                            hoverIndex = selectedIndex
                        },
                        onDragEnd = {
                            expanded = false
                            val dest = primaryNavItems.getOrNull(hoverIndex)?.destination
                            if (dest != null && dest != selected) onSelect(dest)
                        },
                        onDragCancel = { expanded = false },
                        onDrag = { change, _ ->
                            change.consume()
                            val x = change.position.x.coerceIn(0f, barWidthPx)
                            val idx = ((x / barWidthPx) * slotCount)
                                .toInt()
                                .coerceIn(0, slotCount - 1)
                            hoverIndex = idx
                        }
                    )
                }
        ) {
            val w = constraints.maxWidth.toFloat().coerceAtLeast(1f)
            val slotW = w / slotCount
            val indPx = with(density) { indicatorSize.toPx() }

            // Sliding glass circle indicator
            Box(
                Modifier
                    .offset {
                        IntOffset(
                            x = (indicatorFraction.value * w - indPx / 2f).roundToInt(),
                            y = ((constraints.maxHeight - indPx) / 2f).roundToInt()
                        )
                    }
                    .size(indicatorSize)
                    .shadow(8.dp, CircleShape, ambientColor = Color.Black.copy(0.35f))
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color(0xFF4A4A4C),
                                Color(0xFF2C2C2E)
                            )
                        )
                    )
                    .border(0.7.dp, Color.White.copy(alpha = 0.18f), CircleShape)
            )

            Row(
                Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                primaryNavItems.forEachIndexed { index, item ->
                    val isSel = index == displayIndex
                    val iconAlpha by animateFloatAsState(
                        targetValue = if (isSel) 1f else 0.55f,
                        animationSpec = tween(180),
                        label = "a$index"
                    )
                    val scale = remember { Animatable(1f) }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .pointerInput(item.destination) {
                                detectTapGestures(
                                    onTap = {
                                        scope.launch {
                                            scale.snapTo(1f)
                                            scale.animateTo(
                                                0.86f,
                                                spring(stiffness = Spring.StiffnessHigh)
                                            )
                                            onSelect(item.destination)
                                            scale.animateTo(
                                                1.08f,
                                                spring(dampingRatio = 0.42f, stiffness = 400f)
                                            )
                                            scale.animateTo(
                                                1f,
                                                spring(dampingRatio = 0.72f, stiffness = 280f)
                                            )
                                        }
                                    }
                                )
                            }
                            .semantics {
                                role = Role.Tab
                                contentDescription = item.contentDescription
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.graphicsLayer {
                                scaleX = scale.value
                                scaleY = scale.value
                                alpha = iconAlpha
                            }
                        ) {
                            Icon(
                                item.icon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                item.label,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FloatingBottomNavigation(
    selected: TopLevelDestination,
    onSelect: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingTopNavigation(selected, onSelect, modifier)
}

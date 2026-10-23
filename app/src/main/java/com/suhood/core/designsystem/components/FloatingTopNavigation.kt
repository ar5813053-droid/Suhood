package com.suhood.core.designsystem.components

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.geometry.Offset
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
 * CONVEX-inspired glassy bottom bar:
 * - Soft translucent capsule
 * - Selected pill highlight
 * - Long-press expands glass; drag to switch tabs; release navigates
 */
@Composable
fun FloatingTopNavigation(
    selected: TopLevelDestination,
    onSelect: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var expanded by remember { mutableStateOf(false) }
    var hoverIndex by remember { mutableIntStateOf(primaryNavItems.indexOfFirst { it.destination == selected }.coerceAtLeast(0)) }
    var barWidthPx by remember { mutableFloatStateOf(1f) }
    val scope = rememberCoroutineScope()
    val expandScale by animateFloatAsState(
        targetValue = if (expanded) 1.08f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
        label = "navExpand"
    )
    val barHeight by animateDpAsState(
        targetValue = if (expanded) 72.dp else 62.dp,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium),
        label = "navH"
    )
    val selectedIndex = primaryNavItems.indexOfFirst { it.destination == selected }.coerceAtLeast(0)
    val displayIndex = if (expanded) hoverIndex else selectedIndex

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = expandScale
                    scaleY = expandScale
                }
                .onSizeChanged { barWidthPx = it.width.toFloat() }
                .shadow(
                    elevation = if (expanded) 28.dp else 16.dp,
                    shape = RoundedCornerShape(40.dp),
                    ambientColor = Color.Black.copy(alpha = 0.55f),
                    spotColor = Color.Black.copy(alpha = 0.4f)
                )
                .clip(RoundedCornerShape(40.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xCC2C2C2E),
                            Color(0xE61C1C1E)
                        )
                    )
                )
                .border(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.06f))
                    ),
                    RoundedCornerShape(40.dp)
                )
                .height(barHeight)
                .padding(horizontal = 8.dp)
                .pointerInput(barWidthPx, selected) {
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
                            val idx = ((x / barWidthPx) * primaryNavItems.size)
                                .toInt()
                                .coerceIn(0, primaryNavItems.lastIndex)
                            hoverIndex = idx
                        }
                    )
                },
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            primaryNavItems.forEachIndexed { index, item ->
                val isSel = index == displayIndex
                val pill by animateColorAsState(
                    if (isSel) Color(0xFF3A3A3C) else Color.Transparent,
                    tween(180),
                    label = "pill$index"
                )
                val tint by animateColorAsState(
                    if (isSel) Color.White else Color(0x88EBEBF5),
                    tween(180),
                    label = "tint$index"
                )
                val scale = remember { Animatable(1f) }
                Column(
                    modifier = Modifier
                        .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
                        .clip(RoundedCornerShape(28.dp))
                        .background(pill)
                        .pointerInput(item.destination) {
                            detectTapGestures(
                                onTap = {
                                    scope.launch {
                                        scale.animateTo(0.88f, spring(stiffness = Spring.StiffnessHigh))
                                        onSelect(item.destination)
                                        scale.animateTo(1.05f, spring(dampingRatio = 0.45f))
                                        scale.animateTo(1f, spring(dampingRatio = 0.7f))
                                    }
                                }
                            )
                        }
                        .semantics {
                            role = Role.Tab
                            contentDescription = item.contentDescription
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(item.icon, null, tint = tint, modifier = Modifier.size(22.dp))
                    Text(
                        item.label,
                        color = tint,
                        fontSize = 11.sp,
                        fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 1
                    )
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

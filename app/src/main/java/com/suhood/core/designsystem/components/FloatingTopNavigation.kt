package com.suhood.core.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.suhood.core.designsystem.theme.suhoodColors
import com.suhood.core.navigation.TopLevelDestination
import kotlinx.coroutines.launch

data class NavItem(
    val destination: TopLevelDestination,
    val label: String,
    val icon: ImageVector,
    val contentDescription: String
)

val primaryNavItems = listOf(
    NavItem(TopLevelDestination.HOME, "Home", Icons.Filled.Home, "Home"),
    NavItem(TopLevelDestination.QURAN, "Quran", Icons.Filled.MenuBook, "Quran"),
    NavItem(TopLevelDestination.AUDIO, "Audio", Icons.Filled.MusicNote, "Nasheedify")
)

@Composable
fun FloatingTopNavigation(
    selected: TopLevelDestination,
    onSelect: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = suhoodColors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .shadow(
                    elevation = 18.dp,
                    shape = RoundedCornerShape(30.dp),
                    ambientColor = Color.Black.copy(alpha = 0.45f),
                    spotColor = Color.Black.copy(alpha = 0.35f)
                )
                .clip(RoundedCornerShape(30.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            colors.surfaceElevated.copy(alpha = 0.94f),
                            colors.surface.copy(alpha = 0.88f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.28f),
                            Color.White.copy(alpha = 0.06f)
                        )
                    ),
                    shape = RoundedCornerShape(30.dp)
                )
                .drawBehind {
                    drawRect(
                        brush = Brush.verticalGradient(
                            0f to Color.White.copy(alpha = 0.18f),
                            0.35f to Color.Transparent
                        ),
                        size = size.copy(height = size.height * 0.45f)
                    )
                }
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            primaryNavItems.forEach { item ->
                NavBarItem(
                    item = item,
                    selected = selected == item.destination,
                    onClick = { onSelect(item.destination) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun NavBarItem(
    item: NavItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = suhoodColors
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val tint by animateColorAsState(
        if (selected) colors.accentLavender else colors.textMuted,
        tween(180),
        label = "navTint"
    )
    val pill by animateColorAsState(
        if (selected) colors.accentLavender.copy(alpha = 0.18f) else Color.Transparent,
        tween(180),
        label = "navPill"
    )

    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            .clip(RoundedCornerShape(20.dp))
            .background(pill)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                scope.launch {
                    scale.animateTo(0.86f, spring(stiffness = Spring.StiffnessHigh, dampingRatio = 0.6f))
                    onClick()
                    scale.animateTo(1.06f, spring(stiffness = Spring.StiffnessMedium, dampingRatio = 0.4f))
                    scale.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow))
                }
            }
            .semantics {
                role = Role.Tab
                contentDescription = item.contentDescription
            }
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(item.icon, null, tint = tint, modifier = Modifier.size(24.dp))
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

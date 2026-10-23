package com.suhood.core.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    NavItem(TopLevelDestination.AUDIO, "Nasheedify", Icons.Filled.MusicNote, "Nasheedify")
)

/**
 * CONVEX-style floating bottom bar:
 * soft dark capsule, selected pill, labels, press scale — no liquid glass.
 */
@Composable
fun FloatingTopNavigation(
    selected: TopLevelDestination,
    onSelect: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 28.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(36.dp),
                    ambientColor = Color.Black.copy(alpha = 0.5f),
                    spotColor = Color.Black.copy(alpha = 0.4f)
                )
                .clip(RoundedCornerShape(36.dp))
                .background(Color(0xE61C1C1E))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            primaryNavItems.forEach { item ->
                ConvexNavItem(
                    item = item,
                    selected = selected == item.destination,
                    onClick = { onSelect(item.destination) }
                )
            }
        }
    }
}

@Composable
private fun ConvexNavItem(
    item: NavItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = suhoodColors
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val iconTint by animateColorAsState(
        if (selected) Color.White else Color(0x99EBEBF5),
        tween(200),
        label = "iconTint"
    )
    val labelTint by animateColorAsState(
        if (selected) Color.White else Color(0x66EBEBF5),
        tween(200),
        label = "labelTint"
    )
    val pillBg by animateColorAsState(
        if (selected) Color(0xFF3A3A3C) else Color.Transparent,
        tween(220),
        label = "pillBg"
    )

    Column(
        modifier = Modifier
            .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            .clip(RoundedCornerShape(28.dp))
            .background(pillBg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                scope.launch {
                    scale.animateTo(0.88f, spring(stiffness = Spring.StiffnessHigh, dampingRatio = 0.55f))
                    onClick()
                    scale.animateTo(1.04f, spring(stiffness = Spring.StiffnessMedium, dampingRatio = 0.4f))
                    scale.animateTo(1f, spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow))
                }
            }
            .semantics {
                role = Role.Tab
                contentDescription = item.contentDescription
            }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            item.icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(22.dp)
        )
        Text(
            item.label,
            color = labelTint,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1
        )
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

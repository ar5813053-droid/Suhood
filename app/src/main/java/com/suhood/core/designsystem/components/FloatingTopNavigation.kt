package com.suhood.core.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.suhood.core.designsystem.theme.SuhoodShapes
import com.suhood.core.designsystem.theme.SuhoodTypography
import com.suhood.core.designsystem.theme.suhoodColors
import com.suhood.core.navigation.TopLevelDestination

data class NavItem(
    val destination: TopLevelDestination,
    val label: String,
    val icon: ImageVector,
    val contentDescription: String
)

val primaryNavItems = listOf(
    NavItem(TopLevelDestination.HOME, "Home", Icons.Filled.Home, "Home"),
    NavItem(TopLevelDestination.WORSHIP, "Worship", Icons.Filled.SelfImprovement, "Worship"),
    NavItem(TopLevelDestination.FOCUS, "Focus", Icons.Filled.Timelapse, "Focus"),
    NavItem(TopLevelDestination.AUDIO, "Audio", Icons.Filled.MusicNote, "Nasheedify Audio")
)

/**
 * Compact floating top navigation capsule.
 * Positioned below status bar with horizontal insets.
 * Hybrid Glass appearance: translucent + silver border.
 */
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
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = SuhoodShapes.navPill,
                    ambientColor = Color.Black.copy(alpha = 0.35f),
                    spotColor = Color.Black.copy(alpha = 0.2f)
                )
                .clip(SuhoodShapes.navPill)
                .background(colors.surfaceElevated.copy(alpha = 0.92f))
                .border(1.dp, colors.borderStrong, SuhoodShapes.navPill)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Subtle top glass highlight
            primaryNavItems.forEach { item ->
                NavPillItem(
                    item = item,
                    selected = item.destination == selected,
                    onClick = { onSelect(item.destination) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun NavPillItem(
    item: NavItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = suhoodColors
    val iconColor by animateColorAsState(
        targetValue = if (selected) colors.silverBright else colors.textMuted,
        animationSpec = tween(200),
        label = "navIcon"
    )
    val bg by animateColorAsState(
        targetValue = if (selected) colors.surfaceSelected else Color.Transparent,
        animationSpec = tween(200),
        label = "navBg"
    )

    Box(
        modifier = modifier
            .height(40.dp)
            .clip(SuhoodShapes.pill)
            .background(bg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .semantics {
                contentDescription = item.contentDescription
                role = Role.Tab
                this.selected = selected
            },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
            if (selected) {
                Spacer(Modifier.width(6.dp))
                Text(
                    text = item.label,
                    style = SuhoodTypography.labelMedium,
                    color = colors.textPrimary,
                    maxLines = 1
                )
            }
        }
    }
}

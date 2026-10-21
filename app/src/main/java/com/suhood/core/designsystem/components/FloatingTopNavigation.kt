package com.suhood.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.suhood.core.designsystem.theme.SuhoodTypography
import com.suhood.core.designsystem.theme.suhoodColors
import com.suhood.core.navigation.TopLevelDestination

data class NavItem(
    val destination: TopLevelDestination,
    val label: String,
    val icon: ImageVector,
    val contentDescription: String
)

/** Exactly three top-level destinations: Home, Quran, Nasheedify. */
val primaryNavItems = listOf(
    NavItem(TopLevelDestination.HOME, "Home", Icons.Filled.Home, "Home"),
    NavItem(TopLevelDestination.JOURNEY, "Journey", Icons.Filled.Checklist, "My Journey"),
    NavItem(TopLevelDestination.QURAN, "Quran", Icons.Filled.MenuBook, "Quran"),
    NavItem(TopLevelDestination.MUHASABAH, "Journal", Icons.Filled.EditNote, "Muhasabah"),
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
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(colors.surfaceElevated.copy(alpha = 0.92f))
                .border(1.dp, colors.borderSubtle.copy(alpha = 0.7f), RoundedCornerShape(28.dp))
                .padding(horizontal = 8.dp),
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
    val tint = if (selected) colors.accentLavender else colors.textMuted
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .then(
                if (selected) Modifier.background(colors.accentLavender.copy(alpha = 0.16f))
                else Modifier
            )
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Tab
                contentDescription = item.contentDescription
            }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        // Icons only — labels stay in contentDescription for TalkBack
        Icon(
            item.icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
    }
}

/** Alias used by SuhoodNavGraph. */
@Composable
fun FloatingBottomNavigation(
    selected: TopLevelDestination,
    onSelect: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingTopNavigation(selected = selected, onSelect = onSelect, modifier = modifier)
}

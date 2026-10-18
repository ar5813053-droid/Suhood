package com.suhood.core.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.suhood.core.designsystem.theme.SuhoodShapes
import com.suhood.core.designsystem.theme.SuhoodSpacing
import com.suhood.core.designsystem.theme.SuhoodTypography
import com.suhood.core.designsystem.theme.suhoodColors

/**
 * Core Hybrid Glass surface — translucent background + thin silver border.
 * Blur is approximated via semi-transparent layers; real blur is expensive on many devices.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = SuhoodShapes.card,
    elevated: Boolean = false,
    selected: Boolean = false,
    borderWidth: Dp = 1.dp,
    content: @Composable () -> Unit
) {
    val colors = suhoodColors
    val bg = when {
        selected -> colors.surfaceSelected
        elevated -> colors.surfaceElevated
        else -> colors.surfaceGlass
    }
    val borderColor = if (selected) colors.borderStrong else colors.borderSubtle

    Box(
        modifier = modifier
            .shadow(
                elevation = if (elevated) 8.dp else 2.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.25f),
                spotColor = Color.Black.copy(alpha = 0.15f)
            )
            .clip(shape)
            .background(bg)
            .border(borderWidth, borderColor, shape)
    ) {
        // Subtle top highlight for glass reflection
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (colors.isDark) 0.06f else 0.15f),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = 80f
                    )
                )
        )
        content()
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    selected: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(SuhoodSpacing.lg),
    content: @Composable () -> Unit
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    } else Modifier

    GlassSurface(
        modifier = modifier.then(clickableModifier),
        shape = SuhoodShapes.card,
        elevated = true,
        selected = selected
    ) {
        Box(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}

@Composable
fun GlassPill(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    val colors = suhoodColors
    val bg by animateColorAsState(
        targetValue = if (selected) colors.surfaceSelected else colors.surfaceGlass,
        animationSpec = tween(200),
        label = "pillBg"
    )
    val border by animateColorAsState(
        targetValue = if (selected) colors.borderStrong else colors.borderSubtle,
        animationSpec = tween(200),
        label = "pillBorder"
    )

    val clickMod = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    } else Modifier

    Row(
        modifier = modifier
            .then(clickMod)
            .clip(SuhoodShapes.pill)
            .background(bg)
            .border(1.dp, border, SuhoodShapes.pill)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        content = content
    )
}

@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    primary: Boolean = true,
    icon: ImageVector? = null,
    contentDescription: String? = null
) {
    val colors = suhoodColors
    val bg = when {
        !enabled -> colors.disabled.copy(alpha = 0.3f)
        primary -> colors.surfaceSelected
        else -> colors.surfaceGlass
    }
    val textColor = when {
        !enabled -> colors.disabled
        else -> colors.textPrimary
    }

    Row(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(SuhoodShapes.pill)
            .background(bg)
            .border(1.dp, if (enabled) colors.borderStrong else colors.borderSubtle, SuhoodShapes.pill)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .semantics {
                this.contentDescription = contentDescription ?: text
                role = Role.Button
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = SuhoodTypography.labelLarge,
            color = textColor
        )
    }
}

@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 48.dp,
    iconSize: Dp = 22.dp
) {
    val colors = suhoodColors
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(colors.surfaceGlass)
            .border(1.dp, colors.borderSubtle, CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics {
                this.contentDescription = contentDescription
                role = Role.Button
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) colors.silverBright else colors.disabled,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
fun GlassSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search",
    leadingIcon: ImageVector? = null
) {
    val colors = suhoodColors
    GlassSurface(
        modifier = modifier.fillMaxWidth(),
        shape = SuhoodShapes.search,
        elevated = false
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = colors.textMuted,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(12.dp))
            }
            androidx.compose.foundation.text.BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = SuhoodTypography.bodyMedium.copy(color = colors.textPrimary),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    if (query.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = SuhoodTypography.bodyMedium,
                            color = colors.textMuted
                        )
                    }
                    inner()
                }
            )
        }
    }
}

@Composable
fun GlassSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = suhoodColors
    GlassSurface(
        modifier = modifier.fillMaxWidth(),
        shape = SuhoodShapes.pill,
        elevated = false
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            options.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(SuhoodShapes.pill)
                        .background(if (selected) colors.surfaceSelected else Color.Transparent)
                        .clickable { onSelect(index) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = SuhoodTypography.labelMedium,
                        color = if (selected) colors.textPrimary else colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun DemoStatusLabel(
    text: String = "Demo",
    modifier: Modifier = Modifier
) {
    val colors = suhoodColors
    Box(
        modifier = modifier
            .clip(SuhoodShapes.badge)
            .background(colors.surfaceSelected)
            .border(1.dp, colors.borderSubtle, SuhoodShapes.badge)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            style = SuhoodTypography.labelSmall,
            color = colors.textMuted
        )
    }
}

@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val colors = suhoodColors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(SuhoodSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = SuhoodTypography.headlineSmall,
            color = colors.textPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            style = SuhoodTypography.bodyMedium,
            color = colors.textSecondary,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(20.dp))
            GlassButton(text = actionLabel, onClick = onAction)
        }
    }
}

@Composable
fun LoadingState(
    message: String = "Loading…",
    modifier: Modifier = Modifier
) {
    val colors = suhoodColors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(SuhoodSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            style = SuhoodTypography.bodyMedium,
            color = colors.textSecondary
        )
    }
}

@Composable
fun ScreenHeader(
    title: String,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val colors = suhoodColors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = SuhoodSpacing.screenHorizontal, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            GlassIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                onClick = onBack,
                size = 40.dp,
                iconSize = 20.dp
            )
            Spacer(Modifier.width(12.dp))
        }
        Text(
            text = title,
            style = SuhoodTypography.headlineSmall,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        actions()
    }
}

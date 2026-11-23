package com.suhood.core.designsystem.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val SkeletonBase = Color(0xFF1A1C20)
private val SkeletonHighlight = Color(0xFF2A2D34)

@Composable
fun rememberSkeletonBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val x by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )
    return Brush.linearGradient(
        colors = listOf(SkeletonBase, SkeletonHighlight, SkeletonBase),
        start = Offset(x - 400f, 0f),
        end = Offset(x, 200f)
    )
}

@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(10.dp)
) {
    val brush = rememberSkeletonBrush()
    Box(modifier = modifier.clip(shape).background(brush))
}

@Composable
fun SkeletonLine(
    widthFraction: Float = 1f,
    height: Dp = 12.dp,
    modifier: Modifier = Modifier
) {
    SkeletonBox(
        modifier = modifier
            .fillMaxWidth(widthFraction)
            .height(height)
    )
}

@Composable
fun SkeletonBookCover(
    modifier: Modifier = Modifier
        .width(120.dp)
        .height(168.dp)
) {
    SkeletonBox(modifier = modifier, shape = RoundedCornerShape(12.dp))
}

@Composable
fun SkeletonBookCard(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SkeletonBookCover(Modifier = Modifier.width(72.dp).height(100.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SkeletonLine(widthFraction = 0.85f, height = 14.dp)
            SkeletonLine(widthFraction = 0.55f, height = 12.dp)
            SkeletonLine(widthFraction = 0.95f, height = 10.dp)
            SkeletonLine(widthFraction = 0.7f, height = 10.dp)
        }
    }
}

@Composable
fun SkeletonCategoryRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        repeat(4) {
            SkeletonBox(
                modifier = Modifier
                    .width(110.dp)
                    .height(72.dp),
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@Composable
fun SkeletonTopPicksRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(3) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SkeletonBookCover()
                SkeletonLine(widthFraction = 0.9f, height = 12.dp, modifier = Modifier.width(120.dp))
                SkeletonLine(widthFraction = 0.6f, height = 10.dp, modifier = Modifier.width(120.dp))
            }
        }
    }
}

@Composable
fun SkeletonReaderPage(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SkeletonBookCover(modifier = Modifier.width(100.dp).height(140.dp))
        Spacer(Modifier.height(8.dp))
        SkeletonLine(widthFraction = 0.7f, height = 16.dp)
        SkeletonLine(widthFraction = 0.45f, height = 12.dp)
        Spacer(Modifier.height(8.dp))
        repeat(10) {
            SkeletonLine(widthFraction = if (it % 3 == 2) 0.75f else 1f, height = 11.dp)
        }
    }
}

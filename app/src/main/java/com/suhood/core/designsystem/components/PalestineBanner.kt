package com.suhood.core.designsystem.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suhood.core.designsystem.theme.SuhoodTypography

const val PALESTINE_DONATION_URL =
    "https://islamic-relief.org/appeals/palestine-emergency-appeal/"

@Composable
fun PalestineBanner(
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(if (compact) 14.dp else 18.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF0D3B2E), Color(0xFF145C45), Color(0xFF1A6B4F))
                )
            )
            .border(1.dp, Color(0xFF2E8B6A).copy(alpha = 0.5f), shape)
            .clickable {
                runCatching {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(PALESTINE_DONATION_URL))
                    )
                }
            }
            .padding(
                horizontal = if (compact) 14.dp else 18.dp,
                vertical = if (compact) 12.dp else 16.dp
            )
    ) {
        Column(Modifier.fillMaxWidth()) {
            Text(
                if (compact) "Help Palestine Families Survive" else "They Fled With Nothing",
                style = if (compact) SuhoodTypography.titleSmall.copy(fontWeight = FontWeight.Bold)
                else SuhoodTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (compact) "Support emergency relief for families in need."
                else "Give meals to a Palestinian refugee family and bring comfort.",
                style = SuhoodTypography.bodySmall,
                color = Color.White.copy(alpha = 0.9f),
                lineHeight = 18.sp
            )
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    if (compact) "Support Now" else "Help Them",
                    style = SuhoodTypography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF0D3B2E)
                )
            }
        }
    }
}

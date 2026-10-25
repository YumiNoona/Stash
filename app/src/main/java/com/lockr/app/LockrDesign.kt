package com.lockr.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun AuroraBackdrop(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(LockrColors.Pearl)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xBFE4F7FF), Color(0x55E4F7FF), Color.Transparent),
                center = Offset(size.width * 0.08f, size.height * 0.17f),
                radius = size.width * 0.75f
            ),
            radius = size.width * 0.75f,
            center = Offset(size.width * 0.08f, size.height * 0.17f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x66FFD9D1), Color(0x33FFD9D1), Color.Transparent),
                center = Offset(size.width * 0.95f, size.height * 0.36f),
                radius = size.width * 0.72f
            ),
            radius = size.width * 0.72f,
            center = Offset(size.width * 0.95f, size.height * 0.36f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x66D3F2E9), Color(0x22D3F2E9), Color.Transparent),
                center = Offset(size.width * 0.2f, size.height * 0.96f),
                radius = size.width * 0.8f
            ),
            radius = size.width * 0.8f,
            center = Offset(size.width * 0.2f, size.height * 0.96f)
        )
    }
}

@Composable
fun VaultBackground(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.vault_aurora_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.035f)))
    }
}

@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    radius: Dp = 24.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(radius)
    Box(
        modifier = modifier
            .shadow(12.dp, shape, ambientColor = Color(0x1837485C), spotColor = Color(0x1C37485C))
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xDDFEFFFF), Color(0xCFF9FBFC), Color(0xE4FFFFFF)),
                    start = Offset.Zero,
                    end = Offset(520f, 860f)
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.82f), shape),
        content = content
    )
}

@Composable
fun IridescentOrb(
    modifier: Modifier = Modifier,
    orbSize: Dp = 46.dp
) {
    Box(
        modifier = modifier
            .size(orbSize)
            .shadow(12.dp, CircleShape, ambientColor = Color(0x447E9FC6))
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFFE8CFFF), Color(0xFFC8E8FF), Color(0xFFB9F2E2), Color(0xFFFFD9CE))
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.9f), CircleShape)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.9f), Color.Transparent),
                    center = Offset(this.size.width * 0.28f, this.size.height * 0.2f),
                    radius = this.size.minDimension * 0.65f
                )
            )
        }
    }
}

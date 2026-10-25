package com.lockr.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
fun StashTopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier.fillMaxWidth().height(64.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        if (onBack != null) {
            androidx.compose.material3.IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = LockrColors.TextPrimary)
            }
            Spacer(Modifier.width(4.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, color = LockrColors.TextPrimary, maxLines = 1)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = LockrColors.TextSecondary, maxLines = 1)
        }
        trailing?.invoke()
    }
}

enum class DockDestination { CARDS, DOCUMENTS }

@Composable
fun StashDock(
    selected: DockDestination?,
    onCards: () -> Unit,
    onAdd: () -> Unit,
    onDocuments: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassPanel(
        modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 7.dp, bottom = 8.dp),
        radius = 30.dp
    ) {
        Row(
            Modifier.fillMaxWidth().height(74.dp).padding(horizontal = 9.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
        ) {
            DockAction("Cards", Icons.Outlined.CreditCard, selected == DockDestination.CARDS, onCards, Modifier.weight(1f))
            Box(Modifier.width(72.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Box(
                    Modifier.size(58.dp)
                        .shadow(12.dp, CircleShape, ambientColor = Color(0x667ACCC6))
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Color(0xFFEEDFFF), Color(0xFFBFEDEB), Color(0xFFD6E8FF), Color(0xFFFFE2D4))))
                        .border(1.dp, Color.White, CircleShape)
                        .clickable(onClick = onAdd),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "Add to Stash", tint = LockrColors.TextPrimary, modifier = Modifier.size(29.dp))
                }
            }
            DockAction("Documents", Icons.Outlined.Description, selected == DockDestination.DOCUMENTS, onDocuments, Modifier.weight(1f))
        }
    }
}

@Composable
private fun DockAction(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.height(60.dp).clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick).padding(horizontal = 3.dp, vertical = 5.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        Icon(icon, contentDescription = label, tint = if (selected) LockrColors.Accent else LockrColors.TextPrimary, modifier = Modifier.size(23.dp))
        Text(label, color = if (selected) LockrColors.Accent else LockrColors.TextSecondary, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
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

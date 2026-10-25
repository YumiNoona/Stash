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
import androidx.compose.material.icons.outlined.ChevronLeft
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign

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
        modifier.fillMaxWidth().height(70.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            Box(
                Modifier.size(42.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.76f))
                    .border(1.dp, Color.White.copy(alpha = 0.9f), CircleShape).clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.ChevronLeft, contentDescription = "Back", tint = LockrColors.TextPrimary, modifier = Modifier.size(27.dp))
            }
            Spacer(Modifier.width(11.dp))
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
    Box(modifier.fillMaxWidth().height(100.dp).padding(start = 11.dp, end = 11.dp, bottom = 5.dp)) {
        GlassPanel(Modifier.fillMaxWidth().height(78.dp).align(Alignment.BottomCenter), radius = 38.dp) {
            Row(Modifier.fillMaxSize().padding(horizontal = 19.dp), verticalAlignment = Alignment.CenterVertically) {
                DockAction("Cards", Icons.Outlined.CreditCard, selected == DockDestination.CARDS, onCards, Modifier.weight(1f))
                Spacer(Modifier.width(84.dp))
                DockAction("Documents", Icons.Outlined.Description, selected == DockDestination.DOCUMENTS, onDocuments, Modifier.weight(1f))
            }
        }
        Box(Modifier.align(Alignment.TopCenter).size(74.dp).shadow(15.dp, CircleShape,
            ambientColor = Color(0x7774A9DB)).clip(CircleShape).clickable(onClick = onAdd),
            contentAlignment = Alignment.Center) {
            IridescentOrb(orbSize = 74.dp)
            Icon(Icons.Outlined.Add, contentDescription = "Add to Stash", tint = Color(0xFF08184B), modifier = Modifier.size(33.dp))
        }
    }
}

@Composable
private fun DockAction(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.height(56.dp).clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick).padding(horizontal = 2.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        Icon(icon, contentDescription = label, tint = if (selected) LockrColors.Accent else LockrColors.TextPrimary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(3.dp))
        Text(label, color = if (selected) LockrColors.Accent else LockrColors.TextSecondary,
            style = MaterialTheme.typography.labelSmall, maxLines = 1, textAlign = TextAlign.Center)
    }
}

@Composable
fun IridescentOrb(
    modifier: Modifier = Modifier,
    orbSize: Dp = 46.dp
) {
    Image(painterResource(R.drawable.opal_orb), contentDescription = null,
        contentScale = ContentScale.Fit, modifier = modifier.size(orbSize))
}

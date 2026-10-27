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
import androidx.compose.runtime.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Arrangement
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
    val base = LockrColors.Pearl
    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(base)
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
    StashBackdrop(modifier)
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
                    colors = if (LocalDarkTheme.current) listOf(Color(0xDA4A465D), Color(0xCA353D50), Color(0xD6464A60))
                        else listOf(Color(0xDDFEFFFF), Color(0xCFF9FBFC), Color(0xE4FFFFFF)),
                    start = Offset.Zero,
                    end = Offset(520f, 860f)
                )
            )
            .border(1.dp, if (LocalDarkTheme.current) Color(0x887F8CA6) else Color.White.copy(alpha = 0.82f), shape),
        content = content
    )
}

/** A shared floating glass surface for headers and compact controls. */
@Composable
fun FloatingGlass(modifier: Modifier = Modifier, shape: RoundedCornerShape = RoundedCornerShape(25.dp), content: @Composable BoxScope.() -> Unit) {
    val dark = LocalDarkTheme.current
    val fill = if (dark) listOf(Color(0x68585272), Color(0x78404D65), Color(0x58445965))
        else listOf(Color(0xB8FFFFFF), Color(0x97E9E2FF), Color(0xA0E5F7F1))
    val rim = Brush.linearGradient(listOf(Color.White.copy(alpha = if (dark) 0.4f else 0.8f), Color(0x5589CCC9), Color(0x77C9ADF3)))
    Box(modifier.shadow(8.dp, shape, ambientColor = Color(0x245F4A88), spotColor = Color(0x245F4A88))
        .clip(shape).background(Brush.linearGradient(fill)).border(1.dp, rim, shape), content = content)
}

@Composable
fun StashTopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(modifier.fillMaxWidth().height(80.dp).padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) FloatingGlass(Modifier.size(46.dp), RoundedCornerShape(23.dp)) {
            androidx.compose.material3.IconButton(onClick = onBack, modifier = Modifier.fillMaxSize()) {
                Icon(Icons.Outlined.ChevronLeft, "Back", tint = LockrColors.TextPrimary, modifier = Modifier.size(27.dp))
            }
        }
        Column(Modifier.weight(1f).padding(start = 5.dp, end = 2.dp), verticalArrangement = Arrangement.Center) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = LockrColors.TextPrimary,
                maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.labelSmall, color = LockrColors.TextSecondary,
                maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
        if (trailing != null) FloatingGlass(shape = RoundedCornerShape(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { trailing() }
        }
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
    val dark = LocalDarkTheme.current
    val shape = RoundedCornerShape(36.dp)
    val rim = Brush.linearGradient(if (dark) listOf(Color(0x99E4D6FF), Color(0x336ED8D3), Color(0x66FFFFFF))
        else listOf(Color.White, Color(0x668DD5D0), Color(0x99C7B3F5)))
    Box(modifier.fillMaxWidth().height(104.dp).padding(horizontal = 18.dp, vertical = 8.dp), contentAlignment = Alignment.BottomCenter) {
        Box(Modifier.widthIn(max = 368.dp).fillMaxWidth().height(68.dp)
            .shadow(16.dp, shape, ambientColor = Color(0x335D477A), spotColor = Color(0x335D477A))
            .clip(shape).background(Brush.verticalGradient(
                if (dark) listOf(Color(0x385A566A), Color(0x503A4354))
                else listOf(Color(0x70FFFFFF), Color(0x4DEDEAF8))))
            .border(1.dp, rim, shape)) {
            Row(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                DockAction("Cards", Icons.Outlined.CreditCard, selected == DockDestination.CARDS, onCards, Modifier.weight(1f))
                Spacer(Modifier.width(82.dp))
                DockAction("Documents", Icons.Outlined.Description, selected == DockDestination.DOCUMENTS, onDocuments, Modifier.weight(1f))
            }
        }
        Box(Modifier.align(Alignment.TopCenter).size(72.dp)
            .shadow(12.dp, CircleShape, ambientColor = Color(0x557E69C5))
            .clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFFD8C1FF), Color(0xFFB0E7E1), Color(0xFFFFD2B2))))
            .border(1.5.dp, rim, CircleShape).clickable(onClick = onAdd), contentAlignment = Alignment.Center) {
            IridescentOrb(orbSize = 70.dp)
            Icon(Icons.Outlined.Add, "Add to Stash", tint = Color(0xFF26213F), modifier = Modifier.size(31.dp))
        }
    }
}

@Composable
private fun DockAction(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(25.dp)
    val dark = LocalDarkTheme.current
    Column(modifier.height(54.dp).clip(shape)
        .background(if (selected) Brush.verticalGradient(
            if (dark) listOf(Color(0x99766A94), Color(0x55596D80)) else listOf(Color(0xCCF0E6FF), Color(0x99D7F0EB)))
            else Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent)))
        .border(1.dp, if (selected) LockrColors.Accent.copy(alpha = 0.35f) else Color.Transparent, shape)
        .clickable(onClick = onClick).padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(icon, label, tint = if (selected) LockrColors.Accent else LockrColors.TextPrimary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(3.dp))
        Text(label, color = if (selected) LockrColors.TextPrimary else LockrColors.TextSecondary,
            style = MaterialTheme.typography.labelSmall, maxLines = 1, textAlign = TextAlign.Center)
    }
}

@Composable
fun IridescentOrb(
    modifier: Modifier = Modifier,
    orbSize: Dp = 46.dp
) {
    val animate = LocalMotionEnabled.current
    val cycle = if (animate) rememberInfiniteTransition(label = "orb") else null
    val phase by cycle?.animateFloat(0f, 1f, infiniteRepeatable(tween(6500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "orb drift")
        ?: remember { mutableStateOf(0f) }
    Image(painterResource(R.drawable.opal_orb), contentDescription = null,
        contentScale = ContentScale.Fit, modifier = modifier.size(orbSize).graphicsLayer {
            rotationZ = -6f + phase * 12f
            scaleX = 0.96f + phase * 0.04f; scaleY = scaleX
            translationY = (phase - 0.5f) * 3f * density
        })
}

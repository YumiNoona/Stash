package com.lockr.app

import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class AddStage { CHOOSE, DOCUMENT, CARD }

@Composable
fun AddSourceSheet(
    stage: AddStage,
    onStageChange: (AddStage) -> Unit,
    onCamera: (VaultCategory) -> Unit,
    onPhotos: (VaultCategory) -> Unit,
    onFiles: (VaultCategory) -> Unit,
    onNfc: () -> Unit,
    onCardManual: () -> Unit,
    onManual: (VaultCategory) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(start = 19.dp, end = 19.dp, bottom = 25.dp)) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(Modifier.padding(top = 11.dp, bottom = 18.dp).size(39.dp, 4.dp)
                .background(Color(0xFFB4C2CC), CircleShape))
        }
        Row(Modifier.fillMaxWidth().height(41.dp), verticalAlignment = Alignment.CenterVertically) {
            if (stage != AddStage.CHOOSE) {
                Box(Modifier.size(39.dp).background(LockrColors.Surface.copy(alpha = 0.75f), CircleShape)
                    .clickable { onStageChange(AddStage.CHOOSE) }, contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back to Add",
                        tint = LockrColors.TextPrimary, modifier = Modifier.size(21.dp))
                }
            } else Spacer(Modifier.size(39.dp))
            Text(when (stage) {
                AddStage.CHOOSE -> "Add to Stash"
                AddStage.DOCUMENT -> "Add documents"
                AddStage.CARD -> "Add a card"
            }, style = MaterialTheme.typography.titleLarge, color = LockrColors.TextPrimary,
                modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(Modifier.size(39.dp))
        }
        Spacer(Modifier.height(18.dp))
        when (stage) {
            AddStage.CHOOSE -> {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    VaultTypeCard("Documents", "Images · PDFs · text", Icons.Outlined.Description,
                        listOf(LockrColors.Surface, Color(0xFFD9F1FF), Color(0xFFE9F9F4)),
                        Color(0xFF2F79A8), Color(0xFFA4EFE0), listOf(Color(0xFF234C52), Color(0xFF2B4058), Color(0xFF394561)), Modifier.weight(1f)) { onStageChange(AddStage.DOCUMENT) }
                    VaultTypeCard("Cards", "Scan · NFC · manual", Icons.Outlined.CreditCard,
                        listOf(LockrColors.Surface, Color(0xFFE9DDFB), Color(0xFFFBE9EF)),
                        Color(0xFF7054A8), Color(0xFFE0C9FF), listOf(Color(0xFF544069), Color(0xFF403C63), Color(0xFF344B5E)), Modifier.weight(1f)) { onStageChange(AddStage.CARD) }
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    SmallSource("Password", Icons.Outlined.Key, Modifier.weight(1f)) { onManual(VaultCategory.PASSWORD) }
                    SmallSource("Note", Icons.Outlined.EditNote, Modifier.weight(1f)) { onManual(VaultCategory.NOTE) }
                }
            }
            AddStage.DOCUMENT -> {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    SourceTile("Camera", Icons.Outlined.CameraAlt, Color(0xFFDDF5EF), Color(0xFF278C82), Modifier.weight(1f)) { onCamera(VaultCategory.DOCUMENT) }
                    SourceTile("Photos", Icons.Outlined.Image, Color(0xFFE7F1FF), Color(0xFF477EB5), Modifier.weight(1f)) { onPhotos(VaultCategory.DOCUMENT) }
                    SourceTile("Files", Icons.Outlined.FolderOpen, Color(0xFFF2E8FB), Color(0xFF866BAE), Modifier.weight(1f)) { onFiles(VaultCategory.DOCUMENT) }
                }
            }
            AddStage.CARD -> {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    SourceTile("Scan card", Icons.Outlined.CameraAlt, Color(0xFFDDF5EF), Color(0xFF278C82), Modifier.weight(1f)) { onCamera(VaultCategory.CARD) }
                    SourceTile("Photos", Icons.Outlined.Image, Color(0xFFE7F1FF), Color(0xFF477EB5), Modifier.weight(1f)) { onPhotos(VaultCategory.CARD) }
                    SourceTile("Files", Icons.Outlined.FolderOpen, Color(0xFFF2E8FB), Color(0xFF866BAE), Modifier.weight(1f)) { onFiles(VaultCategory.CARD) }
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    SmallSource("Tap with NFC", Icons.Outlined.Nfc, Modifier.weight(1f), onNfc)
                    SmallSource("Enter manually", Icons.Outlined.CreditCard, Modifier.weight(1f), onCardManual)
                }
            }
        }
    }
}

@Composable
private fun VaultTypeCard(title: String, subtitle: String, icon: ImageVector, colors: List<Color>, tint: Color,
                          darkAccent: Color, darkColors: List<Color>, modifier: Modifier, onClick: () -> Unit) {
    val dark = LocalDarkTheme.current
    val accent = if (dark) darkAccent else tint
    val shape = RoundedCornerShape(25.dp)
    val rim = Brush.linearGradient(if (dark)
        listOf(darkAccent.copy(alpha = 0.55f), Color.White.copy(alpha = 0.12f), darkAccent.copy(alpha = 0.22f))
        else listOf(Color.White, tint.copy(alpha = 0.16f), Color.White.copy(alpha = 0.9f)))
    Box(modifier.height(176.dp).shadow(7.dp, shape, ambientColor = accent.copy(alpha = 0.14f))
        .clip(shape).background(Brush.linearGradient(if (dark) darkColors else colors))
        .border(1.dp, rim, shape).clickable(onClick = onClick)) {
        Image(painterResource(if (title == "Cards") R.drawable.card_aurora_art else R.drawable.hero_vault_cutout), null,
            contentScale = ContentScale.Crop, modifier = Modifier.align(Alignment.TopEnd).size(145.dp, 108.dp)
                .graphicsLayer { rotationZ = if (title == "Cards") -14f else 8f; alpha = if (dark) 0.85f else 0.9f }.clip(RoundedCornerShape(18.dp)))
        Box(Modifier.size(145.dp).align(Alignment.TopEnd).offset(x = 37.dp, y = (-42).dp)
            .background(Brush.radialGradient(listOf(accent.copy(alpha = if (dark) 0.19f else 0.10f), Color.Transparent)), CircleShape))
        Column(Modifier.fillMaxWidth().height(176.dp).padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top) {
                Box(Modifier.size(56.dp).background(Brush.linearGradient(
                    if (dark) listOf(darkAccent.copy(alpha = 0.20f), darkAccent.copy(alpha = 0.06f))
                    else listOf(Color.White.copy(alpha = 0.96f), Color.White.copy(alpha = 0.55f))), CircleShape)
                    .border(1.dp, if (dark) darkAccent.copy(alpha = 0.45f) else Color.White, CircleShape),
                    contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(30.dp))
                }

            }
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, lineHeight = 21.sp),
                    color = if (dark) Color(0xFFFAF8FF) else LockrColors.TextPrimary, maxLines = 2)
                Text(subtitle, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 16.sp),
                    color = if (dark) Color(0xFFD7DFEE) else LockrColors.TextSecondary,
                    maxLines = 2, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
private fun SourceTile(label: String, icon: ImageVector, glow: Color, tint: Color, modifier: Modifier, onClick: () -> Unit) {
    val dark = LocalDarkTheme.current
    val shape = RoundedCornerShape(23.dp)
    val accent = if (dark) glow else tint
    val fills = if (dark) listOf(
        androidx.compose.ui.graphics.lerp(LockrColors.Surface, tint, 0.20f),
        androidx.compose.ui.graphics.lerp(LockrColors.Raised, tint, 0.12f))
        else listOf(Color.White.copy(alpha = 0.92f), glow.copy(alpha = 0.75f))
    Column(modifier.height(116.dp).clip(shape).background(Brush.linearGradient(fills))
        .border(1.dp, if (dark) glow.copy(alpha = 0.30f) else Color.White, shape)
        .clickable(onClick = onClick).padding(9.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(49.dp).background(if (dark) glow.copy(alpha = 0.12f) else glow, CircleShape)
            .border(1.dp, if (dark) glow.copy(alpha = 0.38f) else Color.White.copy(alpha = 0.7f), CircleShape),
            contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(26.dp))
        }
        Text(label, style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp), color = LockrColors.TextPrimary,
            maxLines = 1, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun SmallSource(label: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Row(modifier.height(55.dp).clip(shape)
        .background(Brush.linearGradient(listOf(LockrColors.Surface.copy(alpha = 0.85f), LockrColors.Raised.copy(alpha = 0.50f))))
        .border(1.dp, LockrColors.Accent.copy(alpha = if (LocalDarkTheme.current) 0.25f else 0.15f), shape)
        .clickable(onClick = onClick).padding(horizontal = 11.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        Icon(icon, contentDescription = null, tint = LockrColors.Accent, modifier = Modifier.size(21.dp))
        Text(label, color = LockrColors.TextPrimary, style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
            maxLines = 1, modifier = Modifier.padding(start = 7.dp))
    }
}

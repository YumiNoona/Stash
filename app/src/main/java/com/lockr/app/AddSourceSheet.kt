package com.lockr.app

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
                        Color(0xFF2F79A8), Modifier.weight(1f)) { onStageChange(AddStage.DOCUMENT) }
                    VaultTypeCard("Cards", "Scan · NFC · manual", Icons.Outlined.CreditCard,
                        listOf(LockrColors.Surface, Color(0xFFE9DDFB), Color(0xFFFBE9EF)),
                        Color(0xFF7054A8), Modifier.weight(1f)) { onStageChange(AddStage.CARD) }
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
                          modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(25.dp)
    Column(modifier.height(170.dp).clip(shape).background(Brush.linearGradient(if (LocalDarkTheme.current) listOf(LockrColors.Surface, LockrColors.Raised) else colors))
        .border(1.dp, LockrColors.Surface, shape).clickable(onClick = onClick).padding(15.dp),
        verticalArrangement = Arrangement.SpaceBetween) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top) {
            Box(Modifier.size(56.dp).background(LockrColors.Surface.copy(alpha = 0.76f), CircleShape),
                contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(31.dp))
            }
            Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = LockrColors.TextPrimary,
                modifier = Modifier.size(21.dp))
        }
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp, lineHeight = 20.sp),
                color = LockrColors.TextPrimary, maxLines = 2)
            Text(subtitle, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 16.sp),
                color = LockrColors.TextSecondary, maxLines = 2, modifier = Modifier.padding(top = 3.dp))
        }
    }
}

@Composable
private fun SourceTile(label: String, icon: ImageVector, glow: Color, tint: Color, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.height(110.dp).background(
        Brush.linearGradient(listOf(LockrColors.Surface, if (LocalDarkTheme.current) LockrColors.Raised else glow.copy(alpha = 0.75f))), RoundedCornerShape(23.dp))
        .border(1.dp, LockrColors.Surface, RoundedCornerShape(23.dp)).clickable(onClick = onClick).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(49.dp).background(glow, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(26.dp))
        }
        Text(label, style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp), color = LockrColors.TextPrimary,
            maxLines = 1, modifier = Modifier.padding(top = 7.dp))
    }
}

@Composable
private fun SmallSource(label: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Row(modifier.height(53.dp).background(LockrColors.Surface.copy(alpha = 0.83f), RoundedCornerShape(18.dp))
        .border(1.dp, LockrColors.Surface, RoundedCornerShape(18.dp)).clickable(onClick = onClick).padding(horizontal = 11.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        Icon(icon, contentDescription = null, tint = LockrColors.Accent, modifier = Modifier.size(21.dp))
        Text(label, color = LockrColors.TextPrimary, style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
            maxLines = 1, modifier = Modifier.padding(start = 7.dp))
    }
}

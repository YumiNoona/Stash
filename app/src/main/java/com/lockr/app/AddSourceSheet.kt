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
                Box(Modifier.size(39.dp).background(Color.White.copy(alpha = 0.75f), CircleShape)
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
                WideSource("Documents & files", "Scan, photos, PDFs and other files", Icons.Outlined.Description,
                    Color(0xFFDBF1FF), Color(0xFF377CB2)) { onStageChange(AddStage.DOCUMENT) }
                Spacer(Modifier.height(9.dp))
                WideSource("Cards", "Camera, contactless NFC or manual", Icons.Outlined.CreditCard,
                    Color(0xFFE9E1FC), Color(0xFF745CB6)) { onStageChange(AddStage.CARD) }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    SmallSource("Password", Icons.Outlined.Key, Modifier.weight(1f)) { onManual(VaultCategory.PASSWORD) }
                    SmallSource("Note", Icons.Outlined.EditNote, Modifier.weight(1f)) { onManual(VaultCategory.NOTE) }
                }
            }
            AddStage.DOCUMENT -> {
                Text("Bring in one page or a whole document. Review the result before it is saved.",
                    color = LockrColors.TextSecondary, style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 15.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    SourceTile("Camera", Icons.Outlined.CameraAlt, Color(0xFFDDF5EF), Color(0xFF278C82), Modifier.weight(1f)) { onCamera(VaultCategory.DOCUMENT) }
                    SourceTile("Photos", Icons.Outlined.Image, Color(0xFFE7F1FF), Color(0xFF477EB5), Modifier.weight(1f)) { onPhotos(VaultCategory.DOCUMENT) }
                    SourceTile("Files", Icons.Outlined.FolderOpen, Color(0xFFF2E8FB), Color(0xFF866BAE), Modifier.weight(1f)) { onFiles(VaultCategory.DOCUMENT) }
                }
                Text("Images, PDF and text files stay attached to the local item.",
                    color = LockrColors.TextSecondary, style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 14.dp))
            }
            AddStage.CARD -> {
                Text("Choose how to add your card. Check every detected detail before saving.",
                    color = LockrColors.TextSecondary, style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 15.dp))
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
                Text("NFC works only when a card exposes readable details. You can always scan or enter it manually.",
                    color = LockrColors.TextSecondary, style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 13.dp))
            }
        }
    }
}

@Composable
private fun WideSource(title: String, subtitle: String, icon: ImageVector, glow: Color, tint: Color, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(74.dp)
        .background(Brush.horizontalGradient(listOf(Color(0xF8FFFFFF), glow.copy(alpha = 0.78f))), RoundedCornerShape(23.dp))
        .border(1.dp, Color.White, RoundedCornerShape(23.dp)).clickable(onClick = onClick)
        .padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(45.dp).background(glow, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(25.dp))
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = LockrColors.TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = LockrColors.TextSecondary)
        }
        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = LockrColors.TextSecondary)
    }
}

@Composable
private fun SourceTile(label: String, icon: ImageVector, glow: Color, tint: Color, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.height(110.dp).background(
        Brush.linearGradient(listOf(Color(0xF8FFFFFF), glow.copy(alpha = 0.75f))), RoundedCornerShape(23.dp))
        .border(1.dp, Color.White, RoundedCornerShape(23.dp)).clickable(onClick = onClick).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(49.dp).background(glow, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(26.dp))
        }
        Text(label, style = MaterialTheme.typography.labelLarge, color = LockrColors.TextPrimary,
            maxLines = 1, modifier = Modifier.padding(top = 7.dp))
    }
}

@Composable
private fun SmallSource(label: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Row(modifier.height(53.dp).background(Color.White.copy(alpha = 0.83f), RoundedCornerShape(18.dp))
        .border(1.dp, Color.White, RoundedCornerShape(18.dp)).clickable(onClick = onClick).padding(horizontal = 11.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        Icon(icon, contentDescription = null, tint = LockrColors.Accent, modifier = Modifier.size(21.dp))
        Text(label, color = LockrColors.TextPrimary, style = MaterialTheme.typography.labelLarge,
            maxLines = 1, modifier = Modifier.padding(start = 7.dp))
    }
}

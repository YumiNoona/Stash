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
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun AddSourceSheet(
    onCamera: (VaultCategory?) -> Unit,
    onPhotos: (VaultCategory?) -> Unit,
    onFiles: (VaultCategory?) -> Unit,
    onManual: (VaultCategory) -> Unit
) {
    Column(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 23.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.padding(top = 11.dp, bottom = 17.dp).size(38.dp, 4.dp)
                .background(Color(0xFFB4C2CC), CircleShape)
        )
        Text("Add to Stash", style = MaterialTheme.typography.headlineSmall, color = LockrColors.TextPrimary)
        Spacer(Modifier.height(22.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SourceTile("Camera", Icons.Outlined.CameraAlt, Color(0xFFDDF5EF), Color(0xFF278C82), Modifier.weight(1f)) { onCamera(null) }
            SourceTile("Photos", Icons.Outlined.Image, Color(0xFFE7F1FF), Color(0xFF477EB5), Modifier.weight(1f)) { onPhotos(null) }
            SourceTile("Files", Icons.Outlined.FolderOpen, Color(0xFFF2E8FB), Color(0xFF866BAE), Modifier.weight(1f)) { onFiles(null) }
        }
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ManualShortcut("Password", Icons.Outlined.Key, Modifier.weight(1f)) { onManual(VaultCategory.PASSWORD) }
            ManualShortcut("Note", Icons.Outlined.EditNote, Modifier.weight(1f)) { onManual(VaultCategory.NOTE) }
        }
    }
}

@Composable
private fun SourceTile(label: String, icon: ImageVector, glow: Color, tint: Color, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.height(112.dp).background(
            Brush.linearGradient(listOf(Color(0xF5FFFFFF), glow.copy(alpha = 0.72f))),
            RoundedCornerShape(24.dp)
        ).border(1.dp, Color.White, RoundedCornerShape(24.dp)).clickable(onClick = onClick).padding(9.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(Modifier.size(51.dp).background(glow, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(27.dp))
        }
        Text(label, color = LockrColors.TextPrimary, style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun ManualShortcut(label: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier.height(53.dp).background(Color.White.copy(alpha = 0.78f), RoundedCornerShape(18.dp))
            .border(1.dp, Color.White, RoundedCornerShape(18.dp)).clickable(onClick = onClick).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = LockrColors.Accent, modifier = Modifier.size(21.dp))
        Text(label, color = LockrColors.TextPrimary, style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(start = 8.dp))
    }
}

package com.lockr.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun AddSourceSheet(
    onDismiss: () -> Unit,
    onCamera: (VaultCategory?) -> Unit,
    onPhotos: (VaultCategory?) -> Unit,
    onFiles: (VaultCategory?) -> Unit,
    onManual: (VaultCategory) -> Unit
) {
    var categoryHint by remember { mutableStateOf<VaultCategory?>(null) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Add to Stash", style = MaterialTheme.typography.headlineSmall, color = LockrColors.TextPrimary)
                Text("Choose how to bring something in.", style = MaterialTheme.typography.bodyMedium, color = LockrColors.TextSecondary, modifier = Modifier.padding(top = 3.dp))
            }
            TextButton(onClick = onDismiss) { Text("Close") }
        }
        Spacer(Modifier.height(18.dp))
        SourceAction(Icons.Outlined.CameraAlt, "Scan with camera", "Capture one or more pages", Color(0xFFDDF5EF), Color(0xFF278C82)) { onCamera(categoryHint) }
        Spacer(Modifier.height(9.dp))
        SourceAction(Icons.Outlined.Image, "Choose photos", "Select images from your gallery", Color(0xFFE7F1FF), Color(0xFF477EB5)) { onPhotos(categoryHint) }
        Spacer(Modifier.height(9.dp))
        SourceAction(Icons.Outlined.Description, "Choose files", "PDFs, images and text files", Color(0xFFF2E8FB), Color(0xFF866BAE)) { onFiles(categoryHint) }
        Text("Save as", style = MaterialTheme.typography.titleSmall, color = LockrColors.TextPrimary, modifier = Modifier.padding(top = 18.dp, bottom = 8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            CategoryChoice("Card", Icons.Outlined.CreditCard, categoryHint == VaultCategory.CARD, Modifier.weight(1f)) { categoryHint = VaultCategory.CARD }
            CategoryChoice("Document", Icons.Outlined.Description, categoryHint == VaultCategory.DOCUMENT, Modifier.weight(1f)) { categoryHint = VaultCategory.DOCUMENT }
            CategoryChoice("Password", Icons.Outlined.Key, categoryHint == VaultCategory.PASSWORD, Modifier.weight(1f)) { onManual(VaultCategory.PASSWORD) }
            CategoryChoice("Note", Icons.AutoMirrored.Outlined.Article, categoryHint == VaultCategory.NOTE, Modifier.weight(1f)) { onManual(VaultCategory.NOTE) }
        }
        Text(
            if (categoryHint == VaultCategory.CARD) "Stash will suggest card details after recognition. You can change this before saving."
            else if (categoryHint == VaultCategory.DOCUMENT) "Stash will keep the original and suggest searchable text when available."
            else "You can change the suggested category during review.",
            style = MaterialTheme.typography.bodySmall,
            color = LockrColors.TextSecondary,
            modifier = Modifier.padding(top = 9.dp, bottom = 6.dp)
        )
    }
}

@Composable
private fun SourceAction(icon: ImageVector, title: String, subtitle: String, background: Color, tint: Color, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(68.dp).clip(RoundedCornerShape(21.dp))
            .background(Color.White.copy(alpha = 0.78f)).border(1.dp, Color.White, RoundedCornerShape(21.dp))
            .clickable(onClick = onClick).padding(horizontal = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.foundation.layout.Box(Modifier.size(43.dp).clip(CircleShape).background(background), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = LockrColors.TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = LockrColors.TextSecondary)
        }
        Icon(Icons.AutoMirrored.Outlined.ArrowForwardIos, contentDescription = null, tint = LockrColors.TextSecondary, modifier = Modifier.size(17.dp))
    }
}

@Composable
private fun CategoryChoice(label: String, icon: ImageVector, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier.height(72.dp).clip(RoundedCornerShape(18.dp))
            .background(if (selected) Color(0xD8DFF3EF) else Color.White.copy(alpha = 0.72f))
            .border(1.dp, if (selected) Color(0xFF92CFC3) else Color.White, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick).padding(horizontal = 3.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = if (selected) LockrColors.Accent else LockrColors.TextSecondary, modifier = Modifier.size(19.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = LockrColors.TextPrimary, maxLines = 1, modifier = Modifier.padding(top = 4.dp))
    }
}

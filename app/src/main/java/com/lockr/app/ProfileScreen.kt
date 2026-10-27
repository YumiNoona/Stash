package com.lockr.app

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ProfileAvatar(photo: Bitmap?, size: Dp, modifier: Modifier = Modifier) {
    val rim = if (LocalDarkTheme.current) listOf(Color(0xFFE4D6FF), Color(0xFF91DDDD), Color(0xFFFFC6B0), Color(0xFFE4D6FF))
        else listOf(Color.White, Color(0xFF9EE2DE), Color(0xFFB49BE2), Color.White)
    Box(modifier.size(size).shadow(8.dp, CircleShape, ambientColor = LockrColors.Accent.copy(alpha = 0.25f))
        .background(LockrColors.Surface, CircleShape).border(3.dp, Brush.sweepGradient(rim), CircleShape)
        .padding(5.dp).clip(CircleShape), contentAlignment = Alignment.Center) {
        if (photo != null) Image(photo.asImageBitmap(), contentDescription = "Profile photo", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Image(painterResource(R.drawable.stash_logo), contentDescription = "Stash logo",
            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
    }
}

@Composable
fun ProfileScreen(
    name: String,
    photo: Bitmap?,
    itemCount: Int,
    onBack: () -> Unit,
    onChangeName: (String) -> Unit,
    onChoosePhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAllItems: () -> Unit,
    modifier: Modifier = Modifier
) {
    var editingName by remember { mutableStateOf(false) }
    var draftName by remember(name, editingName) { mutableStateOf(name) }
    Column(modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        StashTopBar("Profile", onBack = onBack)
        Column(Modifier.fillMaxWidth().padding(top = 35.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(116.dp).clickable(onClick = onChoosePhoto), contentAlignment = Alignment.Center) {
                ProfileAvatar(photo, 104.dp)
                Box(
                    Modifier.align(Alignment.BottomEnd).size(36.dp).clip(CircleShape).background(LockrColors.Surface)
                        .border(1.dp, Color(0xFFE0E8EE), CircleShape),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Outlined.CameraAlt, contentDescription = "Choose profile photo", tint = LockrColors.Accent, modifier = Modifier.size(19.dp)) }
            }
            Row(Modifier.padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(name, style = MaterialTheme.typography.headlineMedium, color = LockrColors.TextPrimary)
                IconButton(onClick = { editingName = true }, modifier = Modifier.size(39.dp)) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Edit profile name", tint = LockrColors.Accent, modifier = Modifier.size(19.dp))
                }
            }
            Text("$itemCount saved ${if (itemCount == 1) "item" else "items"}", color = LockrColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium)
            if (photo != null) TextButton(onClick = onRemovePhoto) { Text("Remove photo") }
        }
        Spacer(Modifier.height(33.dp))
        ProfileAction("All items", "Browse your collection", Icons.Outlined.Description, onOpenAllItems)
        Spacer(Modifier.height(11.dp))
        ProfileAction("Settings", "Security, privacy and export", Icons.Outlined.Settings, onOpenSettings)
    }
    if (editingName) {
        AlertDialog(
            onDismissRequest = { editingName = false },
            title = { Text("Your name") },
            text = { OutlinedTextField(draftName, onValueChange = { draftName = it.take(40) }, label = { Text("Display name") }, singleLine = true) },
            dismissButton = { TextButton(onClick = { editingName = false }) { Text("Cancel") } },
            confirmButton = { TextButton(onClick = { onChangeName(draftName.trim()); editingName = false }, enabled = draftName.isNotBlank()) { Text("Save") } }
        )
    }
}

@Composable
private fun ProfileAction(label: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    GlassPanel(Modifier.fillMaxWidth(), radius = 20.dp) {
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).clickable(onClick = onClick).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(CircleShape).background(Color(0xFFE7F2FA)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = Color(0xFF4C7C9B), modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f).padding(start = 13.dp), verticalArrangement = Arrangement.Center) {
                Text(label, style = MaterialTheme.typography.titleSmall, color = LockrColors.TextPrimary)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = LockrColors.TextSecondary)
            }
            Icon(Icons.AutoMirrored.Outlined.ArrowForwardIos, contentDescription = "Open $label", tint = LockrColors.TextSecondary, modifier = Modifier.size(15.dp))
        }
    }
}

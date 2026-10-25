package com.lockr.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun ProfileScreen(
    items: List<VaultItem>,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAllItems: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize().padding(horizontal = 22.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RoundProfileAction(Icons.AutoMirrored.Outlined.ArrowBack, "Back", onBack)
            Text("Vault Profile", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(start = 12.dp))
        }

        Column(
            Modifier.weight(1f).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(Modifier.size(104.dp), contentAlignment = Alignment.Center) {
                IridescentOrb(orbSize = 98.dp)
                Icon(Icons.Outlined.Lock, contentDescription = null, tint = Color(0xFF467A99), modifier = Modifier.size(39.dp))
            }
            Text("Stash", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 12.dp))
            Text("Your personal vault", color = LockrColors.TextSecondary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 2.dp))

            GlassPanel(Modifier.fillMaxWidth().padding(top = 25.dp), radius = 24.dp) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 5.dp)) {
                    ProfileSummaryRow(Icons.Outlined.Lock, "Storage", "Encrypted on this device", last = false)
                    ProfileSummaryRow(Icons.Outlined.Description, "Vault items", "${items.size} saved", last = false)
                    ProfileSummaryRow(Icons.Outlined.Key, "Connection", "Offline · no cloud account", last = true)
                }
            }

            ProfileAction("All items", "Browse your local vault", Icons.Outlined.Description, onOpenAllItems)
            ProfileAction("Security settings", "Manage privacy and backup", Icons.Outlined.Settings, onOpenSettings)
            Spacer(Modifier.padding(bottom = 15.dp))
        }
    }
}

@Composable
private fun RoundProfileAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(42.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.78f)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = label, tint = LockrColors.TextPrimary)
    }
}

@Composable
private fun ProfileSummaryRow(icon: ImageVector, label: String, value: String, last: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(39.dp).clip(CircleShape).background(Color(0xDDE7F3FC)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = Color(0xFF4C7C9B), modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f).padding(start = 11.dp)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(value, style = MaterialTheme.typography.bodySmall, color = LockrColors.TextSecondary)
        }
    }
    if (!last) androidx.compose.material3.HorizontalDivider(color = LockrColors.Line)
}

@Composable
private fun ProfileAction(label: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    GlassPanel(Modifier.fillMaxWidth().padding(top = 10.dp), radius = 19.dp) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(19.dp)).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(39.dp).clip(CircleShape).background(Color(0xDDE7F3FC)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = Color(0xFF4C7C9B), modifier = Modifier.size(21.dp))
            }
            Column(Modifier.weight(1f).padding(start = 11.dp)) {
                Text(label, style = MaterialTheme.typography.bodyMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = LockrColors.TextSecondary)
            }
            Icon(Icons.AutoMirrored.Outlined.ArrowForwardIos, contentDescription = null, tint = LockrColors.TextSecondary, modifier = Modifier.size(15.dp))
        }
    }
}

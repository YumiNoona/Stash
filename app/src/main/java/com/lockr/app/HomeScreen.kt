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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun HomeScreen(
    items: List<VaultItem>,
    profileName: String,
    profilePhoto: Bitmap?,
    onOpenSettings: () -> Unit,
    onOpenAllItems: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenItem: (VaultItem) -> Unit,
    onOpenDetails: (VaultItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize().padding(horizontal = 17.dp)) {
        Row(Modifier.fillMaxWidth().height(91.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(54.dp).clip(CircleShape).clickable(onClick = onOpenProfile), contentAlignment = Alignment.Center) {
                if (profilePhoto == null) IridescentOrb(orbSize = 52.dp) else ProfileAvatar(profilePhoto, 52.dp)
            }
            Text(profileName, style = MaterialTheme.typography.headlineSmall,
                color = LockrColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(start = 12.dp))
            Box(
                Modifier.size(45.dp).shadow(8.dp, CircleShape)
                    .clip(CircleShape).background(Color.White.copy(alpha = 0.82f))
                    .border(1.dp, Color.White.copy(alpha = 0.94f), CircleShape)
                    .clickable(onClick = onOpenSettings), contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = LockrColors.TextPrimary,
                    modifier = Modifier.size(23.dp))
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(15.dp)) {
            DashboardHero()
            Row(Modifier.fillMaxWidth().padding(horizontal = 3.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Recent Items", style = MaterialTheme.typography.titleMedium,
                    color = LockrColors.TextPrimary, modifier = Modifier.weight(1f))
                Text("See All", style = MaterialTheme.typography.labelMedium, color = Color(0xFF1C67EA),
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onOpenAllItems)
                        .padding(horizontal = 5.dp, vertical = 5.dp))
            }
            if (items.isEmpty()) {
                GlassPanel(Modifier.fillMaxWidth().height(76.dp), radius = 22.dp) {
                    Row(Modifier.fillMaxSize().padding(horizontal = 15.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(47.dp), contentAlignment = Alignment.Center) {
                            IridescentOrb(orbSize = 43.dp)
                        }
                        Column(Modifier.padding(start = 11.dp)) {
                            Text("Your vault is ready", style = MaterialTheme.typography.titleSmall, color = LockrColors.TextPrimary)
                            Text("Tap + to add your first item", style = MaterialTheme.typography.bodySmall, color = LockrColors.TextSecondary)
                        }
                    }
                }
            } else {
                items.take(4).forEach { item ->
                    RecentItemRow(item, onClick = { onOpenItem(item) }, onMore = { onOpenDetails(item) })
                }
            }
            Spacer(Modifier.height(92.dp))
        }
    }
}

@Composable
private fun DashboardHero() {
    val shape = RoundedCornerShape(30.dp)
    Box(
        Modifier.fillMaxWidth().height(237.dp).shadow(16.dp, shape, ambientColor = Color(0x504D9ABC))
            .clip(shape)
            .background(Brush.linearGradient(listOf(Color(0xFFEBDFFB), Color(0xFFDCF8F9), Color(0xFFFFEAD8))))
            .border(1.dp, Color.White.copy(alpha = 0.94f), shape)
    ) {
        Image(painterResource(R.drawable.hero_vault_cutout), contentDescription = "Vault with cards and documents",
            contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(
            listOf(Color(0xCFF7F5FF), Color(0x70F7F5FF), Color.Transparent), endX = 630f)))
        Text("Your\nSecure Space", style = MaterialTheme.typography.headlineSmall,
            color = Color(0xFF09174E), modifier = Modifier.align(Alignment.TopStart).padding(start = 24.dp, top = 29.dp))
    }
}

@Composable
private fun RecentItemRow(item: VaultItem, onClick: () -> Unit, onMore: () -> Unit) {
    GlassPanel(Modifier.fillMaxWidth().height(69.dp).clickable(onClick = onClick), radius = 21.dp) {
        Row(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            RecentThumbnail(item)
            Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.Center) {
                Text(item.title, style = MaterialTheme.typography.titleSmall, color = LockrColors.TextPrimary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${item.category.label} · ${item.time}", style = MaterialTheme.typography.bodySmall,
                    color = LockrColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box(Modifier.size(39.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.74f))
                .clickable(onClick = onMore), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.MoreVert, contentDescription = "More options for ${item.title}",
                    tint = LockrColors.TextPrimary, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun RecentThumbnail(item: VaultItem) {
    val shape = RoundedCornerShape(12.dp)
    val attachment = item.attachments.firstOrNull { it.mimeType.startsWith("image/") }
    var thumbnail by remember(item.id, attachment?.name) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(item.id, attachment?.name) {
        thumbnail = attachment?.let { withContext(Dispatchers.IO) { decodeAttachmentPreview(it.base64Data) } }
    }
    Box(Modifier.size(50.dp).clip(shape).background(Color.White.copy(alpha = 0.8f)), contentAlignment = Alignment.Center) {
        val icon: ImageVector = when (item.category) {
            VaultCategory.CARD -> Icons.Outlined.CreditCard
            VaultCategory.DOCUMENT -> Icons.Outlined.Description
            VaultCategory.PASSWORD -> Icons.Outlined.Key
            VaultCategory.NOTE -> Icons.AutoMirrored.Outlined.Article
        }
        if (thumbnail != null) {
            Image(thumbnail!!.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize())
        } else if (item.category == VaultCategory.CARD) {
            Image(painterResource(R.drawable.card_aurora_texture), null,
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        if (thumbnail == null) Icon(icon, contentDescription = null, tint = Color(0xFF17265C), modifier = Modifier.size(25.dp))
    }
}

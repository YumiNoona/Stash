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
import androidx.compose.foundation.layout.width
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
                ProfileAvatar(profilePhoto, 52.dp)
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
    GlassPanel(Modifier.fillMaxWidth().height(82.dp).clickable(onClick = onClick), radius = 23.dp) {
        Row(Modifier.fillMaxSize().padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            RecentThumbnail(item)
            Column(Modifier.weight(1f).padding(start = 13.dp), verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start) {
                Text(item.title, style = MaterialTheme.typography.titleSmall, color = LockrColors.TextPrimary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${item.category.label} · ${item.time}", style = MaterialTheme.typography.bodySmall,
                    color = LockrColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 3.dp))
            }
            Box(Modifier.size(34.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.66f))
                .clickable(onClick = onMore), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.MoreVert, contentDescription = "More options for ${item.title}",
                    tint = LockrColors.TextPrimary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun RecentThumbnail(item: VaultItem) {
    val shape = RoundedCornerShape(13.dp)
    val attachment = item.attachments.firstOrNull { it.mimeType.startsWith("image/") }
    var thumbnail by remember(item.id, attachment?.name, attachment?.byteCount) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(item.id, attachment?.name, attachment?.byteCount) {
        thumbnail = if (item.category == VaultCategory.DOCUMENT) attachment?.let {
            withContext(Dispatchers.IO) { decodeAttachmentPreview(it.base64Data, 220, 180) }
        } else null
    }
    Box(Modifier.width(70.dp).height(53.dp).clip(shape)
        .background(Brush.linearGradient(listOf(Color(0xFFE9F7FC), Color(0xFFEEE7FA))))
        .border(1.dp, Color.White.copy(alpha = 0.93f), shape), contentAlignment = Alignment.Center) {
        val icon: ImageVector = when (item.category) {
            VaultCategory.CARD -> Icons.Outlined.CreditCard
            VaultCategory.DOCUMENT -> Icons.Outlined.Description
            VaultCategory.PASSWORD -> Icons.Outlined.Key
            VaultCategory.NOTE -> Icons.AutoMirrored.Outlined.Article
        }
        if (item.category == VaultCategory.CARD) {
            Image(painterResource(cardArtStyle(item).image), null,
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Box(Modifier.align(Alignment.TopStart).padding(start = 7.dp, top = 8.dp)
                .size(width = 14.dp, height = 10.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Brush.linearGradient(listOf(Color(0xFFFFE9B5), Color(0xFFE4B96B)))))
            Text(item.lastFour.ifBlank { "••••" }, style = MaterialTheme.typography.labelSmall,
                color = if (cardArtStyle(item).dark) Color.White else Color(0xFF0B2055),
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 5.dp, bottom = 4.dp))
        } else if (thumbnail != null) {
            Image(thumbnail!!.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize())
        } else Icon(icon, contentDescription = null, tint = Color(0xFF3A598A), modifier = Modifier.size(27.dp))
    }
}

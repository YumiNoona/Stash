package com.lockr.app

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.util.Base64
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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
    onRenameItem: (VaultItem, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var menuItem by remember { mutableStateOf<VaultItem?>(null) }
    var menuPage by remember { mutableStateOf("actions") }
    var renameText by remember { mutableStateOf("") }
    val actions = LocalVaultActions.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var downloadItem by remember { mutableStateOf<VaultItem?>(null) }
    var cardDownloadItem by remember { mutableStateOf<VaultItem?>(null) }
    val cardDownloadLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        val card = cardDownloadItem
        cardDownloadItem = null
        if (uri != null && card != null) scope.launch(Dispatchers.IO) {
            val text = buildString {
                appendLine(card.title)
                listOf("issuer" to "Issuer", "cardType" to "Type", "network" to "Network",
                    "cardNumber" to "Number", "cardholder" to "Cardholder", "expiry" to "Expiry")
                    .forEach { (key, label) -> card.metadata[key]?.takeIf { it.isNotBlank() }?.let { appendLine("$label: $it") } }
            }
            context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray(Charsets.UTF_8)) }
        }
    }
    val downloadLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val attachment = downloadItem?.attachments?.firstOrNull()
        downloadItem = null
        if (uri != null && attachment != null) scope.launch(Dispatchers.IO) {
            val bytes = Base64.decode(attachment.base64Data, Base64.DEFAULT)
            try { context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) } }
            finally { bytes.fill(0) }
        }
    }
    menuItem?.let { item ->
        ModalBottomSheet(onDismissRequest = { menuItem = null; menuPage = "actions" },
            containerColor = LockrColors.Surface, shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)) {
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(when (menuPage) { "rename" -> "Rename item"; "delete" -> "Delete item?"; else -> item.title },
                    style = MaterialTheme.typography.titleLarge, color = LockrColors.TextPrimary, maxLines = 2)
                when (menuPage) {
                    "rename" -> {
                        OutlinedTextField(renameText, { renameText = it.take(80) }, label = { Text("Name") },
                            singleLine = true, modifier = Modifier.fillMaxWidth())
                        Button(onClick = { onRenameItem(item, renameText.trim()); menuItem = null; menuPage = "actions" },
                            enabled = renameText.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Save name") }
                    }
                    "delete" -> {
                        Text("This item and its files will be removed from Stash.", color = LockrColors.TextSecondary)
                        Button(onClick = { actions.delete(setOf(item.id)); menuItem = null; menuPage = "actions" },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth()) { Text("Delete item") }
                        TextButton(onClick = { menuPage = "actions" }, modifier = Modifier.fillMaxWidth()) { Text("Keep item") }
                    }
                    else -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HomeMenuAction(Icons.Outlined.Info, "Info", Modifier.weight(1f)) { menuItem = null; onOpenDetails(item) }
                        HomeMenuAction(Icons.Outlined.Edit, "Rename", Modifier.weight(1f)) { renameText = item.title; menuPage = "rename" }
                        HomeMenuAction(Icons.Outlined.Download, "Download", Modifier.weight(1f),
                            item.category == VaultCategory.CARD || item.attachments.isNotEmpty()) {
                            menuItem = null
                            if (item.category == VaultCategory.CARD) {
                                cardDownloadItem = item
                                cardDownloadLauncher.launch(item.title.replace(Regex("[^A-Za-z0-9._-]"), "_") + ".txt")
                            } else if (item.category == VaultCategory.DOCUMENT) actions.download(listOf(item))
                            else { downloadItem = item; downloadLauncher.launch(item.attachments.first().name) }
                        }
                        HomeMenuAction(Icons.Outlined.DeleteOutline, "Delete", Modifier.weight(1f)) { menuPage = "delete" }
                    }
                }
            }
        }
    }
    BoxWithConstraints(modifier.fillMaxSize().padding(horizontal = 17.dp)) {
        val recentLimit = if (maxHeight < 1000.dp) 3 else 4
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 96.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
            DashboardHero()
            Row(Modifier.fillMaxWidth().padding(horizontal = 3.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Recent Items", style = MaterialTheme.typography.titleMedium,
                    color = LockrColors.TextPrimary, modifier = Modifier.weight(1f))
                Text("See All", style = MaterialTheme.typography.labelMedium, color = LockrColors.Accent,
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
                items.take(recentLimit).forEach { item ->
                    RecentItemRow(item, onClick = { onOpenItem(item) }, onMore = { menuPage = "actions"; menuItem = item })
                }
            }
            Spacer(Modifier.height(120.dp))
        }
        Row(Modifier.align(Alignment.TopCenter).fillMaxWidth().height(84.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.weight(1f).clip(RoundedCornerShape(25.dp)).clickable(onClick = onOpenProfile)
                .padding(horizontal = 2.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                ProfileAvatar(profilePhoto, 46.dp)
                Text(profileName, style = MaterialTheme.typography.titleLarge, color = LockrColors.TextPrimary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(start = 10.dp, end = 6.dp))
            }
            FloatingGlass(Modifier.size(48.dp), RoundedCornerShape(24.dp)) {
                IconButton(onClick = onOpenSettings, modifier = Modifier.fillMaxSize()) { Icon(Icons.Outlined.Settings, "Settings", tint = LockrColors.TextPrimary) }
            }
        }

    }
}

@Composable
private fun HomeMenuAction(icon: ImageVector, label: String, modifier: Modifier = Modifier,
                           enabled: Boolean = true, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(modifier.height(78.dp).clip(shape)
        .background(if (enabled) LockrColors.Raised.copy(alpha = .65f) else LockrColors.Raised.copy(alpha = .25f))
        .clickable(enabled = enabled, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(icon, label, tint = if (label == "Delete") MaterialTheme.colorScheme.error else LockrColors.Accent,
            modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(5.dp))
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = if (enabled) LockrColors.TextPrimary else LockrColors.TextSecondary)
    }
}

@Composable
private fun DashboardHero() {
    val shape = RoundedCornerShape(30.dp)
    Box(
        Modifier.fillMaxWidth().height(237.dp).shadow(16.dp, shape, ambientColor = Color(0x504D9ABC))
            .clip(shape)
            .background(Brush.linearGradient(if (LocalDarkTheme.current) listOf(LockrColors.Surface, LockrColors.Raised) else listOf(Color(0xFFEBDFFB), Color(0xFFDCF8F9), Color(0xFFFFEAD8))))
            .border(1.dp, Color.White.copy(alpha = 0.94f), shape)
    ) {
        Image(painterResource(R.drawable.hero_vault_cutout), contentDescription = "Vault with cards and documents",
            contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(
            if (LocalDarkTheme.current) listOf(Color(0xEE112033), Color(0x88112033), Color.Transparent) else listOf(Color(0xCFF7F5FF), Color(0x70F7F5FF), Color.Transparent), endX = 630f)))
        Text("Your\nSecure Space", style = MaterialTheme.typography.headlineSmall,
            color = LockrColors.TextPrimary, modifier = Modifier.align(Alignment.TopStart).padding(start = 24.dp, top = 29.dp))
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
            Box(Modifier.size(34.dp).clip(CircleShape).background(LockrColors.Surface.copy(alpha = 0.8f))
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
            CardArtBackground(cardArtStyle(item), Modifier.fillMaxSize())
            customCardArt(item)?.let { CustomCardArtBackground(it, Modifier.fillMaxSize(), animate = false) }
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

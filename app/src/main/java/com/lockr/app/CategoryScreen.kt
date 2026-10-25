package com.lockr.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun CategoryScreen(
    screen: LockrScreen,
    items: List<VaultItem>,
    revealTimeout: String,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isAllItems = screen == LockrScreen.ALL_ITEMS
    val category = when (screen) {
        LockrScreen.DOCUMENTS -> VaultCategory.DOCUMENT
        LockrScreen.PASSWORDS -> VaultCategory.PASSWORD
        LockrScreen.NOTES -> VaultCategory.NOTE
        else -> null
    }
    val title = when (screen) {
        LockrScreen.ALL_ITEMS -> "All Items"
        LockrScreen.DOCUMENTS -> "Documents"
        LockrScreen.PASSWORDS -> "Passwords"
        else -> "Notes"
    }
    val icon = when (screen) {
        LockrScreen.DOCUMENTS -> Icons.Outlined.Description
        LockrScreen.PASSWORDS -> Icons.Outlined.Key
        LockrScreen.NOTES -> Icons.AutoMirrored.Outlined.Article
        else -> Icons.Outlined.Lock
    }
    var selectedFilter by remember(screen) { mutableStateOf("All") }
    var selected by remember { mutableStateOf<VaultItem?>(null) }
    val filters = listOf("All", "Cards", "Documents", "Passwords", "Notes")
    val filteredItems = if (!isAllItems || selectedFilter == "All") {
        if (category == null) items else items.filter { it.category == category }
    } else {
        val filterCategory = when (selectedFilter) {
            "Cards" -> VaultCategory.CARD
            "Documents" -> VaultCategory.DOCUMENT
            "Passwords" -> VaultCategory.PASSWORD
            "Notes" -> VaultCategory.NOTE
            else -> null
        }
        items.filter { it.category == filterCategory }
    }

    Column(modifier.fillMaxSize().padding(horizontal = 22.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(42.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.77f))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = LockrColors.TextPrimary)
            }
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Text(title, style = MaterialTheme.typography.headlineMedium)
                Text("Stored privately on this device", color = LockrColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
            Box(
                Modifier.size(43.dp).clip(CircleShape).clickable(onClick = onAdd),
                contentAlignment = Alignment.Center
            ) {
                IridescentOrb(orbSize = 42.dp)
                Icon(Icons.Outlined.Add, contentDescription = "Add $title", tint = LockrColors.TextPrimary)
            }
        }

        if (isAllItems) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                filters.forEach { filter ->
                    FilterChip(selected = selectedFilter == filter, onClick = { selectedFilter = filter }, label = { Text(filter) })
                }
            }
        }

        if (filteredItems.isEmpty()) {
            EmptyCategoryState(
                title = if (items.isEmpty()) "Your vault is empty" else "Nothing in this section yet",
                modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 18.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                items(filteredItems, key = { "${it.category}-${it.title}" }) { item ->
                    CategoryItemRow(item, if (isAllItems) categoryIcon(item.category) else icon, onClick = { selected = item })
                }
            }
        }
    }

    selected?.let { item -> VaultItemDetailsDialog(item = item, revealTimeout = revealTimeout, onClose = { selected = null }) }
}

@Composable
private fun VaultItemDetailsDialog(item: VaultItem, revealTimeout: String, onClose: () -> Unit) {
    var revealPrivateValues by remember(item.title) { mutableStateOf(false) }
    LaunchedEffect(revealPrivateValues, revealTimeout) {
        if (revealPrivateValues) {
            val timeoutMillis = when {
                revealTimeout.startsWith("10") -> 10_000L
                revealTimeout.startsWith("1 minute") -> 60_000L
                revealTimeout.startsWith("5 minutes") -> 300_000L
                else -> 30_000L
            }
            delay(timeoutMillis)
            revealPrivateValues = false
        }
    }
    val fields = when (item.category) {
        VaultCategory.DOCUMENT -> listOfNotNull(
            item.detail.takeIf { it.isNotBlank() }?.let { "Document type" to it },
            item.metadata["documentNumber"]?.takeIf { it.isNotBlank() }?.let { "Document number" to it },
            item.metadata["holderName"]?.takeIf { it.isNotBlank() }?.let { "Name on document" to it },
            item.metadata["authority"]?.takeIf { it.isNotBlank() }?.let { "Issuing authority" to it },
            item.metadata["issueDate"]?.takeIf { it.isNotBlank() }?.let { "Issued" to it },
            item.metadata["expiry"]?.takeIf { it.isNotBlank() }?.let { "Expires" to it },
            item.metadata["notes"]?.takeIf { it.isNotBlank() }?.let { "Notes" to it }
        )
        VaultCategory.PASSWORD -> listOfNotNull(
            item.metadata["website"]?.takeIf { it.isNotBlank() }?.let { "Website or app" to it },
            item.metadata["username"]?.takeIf { it.isNotBlank() }?.let { "Username" to it },
            item.metadata["password"]?.takeIf { it.isNotBlank() }?.let { "Password" to it },
            item.metadata["notes"]?.takeIf { it.isNotBlank() }?.let { "Notes" to it }
        )
        VaultCategory.NOTE -> listOfNotNull(item.metadata["content"]?.takeIf { it.isNotBlank() }?.let { "Note" to it })
        VaultCategory.CARD -> emptyList()
    }
    val privateLabels = setOf("Document number", "Password")

    AlertDialog(
        onDismissRequest = onClose,
        title = {
            Column {
                Text(item.title)
                Text("${item.category.label} · ${item.detail} · ${item.time}", color = LockrColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
        },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                if (fields.isEmpty()) {
                    Text("No extra details were saved for this item.", color = LockrColors.TextSecondary)
                } else {
                    if (fields.any { it.first in privateLabels }) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("Private fields are hidden", color = LockrColors.TextSecondary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            IconButton(onClick = { revealPrivateValues = !revealPrivateValues }) {
                                Icon(
                                    if (revealPrivateValues) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = if (revealPrivateValues) "Hide private fields" else "Reveal private fields"
                                )
                            }
                        }
                    }
                    fields.forEach { (label, value) ->
                        val displayValue = if (label in privateLabels && !revealPrivateValues) {
                            "••••••••${value.takeLast(4)}"
                        } else value
                        Text(label, color = LockrColors.TextSecondary, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 10.dp))
                        Text(displayValue, color = LockrColors.TextPrimary, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                if (item.attachments.isNotEmpty()) {
                    VaultAttachmentsSection(item.attachments, Modifier.fillMaxWidth().padding(top = 15.dp))
                }
                Text("Saved ${item.time}. Encrypted on this device.", color = LockrColors.TextSecondary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 15.dp))
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("Close") } }
    )
}

private fun categoryIcon(category: VaultCategory): ImageVector = when (category) {
    VaultCategory.CARD -> Icons.Outlined.CreditCard
    VaultCategory.DOCUMENT -> Icons.Outlined.Description
    VaultCategory.PASSWORD -> Icons.Outlined.Key
    VaultCategory.NOTE -> Icons.AutoMirrored.Outlined.Article
}

@Composable
private fun EmptyCategoryState(title: String, modifier: Modifier = Modifier) {
    GlassPanel(modifier, radius = 25.dp) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(Modifier.size(58.dp).clip(CircleShape).background(Color(0xFFDFF2F5)), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Lock, contentDescription = null, tint = Color(0xFF498D9A), modifier = Modifier.size(28.dp))
            }
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
            Text("Items stay encrypted on this device.", color = LockrColors.TextSecondary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 5.dp))
        }
    }
}

@Composable
private fun CategoryItemRow(item: VaultItem, icon: ImageVector, onClick: () -> Unit) {
    GlassPanel(Modifier.fillMaxWidth().clip(androidx.compose.foundation.shape.RoundedCornerShape(19.dp)).clickable(onClick = onClick), radius = 19.dp) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(45.dp).clip(CircleShape).background(Color(0xDCE3F0FC)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = Color(0xFF598BB4), modifier = Modifier.size(23.dp))
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium)
                Text("${item.category.label}  •  ${item.time}", color = LockrColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, tint = LockrColors.TextSecondary, modifier = Modifier.size(18.dp).rotate(180f))
        }
    }
}

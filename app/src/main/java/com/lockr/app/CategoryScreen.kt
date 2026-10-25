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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun CategoryScreen(
    screen: LockrScreen,
    items: List<VaultItem>,
    onBack: () -> Unit,
    onOpenItem: (VaultItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val all = screen == LockrScreen.ALL_ITEMS
    val category = when (screen) {
        LockrScreen.CARDS -> VaultCategory.CARD
        LockrScreen.DOCUMENTS -> VaultCategory.DOCUMENT
        LockrScreen.PASSWORDS -> VaultCategory.PASSWORD
        LockrScreen.NOTES -> VaultCategory.NOTE
        else -> null
    }
    val title = when (screen) {
        LockrScreen.CARDS -> "Cards"
        LockrScreen.DOCUMENTS -> "Documents"
        LockrScreen.PASSWORDS -> "Passwords"
        LockrScreen.NOTES -> "Notes"
        else -> "All items"
    }
    val filters = if (screen == LockrScreen.DOCUMENTS) listOf("All", "Identity", "Medical", "Bills", "Insurance", "Other") else listOf("All", "Cards", "Documents", "Passwords", "Notes")
    var selectedFilter by rememberSaveable(screen) { mutableStateOf("All") }
    var query by rememberSaveable(screen) { mutableStateOf("") }
    val visibleItems = items.filter { item ->
        val matchesCategory = category == null || item.category == category
        val filterCategory = when (selectedFilter) {
            "Cards" -> VaultCategory.CARD
            "Documents" -> VaultCategory.DOCUMENT
            "Passwords" -> VaultCategory.PASSWORD
            "Notes" -> VaultCategory.NOTE
            else -> null
        }
        val matchesFilter = if (screen == LockrScreen.DOCUMENTS && selectedFilter != "All") {
            item.metadata["documentCategory"].equals(selectedFilter, ignoreCase = true) ||
                (selectedFilter == "Other" && item.metadata["documentCategory"].isNullOrBlank())
        } else filterCategory == null || item.category == filterCategory
        matchesCategory && matchesFilter && searchableText(item).contains(query, ignoreCase = true)
    }

    Column(modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        StashTopBar(title, "${visibleItems.size} saved", onBack = onBack, modifier = Modifier.padding(top = 3.dp))
        androidx.compose.material3.OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search $title") },
            singleLine = true,
            shape = RoundedCornerShape(17.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 5.dp, bottom = 9.dp)
        )
        if (all || screen == LockrScreen.DOCUMENTS) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                filters.forEach { filter ->
                    FilterChip(selected = selectedFilter == filter, onClick = { selectedFilter = filter }, label = { Text(filter) })
                }
            }
        }
        if (visibleItems.isEmpty()) {
            EmptyCategoryState(
                title = when {
                    items.isEmpty() -> "Nothing here yet"
                    query.isNotBlank() -> "No matches found"
                    else -> "No ${title.lowercase()} yet"
                },
                subtitle = if (items.isEmpty()) "Use Add below to import your first item." else "Try another search or filter.",
                modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 12.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).padding(top = 10.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                items(visibleItems, key = { it.id }) { item ->
                    CategoryItemRow(item, categoryIcon(item.category), onClick = { onOpenItem(item) })
                }
            }
        }
    }
}

fun searchableText(item: VaultItem): String = buildString {
    append(item.title).append(' ')
    append(item.category.label).append(' ')
    when (item.category) {
        VaultCategory.PASSWORD -> Unit // Never search or expose password field values.
        VaultCategory.NOTE -> append(item.metadata["content"].orEmpty()).append(' ').append(item.metadata["ocrText"].orEmpty()).append(' ')
        else -> item.metadata["ocrText"]?.let { append(it).append(' ') }
    }
}

@Composable
private fun EmptyCategoryState(title: String, subtitle: String, modifier: Modifier = Modifier) {
    GlassPanel(modifier, radius = 23.dp) {
        Column(
            Modifier.fillMaxSize().padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(Modifier.size(54.dp).clip(CircleShape).background(Color(0xFFDFF2F5)), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Description, contentDescription = null, tint = Color(0xFF498D9A), modifier = Modifier.size(26.dp))
            }
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 10.dp))
            Text(subtitle, color = LockrColors.TextSecondary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun CategoryItemRow(item: VaultItem, icon: ImageVector, onClick: () -> Unit) {
    GlassPanel(Modifier.fillMaxWidth(), radius = 19.dp) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(19.dp)).clickable(onClick = onClick).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(45.dp).clip(CircleShape).background(Color(0xDCE3F0FC)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = Color(0xFF598BB4), modifier = Modifier.size(23.dp))
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(item.title, style = MaterialTheme.typography.titleSmall, color = LockrColors.TextPrimary, maxLines = 1)
                Text("${item.category.label} · ${item.time}", color = LockrColors.TextSecondary, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
            Icon(Icons.AutoMirrored.Outlined.ArrowForwardIos, contentDescription = "Open ${item.title}", tint = LockrColors.TextSecondary, modifier = Modifier.size(16.dp))
        }
    }
}

private fun categoryIcon(category: VaultCategory): ImageVector = when (category) {
    VaultCategory.CARD -> Icons.Outlined.CreditCard
    VaultCategory.DOCUMENT -> Icons.Outlined.Description
    VaultCategory.PASSWORD -> Icons.Outlined.Key
    VaultCategory.NOTE -> Icons.AutoMirrored.Outlined.Article
}

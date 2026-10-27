package com.lockr.app

import androidx.compose.foundation.layout.Box

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun VaultSearchScreen(items: List<VaultItem>, onBack: () -> Unit, onOpenItem: (VaultItem) -> Unit, modifier: Modifier = Modifier) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("All") }
    val category = when (filter) {
        "Cards" -> VaultCategory.CARD
        "Documents" -> VaultCategory.DOCUMENT
        "Passwords" -> VaultCategory.PASSWORD
        "Notes" -> VaultCategory.NOTE
        else -> null
    }
    val matches = items.filter { item ->
        (category == null || item.category == category) && (query.isBlank() || searchableText(item).contains(query, ignoreCase = true))
    }
    Box(modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Column(Modifier.fillMaxSize().padding(top = 88.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                placeholder = { Text("Search your vault") },
                singleLine = true,
                shape = RoundedCornerShape(17.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 5.dp)
            )
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf("All", "Cards", "Documents", "Passwords", "Notes").forEach { option ->
                    FilterChip(selected = filter == option, onClick = { filter = option }, label = { Text(option) })
                }
            }
            if (matches.isEmpty()) {
                EmptyCategoryStateForSearch(if (query.isBlank()) "Your search starts here" else "No matches", if (query.isBlank()) "Search stays on this device." else "Try a different word or category.", Modifier.weight(1f).fillMaxWidth().padding(vertical = 16.dp))
            } else {
                LazyColumn(Modifier.weight(1f).padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    items(matches, key = { it.id }) { item ->
                        GlassPanel(Modifier.fillMaxWidth(), radius = 19.dp) {
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(19.dp)).clickable { onOpenItem(item) }.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(item.title, style = MaterialTheme.typography.titleSmall, color = LockrColors.TextPrimary)
                                    Text(item.category.label, style = MaterialTheme.typography.bodySmall, color = LockrColors.TextSecondary, modifier = Modifier.padding(top = 2.dp))
                                    val snippet = if (item.category == VaultCategory.NOTE) item.metadata["content"] ?: item.metadata["ocrText"] else item.metadata["ocrText"]
                                    if (!snippet.isNullOrBlank() && query.isNotBlank()) {
                                        Text(snippet.lineSequence().firstOrNull { it.contains(query, true) }?.take(100).orEmpty(), style = MaterialTheme.typography.bodySmall, color = LockrColors.TextSecondary, maxLines = 1, modifier = Modifier.padding(top = 3.dp))
                                    }
                                }
                                Icon(Icons.AutoMirrored.Outlined.ArrowForwardIos, contentDescription = "Open ${item.title}", tint = LockrColors.TextSecondary)
                            }
                        }
                    }
                }
            }

        }
        StashTopBar("Search", "Search titles and document text", onBack = onBack, modifier = Modifier.padding(top = 3.dp))
    }
}

@Composable
private fun EmptyCategoryStateForSearch(title: String, subtitle: String, modifier: Modifier = Modifier) {
    GlassPanel(modifier, radius = 23.dp) {
        Column(Modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = LockrColors.TextSecondary, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

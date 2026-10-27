package com.lockr.app

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun CategoryScreen(
    screen: LockrScreen,
    items: List<VaultItem>,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onOpenItem: (VaultItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val category = when (screen) {
        LockrScreen.DOCUMENTS -> VaultCategory.DOCUMENT
        LockrScreen.PASSWORDS -> VaultCategory.PASSWORD
        LockrScreen.NOTES -> VaultCategory.NOTE
        else -> null
    }
    val title = when (screen) {
        LockrScreen.DOCUMENTS -> "Documents"
        LockrScreen.PASSWORDS -> "Passwords"
        LockrScreen.NOTES -> "Notes"
        else -> "All items"
    }
    val catalog = LocalTagCatalog.current
    val filters = if (screen == LockrScreen.DOCUMENTS) listOf("All") + catalog.groups()
        else listOf("All", "Cards", "Documents", "Passwords", "Notes")
    var selectedFilter by rememberSaveable(screen) { mutableStateOf("All") }
    androidx.compose.runtime.LaunchedEffect(filters) { if (selectedFilter !in filters) selectedFilter = "All" }
    val categoryItems = items.filter { category == null || it.category == category }
    val visibleItems = categoryItems.filter { item ->
        val matchesFilter = if (screen == LockrScreen.DOCUMENTS && selectedFilter != "All") {
            item.metadata["documentCategory"].equals(selectedFilter, ignoreCase = true) ||
                (selectedFilter == "Other" && item.metadata["documentCategory"].isNullOrBlank())
        } else when (selectedFilter) {
            "Cards" -> item.category == VaultCategory.CARD
            "Documents" -> item.category == VaultCategory.DOCUMENT
            "Passwords" -> item.category == VaultCategory.PASSWORD
            "Notes" -> item.category == VaultCategory.NOTE
            else -> true
        }
        matchesFilter
    }

    Column(modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        StashTopBar(title, "${categoryItems.size} saved", onBack = onBack,
            trailing = { IconButton(onClick = onSearch) { Icon(Icons.Outlined.Search, contentDescription = "Search vault") } })
        if (screen == LockrScreen.ALL_ITEMS || screen == LockrScreen.DOCUMENTS) {
            CollectionFilters(filters, selectedFilter, { selectedFilter = it },
                if (screen == LockrScreen.DOCUMENTS) VaultCategory.DOCUMENT else null)
        }

        if (visibleItems.isEmpty()) {
            CollectionEmptyState(if (categoryItems.isEmpty()) "Your ${title.lowercase()} will live here" else "No items in this filter",
                if (categoryItems.isEmpty()) "Tap + to add your first item." else "Choose another tag.", Icons.Outlined.Description)
        } else {
            LazyColumn(Modifier.weight(1f).padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(visibleItems, key = { it.id }) { item -> VaultVisualCard(item, onClick = { onOpenItem(item) }) }
            }
        }
    }
}

fun searchableText(item: VaultItem): String = buildString {
    append(item.title).append(' ')
    append(item.category.label).append(' ')
    append(item.metadata["documentCategory"].orEmpty()).append(' ')
    if (item.category == VaultCategory.CARD) {
        append(cardTags(item).joinToString(" ")).append(' ')
        listOf("issuer", "partner", "cardholder", "cardType", "network").forEach { append(item.metadata[it].orEmpty()).append(' ') }
    }
    when (item.category) {
        VaultCategory.PASSWORD -> Unit // Never search or expose password field values.
        VaultCategory.NOTE -> append(item.metadata["content"].orEmpty()).append(' ').append(item.metadata["ocrText"].orEmpty()).append(' ')
        else -> item.metadata["ocrText"]?.let { append(it).append(' ') }
    }
}

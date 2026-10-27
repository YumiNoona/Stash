package com.lockr.app

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.outlined.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.BackHandler
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

@OptIn(ExperimentalFoundationApi::class)
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
    val actions = LocalVaultActions.current
    val folders = LocalVaultFolders.current
    val prefs = LocalContext.current.getSharedPreferences("lockr_preferences", 0)
    var folder by rememberSaveable(screen) { mutableStateOf(prefs.getString("last_folder", "All folders") ?: "All folders") }
    var selection by remember(screen) { mutableStateOf(VaultSelection()) }
    val selected = selection.ids
    var moving by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var managing by remember { mutableStateOf(false) }
    var folderToEdit by remember { mutableStateOf("") }
    var newName by remember { mutableStateOf("__closed__") }
    var descending by rememberSaveable { mutableStateOf(true) }
    BackHandler(selection.active) { selection = VaultSelection() }
    LaunchedEffect(folders) { if (folder !in folders && folder !in listOf("All folders", "Unfiled")) { folder = "All folders"; prefs.edit().putString("last_folder", folder).apply() } }
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
        matchesFilter && (screen != LockrScreen.DOCUMENTS || folder == "All folders" ||
            item.metadata["folder"].orEmpty() == if (folder == "Unfiled") "" else folder)
    }.let { if (descending) it.sortedByDescending { item -> item.createdAt } else it.sortedBy { item -> item.title.lowercase() } }

    val visibleIds = visibleItems.map { it.id }.toSet()
    LaunchedEffect(visibleIds) { selection = selection.reconcile(visibleIds) }
    Box(modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 88.dp, bottom = 124.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (screen == LockrScreen.ALL_ITEMS || screen == LockrScreen.DOCUMENTS) item {
                CollectionFilters(filters, selectedFilter, { selectedFilter = it },
                    if (screen == LockrScreen.DOCUMENTS) VaultCategory.DOCUMENT else null)
            }
            if (screen == LockrScreen.DOCUMENTS) item {
                Column {
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (listOf("All folders", "Unfiled") + folders).distinct().forEach { f ->
                            FilterChip(folder == f, { folder = f; prefs.edit().putString("last_folder", f).apply() }, label = { Text(f) })
                        }
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { managing = true }) { Text("Folders") }
                        TextButton(onClick = { descending = !descending }) { Text(if (descending) "Newest first" else "Name A–Z") }
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { selection = if (selection.active) VaultSelection() else selection.enter() }) {
                            Icon(Icons.Outlined.Checklist, if (selection.active) "Exit selection" else "Select documents", tint = LockrColors.Accent)
                        }
                    }
                }
            }
            if (visibleItems.isEmpty()) item {
                CollectionEmptyState(if (categoryItems.isEmpty()) "Your ${title.lowercase()} will live here" else "No items in this filter",
                    if (categoryItems.isEmpty()) "Tap + to add your first item." else "Choose another folder or tag.", Icons.Outlined.Description)
            }
            items(visibleItems, key = { it.id }) { item ->
                val selectedItem = item.id in selected
                Box(Modifier.fillMaxWidth().then(if (selectedItem) Modifier.border(2.dp, LockrColors.Accent, RoundedCornerShape(23.dp)) else Modifier)) {
                    VaultVisualCard(item, onClick = { if (selection.active) selection = selection.toggle(item.id) else onOpenItem(item) },
                        onLongClick = { selection = selection.toggle(item.id) })
                    if (selection.active) FloatingGlass(Modifier.align(Alignment.TopStart).padding(5.dp).size(36.dp), RoundedCornerShape(18.dp)) {
                        Checkbox(selectedItem, { selection = selection.toggle(item.id) }, modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
        if (selection.active) {
            FloatingGlass(Modifier.align(Alignment.TopCenter).padding(top = 8.dp).fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { selection = VaultSelection() }, modifier = Modifier.size(40.dp)) { Icon(Icons.Outlined.Close, "Close selection") }
                    Text("${selected.size} selected", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                    IconButton(onClick = { selection = selection.all(visibleIds) }, enabled = visibleIds.isNotEmpty(), modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Outlined.SelectAll, if (selected.containsAll(visibleIds)) "Deselect all" else "Select all")
                    }
                    IconButton(enabled = selected.isNotEmpty() && items.filter { it.id in selected }.all { it.category == VaultCategory.DOCUMENT }, onClick = { moving = true }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Outlined.DriveFileMove, "Move selected documents")
                    }
                    IconButton(enabled = items.any { it.id in selected && it.category == VaultCategory.DOCUMENT && it.attachments.isNotEmpty() }, onClick = { actions.download(items.filter { it.id in selected }) }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Outlined.Download, "Download selected documents")
                    }
                    IconButton(enabled = selected.isNotEmpty(), onClick = { deleting = true }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Outlined.DeleteOutline, "Delete selected items", tint = if (selected.isNotEmpty()) MaterialTheme.colorScheme.error else LockrColors.TextSecondary)
                    }
                }
            }
        } else StashTopBar(title, "${categoryItems.size} saved", onBack = onBack,
            trailing = { IconButton(onClick = onSearch) { Icon(Icons.Outlined.Search, "Search vault") } })
    }
    if (moving) FolderPicker(folders, { moving = false }) { destination -> actions.move(selected, destination); moving = false; selection = VaultSelection() }
    if (deleting) AlertDialog(onDismissRequest = { deleting = false }, title = { Text("Delete ${selected.size} items?") }, text = { Text("The selected items and their files will be removed.") }, confirmButton = { TextButton(onClick = { actions.delete(selected); selection = VaultSelection(); deleting = false }) { Text("Delete") } }, dismissButton = { TextButton(onClick = { deleting = false }) { Text("Cancel") } })
    if (managing) AlertDialog(onDismissRequest = { managing = false }, title = { Text("Folders") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            folders.forEach { f -> Row(verticalAlignment = Alignment.CenterVertically) {
                Text(f, modifier = Modifier.weight(1f))
                TextButton(onClick = { folderToEdit = f; newName = f; managing = false }) { Text("Edit") }
            } }
            TextButton(onClick = { folderToEdit = ""; newName = ""; managing = false }) { Text("Create folder") }
        }
    }, confirmButton = { TextButton(onClick = { managing = false }) { Text("Done") } })
    if (!managing && newName != "__closed__" && (folderToEdit.isNotBlank() || newName == "")) {
        AlertDialog(onDismissRequest = { newName = "__closed__"; folderToEdit = "" }, title = { Text(if (folderToEdit.isBlank()) "Create folder" else "Edit folder") }, text = { Column {
            OutlinedTextField(newName, { newName = it.take(40) }, label = { Text("Folder name") }, singleLine = true)
            if (folderToEdit.isNotBlank()) TextButton(onClick = { actions.folderChanged(folderToEdit, null); folderToEdit = ""; newName = "__closed__" }) { Text("Delete folder · keep documents in Unfiled") }
        } }, confirmButton = { TextButton(enabled = newName.isNotBlank() && newName !in listOf("All folders", "Unfiled", "__closed__"), onClick = { actions.folderChanged(folderToEdit, newName.trim()); folderToEdit = ""; newName = "__closed__" }) { Text("Save") } }, dismissButton = { TextButton(onClick = { folderToEdit = ""; newName = "__closed__" }) { Text("Cancel") } })
    }
}

fun searchableText(item: VaultItem): String = buildString {
    append(item.title).append(' ')
    append(item.metadata["folder"].orEmpty()).append(' ')
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

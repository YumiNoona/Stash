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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** One place to organize labels and document folders without adding controls to browse filters. */
@Composable
fun OrganizationScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val catalog = LocalTagCatalog.current
    val folders = LocalVaultFolders.current
    val actions = LocalVaultActions.current
    var renameFolder by remember { mutableStateOf<String?>(null) }
    var folderToDelete by remember { mutableStateOf<String?>(null) }
    var folderName by remember { mutableStateOf("") }
    var folderError by remember { mutableStateOf("") }

    Column(modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        StashTopBar("Tags & folders", onBack = onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            OrganizationTagSection(
                title = "Card tags",
                caption = "Label cards in your own way",
                category = VaultCategory.CARD,
                iconTint = Color(0xFF9471DA)
            )
            OrganizationTagSection(
                title = "Document tags",
                caption = "Group the documents you save",
                category = VaultCategory.DOCUMENT,
                iconTint = Color(0xFF4F9DC7)
            )
            OrganizationSection("Folders", "Keep documents together", Icons.Outlined.Folder, Color(0xFF38A896)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = folderName,
                        onValueChange = { folderName = it.take(40); folderError = "" },
                        label = { Text("New folder") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = {
                        val next = folderName.trim().replace(Regex("\\s+"), " ")
                        folderError = when {
                            next.isEmpty() -> "Enter a folder name."
                            next.equals("Unfiled", true) || next.equals("All folders", true) -> "Choose another name."
                            folders.any { it.equals(next, true) } -> "That folder already exists."
                            else -> ""
                        }
                        if (folderError.isEmpty()) {
                            actions.folderChanged("", next)
                            folderName = ""
                        }
                    }) { Icon(Icons.Outlined.Add, "Create folder", tint = LockrColors.Accent) }
                }
                if (folderError.isNotBlank()) Text(folderError, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                if (folders.isEmpty()) Text("No folders yet", style = MaterialTheme.typography.bodySmall,
                    color = LockrColors.TextSecondary, modifier = Modifier.padding(vertical = 16.dp))
                folders.forEachIndexed { index, folder ->
                    if (index == 0) HorizontalDivider(color = LockrColors.Line, modifier = Modifier.padding(top = 14.dp))
                    Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Folder, null, tint = Color(0xFF38A896), modifier = Modifier.size(20.dp))
                        Text(folder, style = MaterialTheme.typography.bodyMedium,
                            color = LockrColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(start = 12.dp))
                        IconButton(onClick = { renameFolder = folder }) {
                            Icon(Icons.Outlined.Edit, "Rename $folder", tint = LockrColors.TextSecondary,
                                modifier = Modifier.size(19.dp))
                        }
                        IconButton(onClick = { folderToDelete = folder }) {
                            Icon(Icons.Outlined.DeleteOutline, "Delete $folder", tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(19.dp))
                        }
                    }
                    HorizontalDivider(color = LockrColors.Line)
                }
            }
        }
    }

    renameFolder?.let { original ->
        var edited by remember(original) { mutableStateOf(original) }
        var error by remember(original) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { renameFolder = null },
            title = { Text("Rename folder") },
            text = {
                Column {
                    OutlinedTextField(edited, { edited = it.take(40); error = "" },
                        label = { Text("Folder name") }, singleLine = true)
                    if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                }
            },
            confirmButton = { TextButton(onClick = {
                val next = edited.trim().replace(Regex("\\s+"), " ")
                error = when {
                    next.isEmpty() -> "Enter a folder name."
                    next.equals("Unfiled", true) || next.equals("All folders", true) -> "Choose another name."
                    folders.any { it != original && it.equals(next, true) } -> "That folder already exists."
                    else -> ""
                }
                if (error.isEmpty()) {
                    if (next != original) actions.folderChanged(original, next)
                    renameFolder = null
                }
            }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { renameFolder = null }) { Text("Cancel") } }
        )
    }
    folderToDelete?.let { folder ->
        AlertDialog(
            onDismissRequest = { folderToDelete = null },
            title = { Text("Delete $folder?") },
            text = { Text("Documents in this folder will move to Unfiled. No files will be deleted.") },
            confirmButton = { TextButton(onClick = {
                actions.folderChanged(folder, null)
                folderToDelete = null
            }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { folderToDelete = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun OrganizationTagSection(title: String, caption: String, category: VaultCategory, iconTint: Color) {
    val catalog = LocalTagCatalog.current
    val labels = catalog.custom(category)
    var name by remember(category) { mutableStateOf("") }
    var error by remember(category) { mutableStateOf("") }
    var pendingDelete by remember(category) { mutableStateOf<String?>(null) }
    OrganizationSection(title, caption, Icons.Outlined.Label, iconTint) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(name, { name = it.take(24); error = "" },
                label = { Text("New tag") }, singleLine = true, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = {
                if (catalog.add(category, name)) {
                    name = ""
                    error = ""
                } else error = if (name.isBlank()) "Enter a tag name." else "Use a unique tag name."
            }) { Icon(Icons.Outlined.Add, "Create $title tag", tint = LockrColors.Accent) }
        }
        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
        if (labels.isEmpty()) Text("No custom tags yet", style = MaterialTheme.typography.bodySmall,
            color = LockrColors.TextSecondary, modifier = Modifier.padding(vertical = 16.dp))
        labels.forEachIndexed { index, label ->
            if (index == 0) HorizontalDivider(color = LockrColors.Line, modifier = Modifier.padding(top = 14.dp))
            Row(Modifier.fillMaxWidth().height(54.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(28.dp).background(iconTint.copy(alpha = 0.16f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Label, null, tint = iconTint, modifier = Modifier.size(17.dp))
                }
                Text(label, style = MaterialTheme.typography.bodyMedium, color = LockrColors.TextPrimary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(start = 12.dp))
                IconButton(enabled = !catalog.deleting, onClick = { pendingDelete = label }) {
                    Icon(Icons.Outlined.DeleteOutline, "Delete $label", tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(19.dp))
                }
            }
            HorizontalDivider(color = LockrColors.Line)
        }
    }
    pendingDelete?.let { label ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete $label?") },
            text = { Text("This tag will be removed from saved items. The items themselves will stay in your vault.") },
            confirmButton = { TextButton(onClick = {
                catalog.onDelete(category, label)
                pendingDelete = null
            }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun OrganizationSection(
    title: String,
    caption: String,
    icon: ImageVector,
    tint: Color,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).background(tint.copy(alpha = 0.17f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.padding(start = 11.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = LockrColors.TextPrimary)
                Text(caption, style = MaterialTheme.typography.bodySmall, color = LockrColors.TextSecondary)
            }
        }
        GlassPanel(Modifier.fillMaxWidth(), radius = 24.dp) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) { content() }
        }
    }
}

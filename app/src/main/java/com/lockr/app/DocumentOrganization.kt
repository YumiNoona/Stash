package com.lockr.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.RotateRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Base64
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Names are unique within a category/folder and preserve the original extension. */
fun uniqueVaultTitle(requested: String, occupied: Collection<String>): String {
    val name = requested.trim().ifBlank { "Untitled" }
    val used = occupied.map { it.lowercase(java.util.Locale.ROOT) }.toSet()
    if (name.lowercase(java.util.Locale.ROOT) !in used) return name
    val dot = name.lastIndexOf('.').takeIf { it > 0 } ?: name.length
    val stem = name.substring(0, dot).replace(Regex(""" \(\d+\)$"""), "")
    val extension = name.substring(dot)
    var n = 1
    while ("$stem ($n)$extension".lowercase(java.util.Locale.ROOT) in used) n++
    return "$stem ($n)$extension"
}

fun uniqueItem(item: VaultItem, others: List<VaultItem>): VaultItem = item.copy(title = uniqueVaultTitle(item.title,
    others.filter { it.id != item.id && it.category == item.category && it.metadata["folder"].orEmpty() == item.metadata["folder"].orEmpty() }.map { it.title }))

class VaultActions(val move: (Set<String>, String) -> Unit = { _, _ -> },
    val delete: (Set<String>) -> Unit = {}, val download: (List<VaultItem>) -> Unit = {},
    val folderChanged: (String, String?) -> Unit = { _, _ -> })
val LocalVaultFolders = staticCompositionLocalOf { emptyList<String>() }
val LocalVaultActions = staticCompositionLocalOf { VaultActions() }

@Composable
fun FolderPicker(folders: List<String>, onDismiss: () -> Unit, onMove: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    val error = folderNameError(name, folders)
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Move to folder") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            (listOf("Unfiled") + folders).distinct().forEach { folder ->
                TextButton(onClick = { onMove(if (folder == "Unfiled") "" else folder) }) { Text(folder) }
            }
            OutlinedTextField(name, { name = it.take(40) }, singleLine = true, label = { Text("New folder") },
                isError = name.isNotBlank() && error != null, supportingText = if (name.isNotBlank() && error != null) ({ Text(error) }) else null)
        }
    }, confirmButton = { TextButton(enabled = error == null, onClick = { onMove(normalizedFolderName(name)) }) { Text("Create and move") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

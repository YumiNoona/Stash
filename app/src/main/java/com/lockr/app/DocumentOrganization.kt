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
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Move to folder") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            (listOf("Unfiled") + folders).distinct().forEach { folder ->
                TextButton(onClick = { onMove(if (folder == "Unfiled") "" else folder) }) { Text(folder) }
            }
            OutlinedTextField(name, { name = it.take(40) }, singleLine = true, label = { Text("New folder") })
        }
    }, confirmButton = { TextButton(enabled = name.isNotBlank() && !name.equals("Unfiled", true), onClick = { onMove(name.trim()) }) { Text("Create and move") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentPagesDialog(attachments: List<VaultAttachment>, onDismiss: () -> Unit, onSave: (List<VaultAttachment>) -> Unit) {
    var pages by remember { mutableStateOf(attachments) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    ModalBottomSheet(onDismissRequest = { if (!busy) onDismiss() },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 20.dp, end = 20.dp, bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Organize pages", style = MaterialTheme.typography.headlineSmall)
            Text("Reorder, rotate or remove pages before saving.", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(Modifier.heightIn(max = 390.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            pages.forEachIndexed { i, page ->
                Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .65f)) {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Description, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Page ${i + 1}", style = MaterialTheme.typography.titleSmall)
                                Text(page.name, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            IconButton(enabled = !busy && i > 0, onClick = { pages = pages.toMutableList().apply { add(i - 1, removeAt(i)) } }) {
                                Icon(Icons.Outlined.ArrowUpward, "Move page ${i + 1} up")
                            }
                            IconButton(enabled = !busy && i < pages.lastIndex, onClick = { pages = pages.toMutableList().apply { add(i + 1, removeAt(i)) } }) {
                                Icon(Icons.Outlined.ArrowDownward, "Move page ${i + 1} down")
                            }
                            IconButton(enabled = !busy && page.mimeType.startsWith("image/"), onClick = {
                        busy = true
                        scope.launch {
                            try {
                                val rotated = withContext(Dispatchers.Default) {
                                    val source = decodeAttachmentPreview(page.base64Data, 2400, 3200) ?: error("Unreadable image")
                                    val target = Bitmap.createBitmap(source, 0, 0, source.width, source.height, Matrix().apply { postRotate(90f) }, true)
                                    val out = ByteArrayOutputStream(); target.compress(Bitmap.CompressFormat.PNG, 100, out)
                                    val bytes = out.toByteArray(); source.recycle(); target.recycle()
                                    page.copy(name = page.name.substringBeforeLast('.') + ".png", mimeType = "image/png", byteCount = bytes.size.toLong(), base64Data = Base64.encodeToString(bytes, Base64.NO_WRAP))
                                }
                                val updated = pages.toMutableList().apply { set(i, rotated) }
                                require(updated.sumOf { it.byteCount } <= MAX_VAULT_TOTAL_ATTACHMENT_BYTES)
                                pages = updated
                            } catch (_: Exception) { error = "Could not rotate this page within the document size limit." }
                            finally { busy = false }
                        }
                            }) { Icon(Icons.Outlined.RotateRight, "Rotate page ${i + 1}") }
                            IconButton(enabled = !busy && pages.size > 1, onClick = { pages = pages.toMutableList().apply { removeAt(i) } }) {
                                Icon(Icons.Outlined.DeleteOutline, "Remove page ${i + 1}", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
            }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall)
            Button(enabled = !busy, onClick = { onSave(pages) }, modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(18.dp)) { Text("Save pages") }
        }
    }
}

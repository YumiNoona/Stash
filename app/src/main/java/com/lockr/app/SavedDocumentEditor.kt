package com.lockr.app

import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/** All edits stay in this draft until Save pages. Cancel leaves the original untouched. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentPagesDialog(attachments: List<VaultAttachment>, onDismiss: () -> Unit, onSave: (List<VaultAttachment>) -> Unit) {
    var pages by remember { mutableStateOf(attachments) }
    var undo by remember { mutableStateOf<List<VaultAttachment>?>(null) }
    var busy by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf(false) }
    var correctIndex by remember { mutableIntStateOf(-1) }
    var error by remember { mutableStateOf("") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val change: (List<VaultAttachment>) -> Unit = { next ->
        require(next.isNotEmpty()) { "Keep at least one page." }
        require(next.size <= MAX_VAULT_ATTACHMENT_COUNT || next.size <= pages.size) { "A document can contain up to 20 files." }
        require(next.sumOf { it.byteCount } <= MAX_VAULT_TOTAL_ATTACHMENT_BYTES || next.sumOf { it.byteCount } <= pages.sumOf { it.byteCount }) { "The document must fit within 24 MB." }
        undo = pages; pages = next; error = ""
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isEmpty()) busy = false
        else scope.launch {
            val base = pages
            val added = mutableListOf<VaultAttachment>()
            var bytes = base.sumOf { it.byteCount }
            var warning = if (uris.size > remainingDocumentSlots(base.size)) "Only 20 files can be kept in a document." else ""
            try {
                for (uri in uris.take(remainingDocumentSlots(base.size))) {
                    try {
                        val remaining = MAX_VAULT_TOTAL_ATTACHMENT_BYTES - bytes
                        require(remaining > 0) { "The document must fit within 24 MB." }
                        val file = readVaultAttachment(context, uri, remaining)
                        added += file; bytes += file.byteCount
                    } catch (cancelled: CancellationException) { throw cancelled }
                    catch (failure: Exception) { warning = failure.message ?: "One file could not be opened."; break }
                }
                if (added.isNotEmpty()) change(base + added)
                error = warning
            } finally { busy = false }
        }
    }
    ModalBottomSheet(onDismissRequest = { if (!busy && !camera) onDismiss() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true, confirmValueChange = { it != SheetValue.Hidden || (!busy && !camera) }),
        containerColor = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 20.dp, end = 20.dp, bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Edit document pages", style = MaterialTheme.typography.headlineSmall)
            Text("Add, straighten, reorder, rotate or remove. The original changes only when you save.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(enabled = !busy && remainingDocumentSlots(pages.size) > 0, onClick = {
                    busy = true; picker.launch(arrayOf("image/*", "application/pdf"))
                }, modifier = Modifier.weight(1f)) { Icon(Icons.Outlined.AddPhotoAlternate, null, Modifier.size(18.dp)); Text(" Add files") }
                OutlinedButton(enabled = !busy && remainingDocumentSlots(pages.size) > 0, onClick = { camera = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.CameraAlt, null, Modifier.size(18.dp)); Text(" Scan")
                }
                IconButton(enabled = !busy && undo != null, onClick = { pages = undo!!; undo = null; error = "" }) { Icon(Icons.Outlined.Undo, "Undo last page change") }
            }
            Column(Modifier.weight(1f, fill = false).heightIn(max = 360.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                pages.forEachIndexed { index, page ->
                    Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .65f)) {
                        Column(Modifier.fillMaxWidth().padding(10.dp)) {
                            Text("${index + 1}. ${page.name}", style = MaterialTheme.typography.titleSmall, maxLines = 1)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                IconButton(enabled = !busy && index > 0, onClick = { change(pages.toMutableList().apply { add(index - 1, removeAt(index)) }) }) { Icon(Icons.Outlined.ArrowUpward, "Move page ${index + 1} up") }
                                IconButton(enabled = !busy && index < pages.lastIndex, onClick = { change(pages.toMutableList().apply { add(index + 1, removeAt(index)) }) }) { Icon(Icons.Outlined.ArrowDownward, "Move page ${index + 1} down") }
                                IconButton(enabled = !busy && page.mimeType.startsWith("image/"), onClick = { correctIndex = index }) { Icon(Icons.Outlined.Crop, "Correct edges of page ${index + 1}") }
                                IconButton(enabled = !busy && page.mimeType.startsWith("image/"), onClick = {
                                    busy = true
                                    scope.launch {
                                        try {
                                            val rotated = withContext(Dispatchers.Default) {
                                                val source = decodeAttachmentPreview(page.base64Data, 2400, 3200) ?: error("Unreadable image")
                                                var target: Bitmap? = null
                                                try {
                                                    target = Bitmap.createBitmap(source, 0, 0, source.width, source.height, Matrix().apply { postRotate(90f) }, true)
                                                    val output = ByteArrayOutputStream()
                                                    require(target.compress(Bitmap.CompressFormat.JPEG, 94, output))
                                                    val bytes = output.toByteArray()
                                                    page.copy(name = page.name.substringBeforeLast('.') + "-rotated.jpg", mimeType = "image/jpeg", byteCount = bytes.size.toLong(), base64Data = Base64.encodeToString(bytes, Base64.NO_WRAP))
                                                } finally { if (target !== source) target?.recycle(); source.recycle() }
                                            }
                                            change(pages.toMutableList().apply { set(index, rotated) })
                                        } catch (cancelled: CancellationException) { throw cancelled }
                                        catch (failure: Exception) { error = failure.message ?: "Could not rotate this page." }
                                        finally { busy = false }
                                    }
                                }) { Icon(Icons.Outlined.RotateRight, "Rotate page ${index + 1}") }
                                IconButton(enabled = !busy && pages.size > 1, onClick = { change(pages.toMutableList().apply { removeAt(index) }) }) { Icon(Icons.Outlined.DeleteOutline, "Remove page ${index + 1}", tint = MaterialTheme.colorScheme.error) }
                            }
                        }
                    }
                }
            }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            Button(enabled = !busy && pages.size <= MAX_VAULT_ATTACHMENT_COUNT && pages.sumOf { it.byteCount } <= MAX_VAULT_TOTAL_ATTACHMENT_BYTES,
                onClick = { validateDocumentPages(pages); onSave(pages) }, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("Save pages") }
            TextButton(enabled = !busy, onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cancel changes") }
        }
    }
    if (correctIndex in pages.indices) DocumentEdgeCorrectionSheet(pages[correctIndex], { correctIndex = -1 }) { corrected ->
        change(pages.toMutableList().apply { set(correctIndex, corrected) }); correctIndex = -1
    }
    if (camera && !busy) Dialog(onDismissRequest = { camera = false }, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(Modifier.fillMaxSize()) {
            CameraScannerScreen(VaultCategory.DOCUMENT, onBack = { camera = false }, onReviewPages = { files ->
                busy = true
                scope.launch {
                    val base = pages
                    val added = mutableListOf<VaultAttachment>()
                    var bytes = base.sumOf { it.byteCount }
                    var warning = if (files.size > remainingDocumentSlots(base.size)) "Only 20 files can be kept in a document." else ""
                    try {
                        for (file in files.take(remainingDocumentSlots(base.size))) {
                            try {
                                val remaining = MAX_VAULT_TOTAL_ATTACHMENT_BYTES - bytes
                                require(remaining > 0) { "The document must fit within 24 MB." }
                                val attachment = readFileVaultAttachment(file, remaining)
                                added += attachment; bytes += attachment.byteCount
                            } catch (cancelled: CancellationException) { throw cancelled }
                            catch (failure: Exception) { warning = failure.message ?: "A scan could not be opened."; break }
                        }
                        if (added.isNotEmpty()) change(base + added)
                        error = warning
                    } finally { files.forEach { it.delete() }; busy = false; camera = false }
                }
            }, modifier = Modifier.systemBarsPadding())
        }
    }
}

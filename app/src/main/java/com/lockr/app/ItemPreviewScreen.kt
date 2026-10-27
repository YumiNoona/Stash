package com.lockr.app

import android.graphics.Bitmap
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.math.max

@Composable
fun ItemPreviewScreen(
    item: VaultItem,
    onBack: () -> Unit,
    onRename: (String) -> Unit,
    onSaveField: (String, String) -> Unit,
    autoClearClipboard: Boolean,
    onSend: () -> Unit,
    onDelete: () -> Unit,
    onDetails: () -> Unit,
    onUpdatePages: (List<VaultAttachment>) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var dialog by remember(item.id) { mutableStateOf("") }
    var title by remember(item.title) { mutableStateOf(item.title) }
    var recognizedText by remember(item.id) { mutableStateOf("") }
    var extracting by remember(item.id) { mutableStateOf(false) }
    var extractionMessage by remember(item.id) { mutableStateOf("") }
    val scan: () -> Unit = {
        if (!extracting) scope.launch {
            extracting = true
            extractionMessage = ""
            try {
                val text = item.attachments.take(20).mapNotNull { attachment ->
                    try {
                        recognizeAttachmentText(context, attachment).takeIf(String::isNotBlank)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) { null }
                }.joinToString("\n\n").take(120_000)
                if (text.isBlank()) extractionMessage = "No readable text found. Try a clearer photo."
                else { recognizedText = text; dialog = "extract" }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                extractionMessage = "This file could not be read. Try another image or PDF."
            } finally { extracting = false }
        }
    }

    val pager = rememberPagerState(pageCount = { item.attachments.size })
    // The document owns the full canvas; chrome floats above it instead of shrinking it into a card.
    Box(modifier.fillMaxSize()) {
        if (item.attachments.isNotEmpty()) HorizontalPager(pager, Modifier.fillMaxSize(), key = { item.attachments[it].name + it }) { page ->
            DocumentPreviewCanvas(item.attachments[page], Modifier.fillMaxSize())
        } else Text("No attached file", color = LockrColors.TextSecondary, modifier = Modifier.align(Alignment.Center))
        if (item.attachments.size > 1) PreviewGlass(Modifier.align(Alignment.TopCenter).padding(top = 78.dp), RoundedCornerShape(20.dp)) {
            Text("Page ${pager.currentPage + 1} of ${item.attachments.size}", color = LockrColors.TextPrimary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        }

        Row(Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FloatingPreviewButton(Icons.Outlined.ChevronLeft, "Back", onBack)
            PreviewGlass(Modifier.weight(1f), RoundedCornerShape(20.dp)) {
                Text(item.title, color = LockrColors.TextPrimary, style = MaterialTheme.typography.titleSmall,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 15.dp, vertical = 13.dp))
            }
            FloatingPreviewButton(Icons.Outlined.Info, "File information", onDetails)
        }

        if (extracting || extractionMessage.isNotBlank()) PreviewGlass(
            Modifier.align(Alignment.BottomCenter).padding(start = 22.dp, end = 22.dp, bottom = 112.dp), RoundedCornerShape(16.dp)) {
            Text(if (extracting) "Reading text on this device…" else extractionMessage,
                color = LockrColors.TextPrimary, style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
        }
        PreviewGlass(Modifier.align(Alignment.BottomCenter).padding(horizontal = 20.dp, vertical = 16.dp)
            .widthIn(max = 360.dp).fillMaxWidth(), RoundedCornerShape(30.dp)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 9.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                PreviewAction(Icons.Outlined.Edit, "Edit", "Edit document", modifier = Modifier.weight(1f)) { title = item.title; dialog = "edit" }
                PreviewAction(Icons.Outlined.DocumentScanner, if (extracting) "Reading" else "Scan", "Scan document", !extracting, extracting, Modifier.weight(1f), scan)
                PreviewAction(Icons.Outlined.IosShare, "Share", "Share document", modifier = Modifier.weight(1f), onClick = { if (item.attachments.size > 1) scope.launch { shareVaultDocument(context, item) } else onSend() })
                PreviewAction(Icons.Outlined.DeleteOutline, "Delete", "Delete document", modifier = Modifier.weight(1f)) { dialog = "delete" }
            }
        }
    }
    if (dialog == "extract") ExtractFieldsDialog(recognizedText, onDismiss = { dialog = "" },
        onCopy = { copyPrivateText(context, it, autoClearClipboard) },
        onSave = { key, value -> onSaveField(key, value); dialog = "" })
    if (dialog == "edit") AlertDialog(onDismissRequest = { dialog = "" }, title = { Text("Edit document") },
        text = { Column {
            TextButton(onClick = { dialog = "rename" }) { Text("Rename document") }
            TextButton(onClick = { dialog = "pages" }) { Text("Organize pages") }
        } }, confirmButton = { TextButton(onClick = { dialog = "" }) { Text("Done") } })
    if (dialog == "pages") DocumentPagesDialog(item.attachments, { dialog = "" }) { onUpdatePages(it); dialog = "" }
    if (dialog == "rename") AlertDialog(
        onDismissRequest = { dialog = "" }, title = { Text("Edit document") },
        text = { OutlinedTextField(title, { title = it.take(80) }, singleLine = true, label = { Text("Name") }) },
        dismissButton = { TextButton(onClick = { dialog = "" }) { Text("Cancel") } },
        confirmButton = { TextButton(enabled = title.isNotBlank(), onClick = { onRename(title.trim()); dialog = "" }) { Text("Save") } })
    if (dialog == "delete") AlertDialog(
        onDismissRequest = { dialog = "" }, title = { Text("Delete this item?") },
        text = { Text("This removes the item and its files from Stash.") },
        dismissButton = { TextButton(onClick = { dialog = "" }) { Text("Cancel") } },
        confirmButton = { TextButton(onClick = { dialog = ""; onDelete() }) { Text("Delete") } })
}

@Composable
private fun PreviewGlass(modifier: Modifier = Modifier, shape: RoundedCornerShape, content: @Composable BoxScope.() -> Unit) {
    val dark = LocalDarkTheme.current
    val glass = if (dark) listOf(Color(0xCB666177), Color(0xC83B4B60), Color(0xB64F5968))
        else listOf(Color(0xD9FFFFFF), Color(0xC9E8E3FB), Color(0xC7DDF6EF))
    val rim = Brush.linearGradient(listOf(Color.White.copy(alpha = if (dark) 0.52f else 0.88f),
        Color(0x6688D8D0), Color(0x88CFB5F2)))
    Box(modifier.shadow(14.dp, shape, ambientColor = Color(0x335F4A88), spotColor = Color(0x33434D6D))
        .clip(shape).background(Brush.linearGradient(glass)).border(1.dp, rim, shape), content = content)
}

@Composable
private fun FloatingPreviewButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    PreviewGlass(Modifier.size(46.dp), RoundedCornerShape(23.dp)) {
        IconButton(onClick = onClick, modifier = Modifier.fillMaxSize()) {
            Icon(icon, label, tint = LockrColors.TextPrimary, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun PreviewAction(icon: ImageVector, label: String, description: String,
                          enabled: Boolean = true, busy: Boolean = false, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(modifier.height(64.dp).clip(RoundedCornerShape(22.dp))
        .clickable(enabled = enabled, onClick = onClick).padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        if (busy) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = LockrColors.Accent)
        else Icon(icon, description, tint = if (label == "Delete") MaterialTheme.colorScheme.error else LockrColors.TextPrimary,
            modifier = Modifier.size(22.dp))
        Text(label, color = LockrColors.TextPrimary, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

@Composable
internal fun DocumentPreviewCanvas(attachment: VaultAttachment, modifier: Modifier = Modifier,
                                   gesturesEnabled: Boolean = true, showPageControls: Boolean = true) {
    val context = LocalContext.current
    val pdf = attachment.mimeType.equals("application/pdf", true) || attachment.name.endsWith(".pdf", true)
    var pageIndex by remember(attachment.base64Data) { mutableIntStateOf(0) }
    var pageCount by remember(attachment.base64Data) { mutableIntStateOf(0) }
    var bitmap by remember(attachment.base64Data) { mutableStateOf<Bitmap?>(null) }
    var failed by remember(attachment.base64Data) { mutableStateOf(false) }
    var zoom by remember(attachment.base64Data, pageIndex) { mutableFloatStateOf(1f) }
    var pan by remember(attachment.base64Data, pageIndex) { mutableStateOf(Offset.Zero) }
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    val transform = rememberTransformableState { scale, offset, _ ->
        zoom = (zoom * scale).coerceIn(1f, 5f)
        pan = boundedDocumentPan(pan + offset, zoom, viewport, bitmap)
    }
    LaunchedEffect(attachment.base64Data, pageIndex) {
        failed = false
        bitmap = null
        try {
            val result = if (pdf) renderPdfAttachmentPage(attachment, pageIndex, context)
                else withContext(Dispatchers.IO) { decodeAttachmentPreview(attachment.base64Data, 1800, 2400) to 1 }
            bitmap = result.first; pageCount = result.second; failed = bitmap == null
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            bitmap = null; pageCount = 0; failed = true
        }
    }
    Box(modifier.clipToBounds().onSizeChanged {
        viewport = it
        pan = boundedDocumentPan(pan, zoom, it, bitmap)
    }, contentAlignment = Alignment.Center) {
        if (bitmap != null) Image(bitmap!!.asImageBitmap(), attachment.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().then(if (gesturesEnabled) Modifier.transformable(transform, canPan = { zoom > 1f }) else Modifier).graphicsLayer {
                scaleX = zoom; scaleY = zoom; translationX = pan.x; translationY = pan.y
            })
        else Text(if (failed) "This file could not be previewed." else "Opening document…",
            color = LockrColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
        if (showPageControls && pdf && pageCount > 1) PreviewGlass(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 112.dp), RoundedCornerShape(22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { pageIndex-- }, enabled = pageIndex > 0) { Icon(Icons.Outlined.ChevronLeft, "Previous page") }
                Text("${pageIndex + 1} / $pageCount", color = LockrColors.TextPrimary, style = MaterialTheme.typography.labelLarge)
                IconButton(onClick = { pageIndex++ }, enabled = pageIndex < pageCount - 1) { Icon(Icons.Outlined.ChevronRight, "Next page") }
            }
        }
    }
}

/** Constrain panning to the fitted document, including its actual letterboxed dimensions. */
private fun boundedDocumentPan(candidate: Offset, zoom: Float, viewport: IntSize, bitmap: Bitmap?): Offset {
    if (zoom <= 1f || viewport.width <= 0 || viewport.height <= 0 || bitmap == null) return Offset.Zero
    val fit = min(viewport.width.toFloat() / bitmap.width, viewport.height.toFloat() / bitmap.height)
    val maxX = max(0f, (bitmap.width * fit * zoom - viewport.width) / 2f)
    val maxY = max(0f, (bitmap.height * fit * zoom - viewport.height) / 2f)
    return Offset(candidate.x.coerceIn(-maxX, maxX), candidate.y.coerceIn(-maxY, maxY))
}

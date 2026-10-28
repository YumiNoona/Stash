package com.lockr.app

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.onClick
import kotlinx.coroutines.delay
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.BorderColor
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

@OptIn(ExperimentalMaterial3Api::class)
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
    var controlsVisible by remember(item.id) { mutableStateOf(false) }
    var interaction by remember(item.id) { mutableIntStateOf(0) }
    val toggleControls: () -> Unit = { controlsVisible = !controlsVisible; interaction++ }
    val useControls: () -> Unit = { controlsVisible = true; interaction++ }
    LaunchedEffect(controlsVisible, interaction, dialog, extracting) {
        if (controlsVisible && dialog.isBlank() && !extracting) { delay(3500); controlsVisible = false }
    }
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
            DocumentPreviewCanvas(item.attachments[page], Modifier.fillMaxSize(), showPageControls = controlsVisible, onTap = toggleControls, onPageInteraction = useControls)
        } else Text("No attached file", color = LockrColors.TextSecondary, modifier = Modifier.align(Alignment.Center))
        if (controlsVisible && item.attachments.size > 1) PreviewGlass(Modifier.align(Alignment.TopCenter).padding(top = 78.dp), RoundedCornerShape(20.dp)) {
            Text("Page ${pager.currentPage + 1} of ${item.attachments.size}", color = LockrColors.TextPrimary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        }

        AnimatedVisibility(controlsVisible, Modifier.align(Alignment.TopCenter), enter = fadeIn(), exit = fadeOut()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FloatingPreviewButton(Icons.Outlined.ChevronLeft, "Back", onBack)
                PreviewGlass(Modifier.weight(1f), RoundedCornerShape(20.dp)) {
                    Text(item.title, color = LockrColors.TextPrimary, style = MaterialTheme.typography.titleSmall,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 15.dp, vertical = 13.dp))
                }
                FloatingPreviewButton(Icons.Outlined.Info, "File information") { useControls(); onDetails() }
            }
        }

        if (extracting || extractionMessage.isNotBlank()) PreviewGlass(
            Modifier.align(Alignment.BottomCenter).padding(start = 22.dp, end = 22.dp, bottom = 112.dp), RoundedCornerShape(16.dp)) {
            Text(if (extracting) "Reading text on this device…" else extractionMessage,
                color = LockrColors.TextPrimary, style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
        }
        AnimatedVisibility(controlsVisible, Modifier.align(Alignment.BottomCenter), enter = fadeIn(), exit = fadeOut()) {
            PreviewGlass(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                .widthIn(max = 360.dp).fillMaxWidth(), RoundedCornerShape(30.dp)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    PreviewAction(Icons.Outlined.Edit, "Edit", "Edit document", modifier = Modifier.weight(1f)) { useControls(); title = item.title; dialog = "edit" }
                    PreviewAction(Icons.Outlined.DocumentScanner, if (extracting) "Reading" else "Scan", "Scan document", !extracting, extracting, Modifier.weight(1f)) { useControls(); scan() }
                    PreviewAction(Icons.Outlined.IosShare, "Share", "Share document", modifier = Modifier.weight(1f)) { useControls(); dialog = "share" }
                    PreviewAction(Icons.Outlined.DeleteOutline, "Delete", "Delete document", modifier = Modifier.weight(1f)) { useControls(); dialog = "delete" }
                }
            }
        }
    }
    if (dialog == "extract") ExtractFieldsDialog(recognizedText, onDismiss = { dialog = "" },
        onCopy = { copyPrivateText(context, it, autoClearClipboard) },
        onSave = { key, value -> onSaveField(key, value); dialog = "" })
    if (dialog == "edit") ModalBottomSheet(onDismissRequest = { dialog = "" },
        containerColor = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().verticalScroll(rememberScrollState())
            .padding(start = 22.dp, end = 22.dp, bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Edit document", style = MaterialTheme.typography.headlineSmall, color = LockrColors.TextPrimary)
            Text("Give this item a clear name and arrange its pages.", style = MaterialTheme.typography.bodyMedium, color = LockrColors.TextSecondary)
            OutlinedTextField(title, { title = it.take(80) }, modifier = Modifier.fillMaxWidth(),
                singleLine = true, label = { Text("Document name") }, shape = RoundedCornerShape(18.dp))
            Button(onClick = { onRename(title.trim()); dialog = "" },
                enabled = title.isNotBlank() && title.trim() != item.title,
                modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(18.dp)) {
                Icon(Icons.Outlined.Save, null, Modifier.size(19.dp)); Spacer(Modifier.width(9.dp)); Text("Save name")
            }
            Surface(onClick = { dialog = "pages" }, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .7f)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Tune, null, tint = LockrColors.Accent, modifier = Modifier.size(23.dp))
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Edit pages", style = MaterialTheme.typography.titleSmall, color = LockrColors.TextPrimary)
                        Text("Add, correct, reorder or rotate · ${item.attachments.size} ${if (item.attachments.size == 1) "page" else "pages"}",
                            style = MaterialTheme.typography.bodySmall, color = LockrColors.TextSecondary)
                    }
                    Icon(Icons.Outlined.ChevronRight, null, tint = LockrColors.TextSecondary)
                }
            }
        }
    }
    if (dialog == "pages") DocumentPagesDialog(item.attachments, { dialog = "" }) { onUpdatePages(it); dialog = "" }
    if (dialog == "share") ModalBottomSheet(onDismissRequest = { dialog = "" },
        containerColor = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, bottom = 25.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp)) {
            Text("Share document", style = MaterialTheme.typography.headlineSmall, color = LockrColors.TextPrimary)
            Text(item.title, style = MaterialTheme.typography.titleSmall, color = LockrColors.TextPrimary,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (item.attachments.isNotEmpty()) Surface(Modifier.fillMaxWidth().height(180.dp),
                shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                DocumentPreviewCanvas(item.attachments.first(), gesturesEnabled = false, showPageControls = false)
            }
            Text("${item.attachments.size} ${if (item.attachments.size == 1) "file" else "files"} · Send a copy outside Stash",
                style = MaterialTheme.typography.bodySmall, color = LockrColors.TextSecondary)
            if (item.attachments.getOrNull(pager.currentPage)?.let {
                it.mimeType.startsWith("image/") || it.mimeType == "application/pdf" || it.name.endsWith(".pdf", true)
            } == true) OutlinedButton(onClick = { dialog = "redact" }, modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(18.dp)) {
                Icon(Icons.Outlined.BorderColor, null, Modifier.size(19.dp)); Spacer(Modifier.width(9.dp)); Text("Redact text across document")
            }
            Button(onClick = {
                dialog = ""
                if (item.attachments.size > 1) scope.launch { shareVaultDocument(context, item) } else onSend()
            }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(18.dp)) {
                Icon(Icons.Outlined.IosShare, null, Modifier.size(19.dp)); Spacer(Modifier.width(9.dp)); Text("Continue to share")
            }
        }
    }
    if (dialog == "redact") ShareRedactionSheet(item.attachments, initialAttachment = pager.currentPage, onDismiss = { dialog = "" })
    if (dialog == "delete") ModalBottomSheet(onDismissRequest = { dialog = "" },
        containerColor = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 22.dp, end = 22.dp, bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.errorContainer) {
                Icon(Icons.Outlined.DeleteOutline, null, tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(15.dp).size(27.dp))
            }
            Text("Delete document?", style = MaterialTheme.typography.headlineSmall, color = LockrColors.TextPrimary)
            Text(item.title, style = MaterialTheme.typography.titleSmall, color = LockrColors.TextPrimary,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("This removes the document and its attached files from Stash. This cannot be undone.",
                style = MaterialTheme.typography.bodyMedium, color = LockrColors.TextSecondary)
            Button(onClick = { dialog = ""; onDelete() }, modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                Text("Delete document")
            }
            TextButton(onClick = { dialog = "" }, modifier = Modifier.fillMaxWidth()) { Text("Keep document") }
        }
    }
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
    Column(modifier.height(60.dp).clip(RoundedCornerShape(22.dp))
        .clickable(enabled = enabled, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        if (busy) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = LockrColors.Accent)
        else Icon(icon, description, tint = if (label == "Delete") MaterialTheme.colorScheme.error else LockrColors.TextPrimary,
            modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(5.dp))
        Text(label, color = LockrColors.TextPrimary, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

@Composable
internal fun DocumentPreviewCanvas(attachment: VaultAttachment, modifier: Modifier = Modifier,
                                   gesturesEnabled: Boolean = true, showPageControls: Boolean = true,
                                   onTap: (() -> Unit)? = null, onPageInteraction: () -> Unit = {}) {
    val currentTap by rememberUpdatedState(onTap)
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
    Box(modifier.then(if (onTap != null) Modifier.pointerInput(Unit) { detectTapGestures(onTap = { currentTap?.invoke() }) }
        .semantics { onClick(label = "Show or hide document controls") { currentTap?.invoke(); true } } else Modifier).clipToBounds().onSizeChanged {
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
                IconButton(onClick = { onPageInteraction(); pageIndex-- }, enabled = pageIndex > 0) { Icon(Icons.Outlined.ChevronLeft, "Previous page") }
                Text("${pageIndex + 1} / $pageCount", color = LockrColors.TextPrimary, style = MaterialTheme.typography.labelLarge)
                IconButton(onClick = { onPageInteraction(); pageIndex++ }, enabled = pageIndex < pageCount - 1) { Icon(Icons.Outlined.ChevronRight, "Next page") }
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

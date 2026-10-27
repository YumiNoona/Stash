package com.lockr.app

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.os.Handler
import android.os.Looper
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class RedactableLine(val text: String, val bounds: Rect)

private suspend fun <T> Task<T>.awaitRedaction(): T = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        if (!continuation.isActive) return@addOnCompleteListener
        if (task.isSuccessful) continuation.resume(task.result)
        else continuation.resumeWithException(task.exception ?: IllegalStateException("Could not read text"))
    }
}

private suspend fun readRedactableLines(bitmap: Bitmap): List<RedactableLine> {
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    return try {
        recognizer.process(InputImage.fromBitmap(bitmap, 0)).awaitRedaction().textBlocks
            .flatMap { it.lines }.mapNotNull { line -> line.boundingBox?.let { RedactableLine(line.text, it) } }
    } finally { recognizer.close() }
}

private suspend fun redactionBitmap(context: Context, attachment: VaultAttachment, page: Int): Pair<Bitmap?, Int> {
    return if (attachment.mimeType == "application/pdf" || attachment.name.endsWith(".pdf", true))
        renderPdfAttachmentPage(attachment, page, context)
    else withContext(Dispatchers.IO) { decodeAttachmentPreview(attachment.base64Data, 2400, 3200) to 1 }
}

private fun paintRedactions(source: Bitmap, lines: List<RedactableLine>, selected: Set<Int>): Bitmap {
    val output = source.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = AndroidCanvas(output)
    val brush = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.BLACK; style = Paint.Style.FILL }
    selected.forEach { index -> lines.getOrNull(index)?.bounds?.let { box ->
        canvas.drawRoundRect(RectF((box.left - 5).coerceAtLeast(0).toFloat(), (box.top - 4).coerceAtLeast(0).toFloat(),
            (box.right + 5).coerceAtMost(source.width).toFloat(), (box.bottom + 4).coerceAtMost(source.height).toFloat()), 2f, 2f, brush)
    } }
    return output
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareRedactionSheet(attachment: VaultAttachment, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var page by remember(attachment.base64Data) { mutableIntStateOf(0) }
    var pageCount by remember(attachment.base64Data) { mutableIntStateOf(1) }
    var bitmap by remember(attachment.base64Data, page) { mutableStateOf<Bitmap?>(null) }
    var lines by remember(attachment.base64Data, page) { mutableStateOf<List<RedactableLine>>(emptyList()) }
    var selections by remember(attachment.base64Data) { mutableStateOf<Map<Int, Set<Int>>>(emptyMap()) }
    var lineCache by remember(attachment.base64Data) { mutableStateOf<Map<Int, List<RedactableLine>>>(emptyMap()) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    LaunchedEffect(attachment.base64Data, page) {
        bitmap = null; lines = emptyList(); error = ""
        try {
            val (rendered, count) = redactionBitmap(context, attachment, page)
            if (rendered != null) {
                bitmap = rendered
                pageCount = count
                lines = lineCache[page] ?: readRedactableLines(rendered).also { lineCache = lineCache + (page to it) }
            } else error = "This file cannot be redacted as an image."
        } catch (_: Exception) { error = "Text could not be read from this page." }
    }
    val selected = selections[page].orEmpty()
    val preview = remember(bitmap, lines, selected) { bitmap?.let { paintRedactions(it, lines, selected) } }
    ModalBottomSheet(onDismissRequest = { if (!busy) onDismiss() }, containerColor = LockrColors.Surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Redact before sharing", style = MaterialTheme.typography.titleLarge)
            Text("Select text lines to cover. Check the preview; only the exported copy changes.",
                style = MaterialTheme.typography.bodySmall, color = LockrColors.TextSecondary)
            if (preview == null) LinearProgressIndicator(Modifier.fillMaxWidth())
            else Image(preview.asImageBitmap(), "Preview of redacted page", Modifier.fillMaxWidth().height(190.dp),
                contentScale = ContentScale.Fit)
            if (pageCount > 1) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(enabled = page > 0 && !busy, onClick = { page-- }) { Icon(Icons.Outlined.ChevronLeft, "Previous page") }
                Text("Page ${page + 1} of $pageCount", style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                IconButton(enabled = page < pageCount - 1 && !busy, onClick = { page++ }) { Icon(Icons.Outlined.ChevronRight, "Next page") }
            }
            Text("Recognized text", style = MaterialTheme.typography.titleSmall)
            Column(Modifier.fillMaxWidth().heightIn(max = 230.dp).verticalScroll(rememberScrollState())) {
                if (bitmap != null && lines.isEmpty()) Text("No text found on this page.", style = MaterialTheme.typography.bodySmall)
                lines.forEachIndexed { index, line ->
                    Row(Modifier.fillMaxWidth().clickable {
                        selections = selections + (page to if (index in selected) selected - index else selected + index)
                    }.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(index in selected, onCheckedChange = {
                            selections = selections + (page to if (index in selected) selected - index else selected + index)
                        })
                        Text(line.text, style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f).padding(start = 5.dp))
                    }
                }
            }
            if (error.isNotBlank()) Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            Button(onClick = {
                busy = true; error = ""
                scope.launch {
                    try {
                        val file = withContext(Dispatchers.IO) { createRedactedCopy(context, attachment, selections, lineCache, pageCount) }
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = if (file.extension == "pdf") "application/pdf" else "image/png"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            putExtra(Intent.EXTRA_TITLE, file.name)
                            clipData = ClipData.newRawUri(file.name, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share redacted copy"))
                        Handler(Looper.getMainLooper()).postDelayed({ file.delete() }, 10 * 60 * 1000L)
                        onDismiss()
                    } catch (_: Exception) { error = "Could not create a redacted copy." }
                    finally { busy = false }
                }
            }, enabled = selections.values.any { it.isNotEmpty() } && !busy,
                modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text(if (busy) "Preparing copy…" else "Share redacted copy")
            }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) { Text("Cancel") }
        }
    }
}

private suspend fun createRedactedCopy(context: Context, attachment: VaultAttachment,
                                       selections: Map<Int, Set<Int>>,
                                       lineCache: Map<Int, List<RedactableLine>>, pageCount: Int): File {
    val directory = File(context.cacheDir, "shares").apply { mkdirs() }
    val pdf = attachment.mimeType == "application/pdf" || attachment.name.endsWith(".pdf", true)
    val target = File(directory, "redacted-${System.currentTimeMillis()}.${if (pdf) "pdf" else "png"}")
    try {
        if (pdf) {
            val document = PdfDocument()
            try {
                for (index in 0 until pageCount) {
                    val source = redactionBitmap(context, attachment, index).first ?: error("Page unavailable")
                    val masked = paintRedactions(source, lineCache[index].orEmpty(), selections[index].orEmpty())
                    val width = 595
                    val height = (source.height * width.toFloat() / source.width).toInt().coerceAtLeast(1)
                    val page = document.startPage(PdfDocument.PageInfo.Builder(width, height, index + 1).create())
                    page.canvas.drawBitmap(masked, null, RectF(0f,0f,width.toFloat(),height.toFloat()), null)
                    document.finishPage(page)
                    source.recycle(); masked.recycle()
                }
                withContext(Dispatchers.IO) { target.outputStream().use { document.writeTo(it) } }
            } finally { document.close() }
        } else {
            val source = redactionBitmap(context, attachment, 0).first ?: error("Image unavailable")
            val masked = paintRedactions(source, lineCache[0].orEmpty(), selections[0].orEmpty())
            withContext(Dispatchers.IO) { target.outputStream().use { masked.compress(Bitmap.CompressFormat.PNG, 100, it) } }
            source.recycle(); masked.recycle()
        }
        return target
    } catch (error: Exception) { target.delete(); throw error }
}

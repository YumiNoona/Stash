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

internal data class RedactionPage(val attachment: Int, val page: Int)
internal fun documentRedactionPlan(counts: List<Int>): List<RedactionPage> {
    require(counts.isNotEmpty() && counts.all { it > 0 } && counts.sumOf { it.toLong() } <= 100) {
        "Redaction supports readable documents with up to 100 pages."
    }
    return counts.flatMapIndexed { attachment, count -> List(count) { page -> RedactionPage(attachment, page) } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareRedactionSheet(attachments: List<VaultAttachment>, initialAttachment: Int = 0, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pages by remember(attachments) { mutableStateOf<List<RedactionPage>>(emptyList()) }
    var page by remember(attachments) { mutableIntStateOf(0) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var lines by remember { mutableStateOf<List<RedactableLine>>(emptyList()) }
    var selections by remember(attachments) { mutableStateOf<Map<Int, Set<Int>>>(emptyMap()) }
    var lineCache by remember(attachments) { mutableStateOf<Map<Int, List<RedactableLine>>>(emptyMap()) }
    var busy by remember { mutableStateOf(false) }
    var reading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    LaunchedEffect(attachments) {
        try {
            val counts = attachments.map { attachment ->
                if (attachment.mimeType == "application/pdf" || attachment.name.endsWith(".pdf", true)) {
                    val (first, count) = redactionBitmap(context, attachment, 0)
                    first?.recycle()
                    count
                } else {
                    require(attachment.mimeType.startsWith("image/")) { "This document contains a file that cannot be redacted." }
                    1
                }
            }
            pages = documentRedactionPlan(counts)
            page = pages.indexOfFirst { it.attachment == initialAttachment }.coerceAtLeast(0)
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (failure: Exception) { error = failure.message ?: "This document could not be opened."; reading = false }
    }
    LaunchedEffect(pages, page) {
        val ref = pages.getOrNull(page) ?: return@LaunchedEffect
        bitmap = null; lines = emptyList(); error = ""; reading = true
        try {
            val rendered = redactionBitmap(context, attachments[ref.attachment], ref.page).first
                ?: throw java.io.IOException("This page cannot be previewed. No redacted copy will be shared.")
            bitmap = rendered
            lines = lineCache[page] ?: readRedactableLines(rendered).also { lineCache = lineCache + (page to it) }
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (failure: Exception) { error = failure.message ?: "Text could not be read from this page." }
        finally { reading = false }
    }
    val selected = selections[page].orEmpty()
    val preview by produceState<Bitmap?>(null, bitmap, lines, selected) {
        value = bitmap?.let { source -> withContext(Dispatchers.Default) { paintRedactions(source, lines, selected) } }
    }
    ModalBottomSheet(onDismissRequest = { if (!busy) onDismiss() }, containerColor = LockrColors.Surface,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true, confirmValueChange = { it != SheetValue.Hidden || !busy }),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Redact document before sharing", style = MaterialTheme.typography.titleLarge)
            Text("Cover text on any page. All pages stay together in the exported copy; the saved original stays unchanged.", style = MaterialTheme.typography.bodySmall, color = LockrColors.TextSecondary)
            if (preview != null) Image(preview!!.asImageBitmap(), "Preview of redacted page", Modifier.fillMaxWidth().height(190.dp), contentScale = ContentScale.Fit)
            if (reading || busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (pages.size > 1) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(enabled = page > 0 && !busy, onClick = { page-- }) { Icon(Icons.Outlined.ChevronLeft, "Previous page") }
                Text("Page ${page + 1} of ${pages.size}", modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                IconButton(enabled = page < pages.lastIndex && !busy, onClick = { page++ }) { Icon(Icons.Outlined.ChevronRight, "Next page") }
            }
            Text("Recognized text", style = MaterialTheme.typography.titleSmall)
            Column(Modifier.weight(1f, fill = false).fillMaxWidth().heightIn(max = 210.dp).verticalScroll(rememberScrollState())) {
                if (!reading && bitmap != null && lines.isEmpty()) Text("No text found on this page.", style = MaterialTheme.typography.bodySmall)
                lines.forEachIndexed { index, line ->
                    Row(Modifier.fillMaxWidth().clickable(enabled = !busy && !reading) {
                        selections = selections + (page to if (index in selected) selected - index else selected + index)
                    }, verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(index in selected, enabled = !busy && !reading, onCheckedChange = {
                            selections = selections + (page to if (index in selected) selected - index else selected + index)
                        })
                        Text(line.text, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f).padding(start = 5.dp))
                    }
                }
            }
            if (error.isNotBlank()) Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            Button(enabled = selections.values.any { it.isNotEmpty() } && !busy && !reading && error.isBlank(), onClick = {
                busy = true; error = ""
                val chosen = selections.toMap(); val recognized = lineCache.toMap(); val plan = pages.toList()
                scope.launch {
                    try {
                        val file = withContext(Dispatchers.IO) { createRedactedCopy(context, attachments, plan, chosen, recognized) }
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = if (file.extension == "pdf") "application/pdf" else "image/png"
                            putExtra(Intent.EXTRA_STREAM, uri); putExtra(Intent.EXTRA_TITLE, file.name)
                            clipData = ClipData.newRawUri(file.name, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share redacted document"))
                        Handler(Looper.getMainLooper()).postDelayed({ file.delete() }, 10 * 60 * 1000L)
                        onDismiss()
                    } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                    catch (failure: Exception) { error = failure.message ?: "Could not create a redacted copy." }
                    finally { busy = false }
                }
            }, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text(if (busy) "Preparing copy…" else "Share redacted copy") }
            TextButton(enabled = !busy, onClick = onDismiss, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) { Text("Cancel") }
        }
    }
}

private suspend fun createRedactedCopy(context: Context, attachments: List<VaultAttachment>, pages: List<RedactionPage>,
    selections: Map<Int, Set<Int>>, lineCache: Map<Int, List<RedactableLine>>): File {
    require(pages.isNotEmpty() && pages.size <= 100)
    val directory = File(context.cacheDir, "shares").apply { mkdirs() }
    val pdf = pages.size > 1 || attachments.first().mimeType == "application/pdf" || attachments.first().name.endsWith(".pdf", true)
    val target = File(directory, "redacted-${System.currentTimeMillis()}.${if (pdf) "pdf" else "png"}")
    suspend fun render(index: Int): Bitmap {
        val ref = pages[index]
        val source = redactionBitmap(context, attachments[ref.attachment], ref.page).first ?: throw java.io.IOException("Page ${index + 1} could not be read. No copy was shared.")
        try {
            val selection = selections[index].orEmpty()
            if (selection.isEmpty()) return source
            val recognized = lineCache[index] ?: throw java.io.IOException("Redaction data for page ${index + 1} is unavailable.")
            require(selection.all { it in recognized.indices }) { "Redaction data changed. Review the page again." }
            return paintRedactions(source, recognized, selection).also { source.recycle() }
        } catch (failure: Throwable) { source.recycle(); throw failure }
    }
    try {
        if (pdf) {
            val document = PdfDocument()
            try {
                pages.indices.forEach { index ->
                    val masked = render(index)
                    try {
                        val width = 595; val height = (masked.height * width.toFloat() / masked.width).toInt().coerceAtLeast(1)
                        val page = document.startPage(PdfDocument.PageInfo.Builder(width, height, index + 1).create())
                        page.canvas.drawBitmap(masked, null, RectF(0f, 0f, width.toFloat(), height.toFloat()), null)
                        document.finishPage(page)
                    } finally { masked.recycle() }
                }
                target.outputStream().use { document.writeTo(it) }
            } finally { document.close() }
        } else {
            val masked = render(0)
            try { target.outputStream().use { require(masked.compress(Bitmap.CompressFormat.PNG, 100, it)) } }
            finally { masked.recycle() }
        }
        return target
    } catch (_: OutOfMemoryError) { target.delete(); throw java.io.IOException("This document needs too much memory. Redact a smaller document.") }
    catch (failure: Throwable) { target.delete(); throw failure }
}

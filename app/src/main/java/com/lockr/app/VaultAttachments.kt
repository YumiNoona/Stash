package com.lockr.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.File
import java.net.URLConnection

const val MAX_VAULT_ATTACHMENT_COUNT = 20
const val MAX_VAULT_ATTACHMENT_BYTES = 24L * 1024 * 1024
const val MAX_VAULT_TOTAL_ATTACHMENT_BYTES = 24L * 1024 * 1024

suspend fun readVaultAttachment(context: Context, uri: Uri, byteLimit: Long): VaultAttachment = withContext(Dispatchers.IO) {
    val name = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)) else null
    }?.takeIf { it.isNotBlank() } ?: "Attachment"
    val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
    val output = ByteArrayOutputStream()
    var total = 0L
    context.contentResolver.openInputStream(uri)?.use { input ->
        val buffer = ByteArray(16 * 1024)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            total += count
            if (total > byteLimit) throw IOException("The selected files exceed the available attachment space for this item.")
            output.write(buffer, 0, count)
        }
        buffer.fill(0)
    } ?: throw IOException("That file could not be opened.")
    if (total == 0L) throw IOException("That file is empty.")
    val bytes = output.toByteArray()
    try {
        VaultAttachment(
            name = name.replace(Regex("[\\\\/:*?\"<>|]"), "_").take(120),
            mimeType = mimeType,
            byteCount = total,
            base64Data = Base64.encodeToString(bytes, Base64.NO_WRAP)
        )
    } finally {
        bytes.fill(0)
    }
}

suspend fun readFileVaultAttachment(file: File, byteLimit: Long): VaultAttachment = withContext(Dispatchers.IO) {
    if (!file.isFile || file.length() <= 0L) throw IOException("The scanned page could not be opened.")
    if (file.length() > byteLimit) throw IOException("This attachment is larger than the available vault space.")
    val bytes = file.readBytes()
    try {
        val mime = URLConnection.guessContentTypeFromName(file.name)
            ?: if (file.extension.equals("jpg", true) || file.extension.equals("jpeg", true)) "image/jpeg" else "application/octet-stream"
        VaultAttachment(
            name = file.name.replace(Regex("[\\\\/:*?\"<>|]"), "_").take(120),
            mimeType = mime,
            byteCount = bytes.size.toLong(),
            base64Data = Base64.encodeToString(bytes, Base64.NO_WRAP)
        )
    } finally {
        bytes.fill(0)
    }
}

@Composable
fun VaultAttachmentPreview(attachment: VaultAttachment, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var bitmap by remember(attachment.base64Data) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(attachment.base64Data) {
        bitmap = if (isPdfAttachment(attachment)) {
            renderPdfAttachmentPage(attachment, 0, context).first
        } else withContext(Dispatchers.IO) { decodeAttachmentPreview(attachment.base64Data) }
    }
    if (bitmap != null) {
        Image(
            bitmap!!.asImageBitmap(),
            contentDescription = attachment.name,
            modifier = modifier.clip(RoundedCornerShape(22.dp)),
            contentScale = ContentScale.Fit
        )
    } else {
        Box(
            modifier.clip(RoundedCornerShape(22.dp)).background(Color(0xBFEAF2FA)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    if (attachment.mimeType.startsWith("image/")) Icons.Outlined.Image else Icons.AutoMirrored.Outlined.InsertDriveFile,
                    contentDescription = null,
                    tint = Color(0xFF557FA8),
                    modifier = Modifier.size(34.dp)
                )
                Text(attachment.name, color = LockrColors.TextSecondary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                Text(formatAttachmentSize(attachment.byteCount), color = LockrColors.TextSecondary, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun VaultAttachmentsSection(attachments: List<VaultAttachment>, modifier: Modifier = Modifier) {
    if (attachments.isEmpty()) return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var preview by remember { mutableStateOf<VaultAttachment?>(null) }
    var exportTarget by remember { mutableStateOf<VaultAttachment?>(null) }
    var exportMessage by remember { mutableStateOf<String?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val attachment = exportTarget
        exportTarget = null
        if (uri != null && attachment != null) {
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        val bytes = Base64.decode(attachment.base64Data, Base64.DEFAULT)
                        try {
                            context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                                ?: throw IOException("The selected destination could not be opened.")
                        } finally {
                            bytes.fill(0)
                        }
                    }
                    exportMessage = "Saved a copy to the selected location."
                } catch (_: Exception) {
                    exportMessage = "Could not save a copy of this attachment."
                }
            }
        }
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Attachments · ${attachments.size}", style = MaterialTheme.typography.titleSmall)
        attachments.forEach { attachment ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp))
                    .background(Color.White.copy(alpha = 0.82f))
                    .border(1.dp, Color.White, RoundedCornerShape(17.dp))
                    .padding(start = 10.dp, end = 6.dp, top = 7.dp, bottom = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFE8F1FC)), contentAlignment = Alignment.Center) {
                    Icon(
                        if (attachment.mimeType.startsWith("image/")) Icons.Outlined.Image else Icons.AutoMirrored.Outlined.InsertDriveFile,
                        contentDescription = null,
                        tint = Color(0xFF557FA8),
                        modifier = Modifier.size(21.dp)
                    )
                }
                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                    Text(attachment.name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                    Text("${attachment.mimeType.substringAfter('/').uppercase()} · ${formatAttachmentSize(attachment.byteCount)}", color = LockrColors.TextSecondary, style = MaterialTheme.typography.labelSmall)
                }
                if (attachment.mimeType.startsWith("image/") || isPdfAttachment(attachment)) {
                    IconButton(onClick = { preview = attachment }) {
                        Icon(if (attachment.mimeType.startsWith("image/")) Icons.Outlined.Image else Icons.AutoMirrored.Outlined.InsertDriveFile, contentDescription = "Preview ${attachment.name}", tint = LockrColors.TextPrimary)
                    }
                }
                IconButton(onClick = {
                    exportMessage = null
                    exportTarget = attachment
                    exportLauncher.launch(attachment.name)
                }) {
                    Icon(Icons.Outlined.Download, contentDescription = "Save a copy of ${attachment.name}", tint = LockrColors.TextPrimary)
                }
            }
        }
        exportMessage?.let { Text(it, color = LockrColors.TextSecondary, style = MaterialTheme.typography.bodySmall) }
    }

    preview?.let { attachment ->
        AttachmentPreviewDialog(
            attachment = attachment,
            onClose = { preview = null },
            onExport = {
                exportMessage = null
                exportTarget = attachment
                exportLauncher.launch(attachment.name)
                preview = null
            }
        )
    }
}

@Composable
private fun AttachmentPreviewDialog(attachment: VaultAttachment, onClose: () -> Unit, onExport: () -> Unit) {
    var bitmap by remember(attachment.base64Data) { mutableStateOf<Bitmap?>(null) }
    var failed by remember(attachment.base64Data) { mutableStateOf(false) }
    var pageIndex by remember(attachment.base64Data) { mutableStateOf(0) }
    var pageCount by remember(attachment.base64Data) { mutableStateOf(0) }
    val context = LocalContext.current
    val isPdf = isPdfAttachment(attachment)
    LaunchedEffect(attachment.base64Data, pageIndex) {
        val result = if (isPdf) renderPdfAttachmentPage(attachment, pageIndex, context)
        else withContext(Dispatchers.IO) { decodeAttachmentPreview(attachment.base64Data) to 1 }
        bitmap = result.first
        pageCount = result.second
        failed = bitmap == null
    }
    Dialog(onDismissRequest = onClose) {
        Surface(shape = RoundedCornerShape(25.dp), color = Color(0xFFFAFBFD)) {
            Column(Modifier.fillMaxWidth().padding(14.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(attachment.name, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleSmall)
                    IconButton(onClick = onClose) { Icon(Icons.Outlined.Close, contentDescription = "Close preview") }
                }
                Box(Modifier.fillMaxWidth().height(430.dp), contentAlignment = Alignment.Center) {
                    when {
                        bitmap != null -> androidx.compose.foundation.Image(
                            bitmap!!.asImageBitmap(),
                            contentDescription = attachment.name,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Fit
                        )
                        failed -> Text("This image could not be previewed. You can still save a copy.", color = LockrColors.TextSecondary)
                        else -> Text("Opening image…", color = LockrColors.TextSecondary)
                    }
                }
                if (isPdf && pageCount > 1) {
                    androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
                        TextButton(onClick = { pageIndex = (pageIndex - 1).coerceAtLeast(0) }, enabled = pageIndex > 0) { Text("Previous") }
                        Text("Page ${pageIndex + 1} of $pageCount", style = MaterialTheme.typography.bodySmall, color = LockrColors.TextSecondary)
                        TextButton(onClick = { pageIndex = (pageIndex + 1).coerceAtMost(pageCount - 1) }, enabled = pageIndex < pageCount - 1) { Text("Next") }
                    }
                }
                TextButton(onClick = onExport, modifier = Modifier.align(Alignment.End)) { Text("Save a copy") }
            }
        }
    }
}

private fun isPdfAttachment(attachment: VaultAttachment): Boolean =
    attachment.mimeType.equals("application/pdf", ignoreCase = true) || attachment.name.endsWith(".pdf", ignoreCase = true)

internal suspend fun renderPdfAttachmentPage(attachment: VaultAttachment, requestedPage: Int, context: Context): Pair<Bitmap?, Int> = withContext(Dispatchers.IO) {
    val bytes = try { Base64.decode(attachment.base64Data, Base64.DEFAULT) } catch (_: IllegalArgumentException) { return@withContext null to 0 }
    val temporaryPdf = File.createTempFile("stash-preview-", ".pdf", context.cacheDir)
    try {
        temporaryPdf.outputStream().use { it.write(bytes) }
        val descriptor = ParcelFileDescriptor.open(temporaryPdf, ParcelFileDescriptor.MODE_READ_ONLY)
        descriptor.use { parcel ->
            PdfRenderer(parcel).use { renderer ->
                if (renderer.pageCount == 0) return@withContext null to 0
                val page = renderer.openPage(requestedPage.coerceIn(0, renderer.pageCount - 1))
                try {
                    val scale = minOf(1300f / page.width, 1700f / page.height, 2f)
                    val bitmap = Bitmap.createBitmap((page.width * scale).toInt().coerceAtLeast(1), (page.height * scale).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmap to renderer.pageCount
                } finally {
                    page.close()
                }
            }
        }
    } finally {
        bytes.fill(0)
        temporaryPdf.delete()
    }
}

internal fun decodeAttachmentPreview(base64Data: String, maxWidth: Int = 1600, maxHeight: Int = 1900): Bitmap? {
    val bytes = try {
        Base64.decode(base64Data, Base64.DEFAULT)
    } catch (_: IllegalArgumentException) {
        return null
    }
    return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sampleSize = 1
        while (bounds.outWidth / sampleSize > maxWidth || bounds.outHeight / sampleSize > maxHeight) sampleSize *= 2
        BitmapFactory.decodeByteArray(
            bytes,
            0,
            bytes.size,
            BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }
        )
    } finally {
        bytes.fill(0)
    }
}

fun formatAttachmentSize(byteCount: Long): String = when {
    byteCount >= 1024L * 1024L -> "${byteCount / (1024L * 1024L)} MB"
    byteCount >= 1024L -> "${byteCount / 1024L} KB"
    else -> "$byteCount B"
}

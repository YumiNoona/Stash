package com.lockr.app

import android.content.Context
import android.content.Intent
import android.content.ClipData
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

fun shareVaultAttachment(context: Context, attachment: VaultAttachment) {
    try {
        val shareDirectory = File(context.cacheDir, "shares").apply { mkdirs() }
        shareDirectory.listFiles()?.forEach { if (System.currentTimeMillis() - it.lastModified() > 3_600_000L) it.delete() }
        val safeName = attachment.name.replace(Regex("[^A-Za-z0-9._-]"), "_").take(80).ifBlank { "stash-file" }
        val target = File(shareDirectory, "${System.currentTimeMillis()}-$safeName")
        val bytes = Base64.decode(attachment.base64Data, Base64.DEFAULT)
        try { target.writeBytes(bytes) } finally { bytes.fill(0) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", target)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = attachment.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TITLE, attachment.name)
            clipData = ClipData.newRawUri(attachment.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "Send from Stash").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        Handler(Looper.getMainLooper()).postDelayed({ target.delete() }, 10 * 60 * 1000L)
    } catch (_: Exception) {
        Toast.makeText(context, "Could not share this file.", Toast.LENGTH_SHORT).show()
    }
}

fun clearSharedAttachmentCache(context: Context) {
    File(context.cacheDir, "shares").listFiles()?.forEach(File::delete)
}

/** Multi-page image documents share as one PDF; mixed files travel together in a ZIP. */
suspend fun shareVaultDocument(context: Context, item: VaultItem) {
    try {
        val target = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val directory = File(context.cacheDir, "shares").apply { mkdirs() }
            File(directory, "${System.currentTimeMillis()}-${documentDownloadName(listOf(item)).replace(Regex("[^A-Za-z0-9._-]"), "_")}").apply {
                outputStream().use { writeDocumentDownload(context, listOf(item), it) }
            }
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", target)
        val mime = if (target.name.endsWith(".pdf")) "application/pdf" else "application/zip"
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = mime; putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TITLE, item.title)
            clipData = ClipData.newRawUri(item.title, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }, "Share document").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        Handler(Looper.getMainLooper()).postDelayed({ target.delete() }, 10 * 60 * 1000L)
    } catch (_: Exception) { Toast.makeText(context, "Could not share this document.", Toast.LENGTH_SHORT).show() }
}

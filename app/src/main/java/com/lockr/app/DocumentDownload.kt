package com.lockr.app

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.util.Base64
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

private fun imageDocument(items: List<VaultItem>) = items.size == 1 && items.first().attachments.size > 1 && items.first().attachments.all { it.mimeType.startsWith("image/") }
fun documentDownloadName(items: List<VaultItem>): String = when {
    imageDocument(items) -> items.first().title.substringBeforeLast('.') + ".pdf"
    items.size == 1 && items.first().attachments.size == 1 -> items.first().attachments.first().name
    else -> "Stash-documents.zip"
}

/** Streams explicit user exports; private card and password values are never exported here. */
fun writeDocumentDownload(context: Context, items: List<VaultItem>, output: OutputStream) {
    require(items.isNotEmpty() && items.all { it.category != VaultCategory.CARD && it.category != VaultCategory.PASSWORD })
    if (imageDocument(items)) {
        val pdf = PdfDocument()
        try {
            items.first().attachments.forEachIndexed { index, attachment ->
                val bitmap = decodeAttachmentPreview(attachment.base64Data, 2200, 3000) ?: error("Unreadable page")
                val page = pdf.startPage(PdfDocument.PageInfo.Builder(595, 842, index + 1).create())
                val scale = minOf(555f / bitmap.width, 802f / bitmap.height)
                val w = bitmap.width * scale; val h = bitmap.height * scale
                page.canvas.drawBitmap(bitmap, null, RectF((595-w)/2, (842-h)/2, (595+w)/2, (842+h)/2), null)
                pdf.finishPage(page); bitmap.recycle()
            }
            pdf.writeTo(output)
        } finally { pdf.close() }
    } else if (items.size == 1 && items.first().attachments.size == 1) {
        val bytes = Base64.decode(items.first().attachments.first().base64Data, Base64.DEFAULT)
        try { output.write(bytes) } finally { bytes.fill(0) }
    } else ZipOutputStream(output).use { zip ->
        val names = mutableListOf<String>()
        items.forEach { item -> item.attachments.forEach { attachment ->
            val safe = attachment.name.replace(Regex("[^A-Za-z0-9._ -]"), "_").take(120).ifBlank { "document" }
            val name = uniqueVaultTitle(safe, names); names += name
            zip.putNextEntry(ZipEntry(name))
            val bytes = Base64.decode(attachment.base64Data, Base64.DEFAULT)
            try { zip.write(bytes) } finally { bytes.fill(0) }
            zip.closeEntry()
        } }
    }
}

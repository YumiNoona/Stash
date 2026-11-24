package com.lockr.app

import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream

@RunWith(AndroidJUnit4::class)
class AttachmentFormatDeviceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private fun original(name: String) = instrumentation.context.assets.open("formats/$name").use { it.readBytes() }
    private suspend fun imported(name: String) = readVaultAttachment(context,
        Uri.parse("content://com.lockr.app.test.formats/$name"), MAX_VAULT_ATTACHMENT_BYTES)

    @Test fun commonImagesImportPreviewAndExportUnchanged() = runBlocking {
        for (name in listOf("page.png", "page.jpg", "page.jpeg", "page.gif", "page.webp", "page.bmp", "camera.img")) {
            val attachment = imported(name)
            assertTrue(name, isImageAttachment(attachment))
            val bitmap = decodeAttachmentPreview(attachment.base64Data, 80, 80)
            assertNotNull(name, bitmap)
            try { assertTrue(name, bitmap!!.width <= 80 && bitmap.height <= 80) } finally { bitmap?.recycle() }
            assertArrayEquals(name, original(name), exported(attachment))
        }
    }

    @Test fun modernHeifAndAvifUseNativeDecoderWithoutChangingOriginals() = runBlocking {
        // This suite is also run on a modern emulator with both codecs available.
        org.junit.Assume.assumeTrue(android.os.Build.VERSION.SDK_INT >= 31)
        for (name in listOf("page.heic", "page.avif")) {
            val attachment = imported(name)
            val bitmap = decodeAttachmentPreview(attachment.base64Data, 80, 80)
            assertNotNull(name, bitmap)
            try { assertTrue(name, bitmap!!.width <= 80 && bitmap.height <= 80) } finally { bitmap?.recycle() }
            assertArrayEquals(name, original(name), exported(attachment))
        }
    }

    @Test fun cameraRotationStaysUprightAndBounded() = runBlocking {
        val names = if (android.os.Build.VERSION.SDK_INT >= 31) listOf("rotated.jpg", "rotated.heic") else listOf("rotated.jpg")
        for (name in names) {
        val attachment = imported(name)
        val bitmap = requireNotNull(decodeAttachmentPreview(attachment.base64Data, 80, 80))
        try {
            assertTrue("$name: ${bitmap.width} x ${bitmap.height}", bitmap.height > bitmap.width)
            assertTrue(bitmap.width <= 80 && bitmap.height <= 80)
            val top = bitmap.getPixel(bitmap.width / 2, bitmap.height / 4)
            val bottom = bitmap.getPixel(bitmap.width / 2, bitmap.height * 3 / 4)
            assertTrue(android.graphics.Color.red(top) > android.graphics.Color.blue(top))
            assertTrue(android.graphics.Color.blue(bottom) > android.graphics.Color.red(bottom))
        } finally { bitmap.recycle() }
        }
    }

    @Test fun pdfPagesRenderAndCorruptFilesRemainExportable() = runBlocking {
        val bytes = ByteArrayOutputStream().use { output ->
            val pdf = PdfDocument()
            try {
                repeat(2) { index ->
                    val page = pdf.startPage(PdfDocument.PageInfo.Builder(160, 96, index + 1).create())
                    page.canvas.drawColor(android.graphics.Color.WHITE); pdf.finishPage(page)
                }
                pdf.writeTo(output)
            } finally { pdf.close() }
            output.toByteArray()
        }
        val attachment = VaultAttachment("pages", resolveAttachmentMime("pages", null, bytes), bytes.size.toLong(), Base64.encodeToString(bytes, Base64.NO_WRAP))
        for (page in 0..1) {
            val (bitmap, count) = renderPdfAttachmentPage(attachment, page, context)
            assertEquals(2, count); assertNotNull(bitmap); bitmap?.recycle()
        }
        assertArrayEquals(bytes, exported(attachment))
        val corrupt = attachment.copy(base64Data = Base64.encodeToString("%PDF-broken".toByteArray(), Base64.NO_WRAP))
        assertNull(renderPdfAttachmentPage(corrupt, 0, context).first)
        assertArrayEquals("%PDF-broken".toByteArray(), exported(corrupt))
    }

    @Test fun officeTextAndUnknownFilesKeepEveryOriginalByte() = runBlocking {
        for (name in listOf("work.docx", "notes.txt", "page.tiff", "drawing.svg", "unknown.bin")) {
            val attachment = imported(name)
            assertEquals(name, attachment.name)
            assertArrayEquals(name, original(name), exported(attachment))
        }
        assertEquals("Offline format check\nSecond line", recognizeAttachmentText(context, imported("notes.txt")))
    }

    @Test fun mixedAndUnsupportedImageDocumentsExportAsLosslessZip() = runBlocking {
        val attachments = listOf(imported("page.png"), imported("page.tiff"), imported("work.docx"))
        val item = VaultItem("Mixed QA", VaultCategory.DOCUMENT, "", "", attachments = attachments)
        assertTrue(documentDownloadName(listOf(item)).endsWith(".zip"))
        val bytes = ByteArrayOutputStream().use { output -> writeDocumentDownload(context, listOf(item), output); output.toByteArray() }
        java.util.zip.ZipInputStream(bytes.inputStream()).use { zip ->
            for (attachment in attachments) {
                assertEquals(attachment.name, zip.nextEntry!!.name)
                assertArrayEquals(original(attachment.name), zip.readBytes())
                zip.closeEntry()
            }
            assertNull(zip.nextEntry)
        }
    }

    private fun exported(attachment: VaultAttachment): ByteArray = ByteArrayOutputStream().use { output ->
        writeDocumentDownload(context, listOf(VaultItem(title = "Format QA", detail = "", category = VaultCategory.DOCUMENT,
            time = "", attachments = listOf(attachment))), output)
        output.toByteArray()
    }
}

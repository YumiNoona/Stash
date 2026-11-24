package com.lockr.app

import org.junit.Assert.*
import org.junit.Test

class AttachmentFormatsTest {
    @Test fun commonExtensionsWorkWithMissingOrGenericProviderTypes() {
        mapOf("PNG" to "image/png", "jpg" to "image/jpeg", "jpeg" to "image/jpeg",
            "heic" to "image/heic", "heif" to "image/heif", "avif" to "image/avif",
            "pdf" to "application/pdf", "webp" to "image/webp", "gif" to "image/gif",
            "bmp" to "image/bmp", "tiff" to "image/tiff", "txt" to "text/plain",
            "csv" to "text/csv", "json" to "application/json").forEach { (extension, mime) ->
            assertEquals(mime, resolveAttachmentMime("work.$extension", null))
            assertEquals(mime, resolveAttachmentMime("work.$extension", "application/octet-stream"))
        }
    }

    @Test fun headersCorrectWrongNamesAndMimeLabels() {
        assertEquals("image/png", resolveAttachmentMime("camera.img", "application/pdf", byteArrayOf(137.toByte(),80,78,71,13,10,26,10)))
        assertEquals("image/jpeg", resolveAttachmentMime("photo", null, byteArrayOf(255.toByte(),216.toByte(),255.toByte())))
        val corrected = resolveAttachmentMime("scan.jpg", "image/jpeg", "\n%PDF-1.7".toByteArray())
        assertEquals("application/pdf", corrected)
        assertTrue(isPdfAttachment(VaultAttachment("scan.jpg", corrected, 8, "AA==")))
        assertEquals("image/webp", resolveAttachmentMime("unknown", null, "RIFF0000WEBP".toByteArray()))
    }

    @Test fun modernContainersUseMajorAndCompatibleBrandsWithoutMislabelingVideo() {
        fun box(major: String, compatible: String) = byteArrayOf(0,0,0,24) + "ftyp${major}0000$compatible".toByteArray()
        assertEquals("image/heif", detectedImageOrPdfMime(box("heic", "mif1")))
        assertEquals("image/avif", detectedImageOrPdfMime(box("mif1", "avif")))
        assertNull(detectedImageOrPdfMime(box("mp42", "isom")))
        assertNull(detectedImageOrPdfMime(byteArrayOf(0,0,0,8) + "ftypavif".toByteArray()))
    }

    @Test fun providerAliasesAndWildcardsAreNormalized() {
        assertEquals("image/jpeg", resolveAttachmentMime("photo", "IMAGE/JPG; charset=binary"))
        assertEquals("application/octet-stream", resolveAttachmentMime("disk.img", "image/*"))
        assertEquals("application/octet-stream", resolveAttachmentMime("unknown", null))
        assertEquals("application/vnd.custom", resolveAttachmentMime("unknown", "application/vnd.custom"))
    }

    @Test fun textPreviewHandlesUnicodeBomAndCapsLargeFiles() {
        assertEquals("hello", decodeTextPreview(byteArrayOf(0xef.toByte(),0xbb.toByte(),0xbf.toByte()) + "hello".toByteArray()))
        assertEquals("hello", decodeTextPreview(byteArrayOf(0xff.toByte(),0xfe.toByte()) + "hello".toByteArray(Charsets.UTF_16LE)))
        assertEquals("hello", decodeTextPreview(byteArrayOf(0xfe.toByte(),0xff.toByte()) + "hello".toByteArray(Charsets.UTF_16BE)))
        val preview = decodeTextPreview(ByteArray(MAX_TEXT_PREVIEW_CHARACTERS * 5) { 65 })
        assertTrue(preview.startsWith("A".repeat(100)))
        assertTrue(preview.endsWith("[Preview truncated. Download the original for the full text.]"))
        assertTrue(preview.length < MAX_TEXT_PREVIEW_CHARACTERS + 100)
    }

    @Test fun legacyAttachmentsUseNamesForPreviewAndKeepOfficeTypes() {
        fun file(name: String) = VaultAttachment(name, "application/octet-stream", 1, "AA==")
        assertTrue(isImageAttachment(file("page.HEIF")))
        assertTrue(isPdfAttachment(file("page.PDF")))
        assertTrue(isTextAttachment(file("notes.json")))
        assertFalse(isTextAttachment(file("file.docx")))
        assertEquals("application/vnd.openxmlformats-officedocument.wordprocessingml.document", attachmentMime(file("file.docx")))
    }
}

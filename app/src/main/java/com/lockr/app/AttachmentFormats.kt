package com.lockr.app

import java.util.Locale

private val fileMimeTypes = mapOf(
    "jpg" to "image/jpeg", "jpeg" to "image/jpeg", "jpe" to "image/jpeg", "jfif" to "image/jpeg",
    "png" to "image/png", "gif" to "image/gif", "webp" to "image/webp", "bmp" to "image/bmp",
    "heic" to "image/heic", "heif" to "image/heif", "avif" to "image/avif",
    "tif" to "image/tiff", "tiff" to "image/tiff", "svg" to "image/svg+xml", "dng" to "image/x-adobe-dng",
    "pdf" to "application/pdf", "txt" to "text/plain", "text" to "text/plain", "md" to "text/markdown",
    "csv" to "text/csv", "tsv" to "text/tab-separated-values", "json" to "application/json",
    "xml" to "application/xml", "html" to "text/html", "htm" to "text/html", "log" to "text/plain",
    "doc" to "application/msword", "docx" to "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "xls" to "application/vnd.ms-excel", "xlsx" to "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "ppt" to "application/vnd.ms-powerpoint", "pptx" to "application/vnd.openxmlformats-officedocument.presentationml.presentation",
    "odt" to "application/vnd.oasis.opendocument.text", "ods" to "application/vnd.oasis.opendocument.spreadsheet",
    "rtf" to "application/rtf", "zip" to "application/zip", "7z" to "application/x-7z-compressed"
)

/** Providers sometimes report a photo or PDF as octet-stream; trust a recognized header first. */
internal fun resolveAttachmentMime(name: String, declared: String?, header: ByteArray = byteArrayOf()): String {
    detectedImageOrPdfMime(header)?.let { return it }
    val supplied = declared?.substringBefore(';')?.trim()?.lowercase(Locale.ROOT)
    val normalized = when (supplied) {
        "image/jpg", "image/pjpeg" -> "image/jpeg"
        "image/x-png" -> "image/png"
        "image/x-ms-bmp" -> "image/bmp"
        "application/x-pdf" -> "application/pdf"
        else -> supplied?.takeIf { it.matches(Regex("[a-z0-9!#$&^_.+-]+/[a-z0-9!#$&^_.+-]+")) }
    }
    if (normalized != null && normalized !in setOf("application/octet-stream", "binary/octet-stream", "application/x-octet-stream")) return normalized
    return fileMimeTypes[name.substringAfterLast('.', "").lowercase(Locale.ROOT)] ?: normalized ?: "application/octet-stream"
}

internal fun detectedImageOrPdfMime(bytes: ByteArray): String? {
    fun at(offset: Int, value: String) = offset >= 0 && offset + value.length <= bytes.size &&
        value.indices.all { bytes[offset + it].toInt() and 255 == value[it].code }
    fun starts(vararg value: Int) = bytes.size >= value.size && value.indices.all { bytes[it].toInt() and 255 == value[it] }
    return when {
        starts(0xff, 0xd8, 0xff) -> "image/jpeg"
        starts(137, 80, 78, 71, 13, 10, 26, 10) -> "image/png"
        at(0, "GIF87a") || at(0, "GIF89a") -> "image/gif"
        at(0, "RIFF") && at(8, "WEBP") -> "image/webp"
        at(0, "BM") -> "image/bmp"
        at(0, "II*") || starts(77, 77, 0, 42) -> "image/tiff"
        // PDF readers allow a short prefix before the file header.
        (0..minOf(1024, bytes.size - 5)).any { at(it, "%PDF-") } -> "application/pdf"
        at(4, "ftyp") && bytes.size >= 16 -> {
            val size = (0..3).fold(0L) { value, index -> (value shl 8) or (bytes[index].toLong() and 255) }
            val end = minOf(size, bytes.size.toLong(), 512L).toInt()
            val brands = (listOf(8) + (16 until end step 4)).filter { it + 4 <= end }
            when {
                brands.any { at(it, "avif") || at(it, "avis") } -> "image/avif"
                brands.any { offset -> listOf("heic", "heix", "hevc", "hevx", "mif1", "msf1").any { at(offset, it) } } -> "image/heif"
                else -> null
            }
        }
        else -> null
    }
}

internal fun attachmentMime(attachment: VaultAttachment) = resolveAttachmentMime(attachment.name, attachment.mimeType)
internal fun isImageAttachment(attachment: VaultAttachment) = attachmentMime(attachment).startsWith("image/")
internal fun isTextAttachment(attachment: VaultAttachment): Boolean {
    val mime = attachmentMime(attachment)
    return mime.startsWith("text/") || mime in setOf("application/json", "application/xml")
}

internal fun previewUnavailableMessage(attachment: VaultAttachment): String = when {
    isPdfAttachment(attachment) -> "This PDF cannot be previewed. It may be locked or damaged. Your original file is still available to share or download."
    isImageAttachment(attachment) -> "This image cannot be previewed on this device. Your original file is still available to share or download."
    else -> "Original file saved. Share or download it to open in a compatible app."
}

internal const val MAX_TEXT_PREVIEW_CHARACTERS = 120_000

/** Plain text only: HTML, XML and Markdown are never executed or loaded in a WebView. */
internal fun decodeTextPreview(bytes: ByteArray): String {
    val limit = minOf(bytes.size, MAX_TEXT_PREVIEW_CHARACTERS * 4)
    val charset = when {
        bytes.size >= 2 && bytes[0] == 0xff.toByte() && bytes[1] == 0xfe.toByte() -> Charsets.UTF_16LE
        bytes.size >= 2 && bytes[0] == 0xfe.toByte() && bytes[1] == 0xff.toByte() -> Charsets.UTF_16BE
        else -> Charsets.UTF_8
    }
    val decoded = String(bytes, 0, limit, charset).removePrefix("\uFEFF")
    return decoded.take(MAX_TEXT_PREVIEW_CHARACTERS) +
        if (bytes.size > limit || decoded.length > MAX_TEXT_PREVIEW_CHARACTERS) "\n\n[Preview truncated. Download the original for the full text.]" else ""
}

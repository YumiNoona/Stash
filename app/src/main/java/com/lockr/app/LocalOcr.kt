package com.lockr.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Base64
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val MAX_OCR_CHARACTERS = 120_000
private const val MAX_PDF_OCR_PAGES = 20

/** Uses the bundled Latin recognizer: the model is included in the APK and does not need downloading. */
suspend fun recognizeAttachmentText(context: Context, attachment: VaultAttachment): String = withContext(Dispatchers.IO) {
    val bytes = try {
        Base64.decode(attachment.base64Data, Base64.DEFAULT)
    } catch (_: IllegalArgumentException) {
        throw IOException("This attachment could not be decoded.")
    }
    try {
        when {
            attachment.mimeType.equals("application/pdf", ignoreCase = true) || attachment.name.endsWith(".pdf", true) ->
                recognizePdf(context, bytes)
            attachment.mimeType.startsWith("image/", ignoreCase = true) -> {
                val bitmap = decodeAttachmentPreview(attachment.base64Data, 1800, 2400)
                    ?: throw IOException("This image could not be opened for text recognition.")
                try {
                    recognizeBitmap(bitmap)
                } finally {
                    bitmap.recycle()
                }
            }
            attachment.mimeType.startsWith("text/", ignoreCase = true) || attachment.name.endsWith(".txt", true) ->
                bytes.toString(Charsets.UTF_8).take(MAX_OCR_CHARACTERS)
            else -> ""
        }.trim().take(MAX_OCR_CHARACTERS)
    } finally {
        bytes.fill(0)
    }
}

private suspend fun recognizePdf(context: Context, bytes: ByteArray): String {
    val temporaryPdf = File.createTempFile("stash-ocr-", ".pdf", context.cacheDir)
    try {
        temporaryPdf.outputStream().use { it.write(bytes) }
        val descriptor = ParcelFileDescriptor.open(temporaryPdf, ParcelFileDescriptor.MODE_READ_ONLY)
        descriptor.use { parcel ->
            PdfRenderer(parcel).use { renderer ->
                val output = StringBuilder()
                val pagesToRead = minOf(renderer.pageCount, MAX_PDF_OCR_PAGES)
                for (index in 0 until pagesToRead) {
                    val page = renderer.openPage(index)
                    try {
                        val scale = minOf(1800f / page.width, 2400f / page.height, 2f)
                        val bitmap = Bitmap.createBitmap(
                            (page.width * scale).toInt().coerceAtLeast(1),
                            (page.height * scale).toInt().coerceAtLeast(1),
                            Bitmap.Config.ARGB_8888
                        )
                        try {
                            bitmap.eraseColor(android.graphics.Color.WHITE)
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            val text = recognizeBitmap(bitmap)
                            if (text.isNotBlank()) {
                                if (output.isNotEmpty()) output.append("\n\n")
                                output.append("Page ${index + 1}\n").append(text)
                            }
                        } finally {
                            bitmap.recycle()
                        }
                    } finally {
                        page.close()
                    }
                    if (output.length >= MAX_OCR_CHARACTERS) break
                }
                if (renderer.pageCount > pagesToRead) {
                    output.append("\n\nOnly the first $pagesToRead pages were indexed.")
                }
                return output.toString().take(MAX_OCR_CHARACTERS)
            }
        }
    } finally {
        temporaryPdf.delete()
    }
}

private suspend fun recognizeBitmap(bitmap: Bitmap): String {
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    return try {
        recognizer.process(InputImage.fromBitmap(bitmap, 0)).awaitTask().text
    } finally {
        recognizer.close()
    }
}

private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        if (!continuation.isActive) return@addOnCompleteListener
        if (task.isSuccessful) {
            continuation.resume(task.result)
        } else {
            continuation.resumeWithException(task.exception ?: IOException("Text recognition failed."))
        }
    }
}

fun suggestedVaultCategory(filename: String, recognizedText: String): VaultCategory {
    val sample = "$filename $recognizedText".lowercase()
    val paymentHints = listOf("credit card", "debit card", "visa", "mastercard", "american express", "card number", "id card", "identity card")
    return if (paymentHints.any(sample::contains)) VaultCategory.CARD else VaultCategory.DOCUMENT
}

fun suggestedDocumentGroup(filename: String, recognizedText: String): String {
    val sample = "$filename $recognizedText".lowercase()
    return when {
        listOf("passport", "identity", "national id", "driver license", "driving licence", "aadhaar", "aadhar", "uidai", "government of india").any(sample::contains) -> "Identity"
        listOf("prescription", "medical", "hospital", "doctor", "clinic").any(sample::contains) -> "Medical"
        listOf("insurance", "policy number", "premium").any(sample::contains) -> "Insurance"
        listOf("invoice", "bill", "utility", "electricity", "water statement").any(sample::contains) -> "Bills"
        else -> "Other"
    }
}

fun suggestedCardType(recognizedText: String): String = when {
    recognizedText.contains("debit", ignoreCase = true) -> "Debit"
    recognizedText.contains("credit", ignoreCase = true) -> "Credit"
    recognizedText.contains("identity", ignoreCase = true) || recognizedText.contains("id card", ignoreCase = true) -> "ID Card"
    else -> "Other"
}

fun suggestedCardLastFour(text: String): String {
    val candidate = Regex("(?<!\\d)(?:\\d[ -]?){12,19}(?!\\d)").find(text)?.value.orEmpty()
    return candidate.filter(Char::isDigit).takeLast(4)
}

fun suggestedItemTitle(filename: String, recognizedText: String): String {
    val cleanFileName = filename.substringBeforeLast('.', filename)
        .replace(Regex("[_-]+"), " ").trim()
    if (cleanFileName.isNotBlank() && cleanFileName.any(Char::isLetter) && !cleanFileName.matches(Regex("(?i)(image|scan|document|attachment) ?\\d*"))) {
        return cleanFileName.take(60)
    }
    return recognizedText.lineSequence().map(String::trim).firstOrNull { it.length in 3..60 && it.any(Char::isLetter) } ?: "Imported document"
}

/** Prevent OCR text search from indexing a likely PAN or a labeled verification code. */
fun redactPaymentSecrets(text: String): String {
    val labeledCode = Regex("(?i)\\b(CVV|CVC|CID|security code)\\s*[:#-]?\\s*\\d{3,4}\\b")
        .replace(text) { "${it.groupValues[1]} [hidden]" }
    return Regex("(?<!\\d)(?:\\d[ -]?){12,19}(?!\\d)")
        .replace(labeledCode, "[card number hidden]")
}

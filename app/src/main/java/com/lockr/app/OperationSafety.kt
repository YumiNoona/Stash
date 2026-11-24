package com.lockr.app

import java.io.IOException
import java.io.OutputStream

/** Destination failures must reach the caller's error UI, including providers returning null. */
internal fun writeExportDestination(open: () -> OutputStream?, write: (OutputStream) -> Unit) {
    (open() ?: throw IOException("The selected destination is unavailable.")).use(write)
}

internal fun cardExportText(item: VaultItem, includePrivate: Boolean): String = buildString {
    appendLine(item.title)
    val fields = visibleMetadata(item, includePrivate)
    identityType(item)?.let { type ->
        appendLine("Type: ${type.label}")
        appendLine("Number: ${identityNumberText(type, item.metadata["idNumber"].orEmpty(), includePrivate)}")
        if (includePrivate) identityFields(type).filter { it.key != "idNumber" }.forEach { field ->
            fields[field.key]?.takeIf(String::isNotBlank)?.let { appendLine("${field.label.removeSuffix(" *")}: $it") }
        }
        appendLine("Personal record stored in Stash")
        return@buildString
    }
    listOf("issuer" to "Issuer", "cardType" to "Type", "network" to "Network",
        "cardholder" to "Cardholder", "expiry" to "Expiry").forEach { (key, label) ->
        fields[key]?.takeIf(String::isNotBlank)?.let { appendLine("$label: $it") }
    }
    val number = if (includePrivate) fields["cardNumber"].orEmpty()
        else "•••• •••• •••• ${item.lastFour.ifBlank { item.metadata["cardNumber"].orEmpty().takeLast(4) }}"
    appendLine("Number: $number")
    // Verification codes never belong in a readable export, even with private fields enabled.
}

internal class DraftImportGate {
    private var generation = 0L
    fun begin(): Long = ++generation
    fun current(token: Long): Boolean = generation == token
    fun invalidate() { generation++ }
}

internal fun remainingDocumentSlots(existing: Int): Int = (MAX_VAULT_ATTACHMENT_COUNT - existing).coerceAtLeast(0)

internal fun validateDocumentPages(pages: List<VaultAttachment>) {
    require(pages.isNotEmpty()) { "Keep at least one page." }
    require(pages.size <= MAX_VAULT_ATTACHMENT_COUNT) { "A document can contain up to 20 files." }
    require(pages.all { it.byteCount in 0..MAX_VAULT_ATTACHMENT_BYTES } &&
        pages.sumOf { it.byteCount } <= MAX_VAULT_TOTAL_ATTACHMENT_BYTES) { "The document must fit within 24 MB." }
}

internal fun normalizedFolderName(value: String): String = value.trim().replace(Regex("\\s+"), " ")

internal fun folderNameError(value: String, folders: List<String>, original: String? = null): String? {
    val name = normalizedFolderName(value)
    return when {
        name.isBlank() -> "Enter a folder name."
        name.length > 40 || name.any(Char::isISOControl) -> "Use up to 40 characters without control characters."
        listOf("All folders", "Unfiled").any { name.equals(it, true) } -> "Choose another name."
        folders.any { it != original && it.equals(name, true) } -> "That folder already exists."
        else -> null
    }
}

/** Read at most the declared limit before allocating an oversized vault or image. */
internal fun readBoundedBytes(input: java.io.InputStream, maximumBytes: Int): ByteArray {
    require(maximumBytes > 0)
    val output = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(16 * 1024)
    var total = 0
    fun tooLarge(): Nothing {
        val limit = if (maximumBytes % (1024 * 1024) == 0) "${maximumBytes / (1024 * 1024)} MB" else "$maximumBytes bytes"
        throw IOException("The selected file is larger than $limit")
    }
    try {
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            if (count == 0) {
                val single = input.read()
                if (single < 0) break
                if (total == maximumBytes) tooLarge()
                output.write(single); total++
            } else {
                if (count > maximumBytes - total) tooLarge()
                output.write(buffer, 0, count); total += count
            }
        }
        return output.toByteArray()
    } finally { buffer.fill(0) }
}

/** A delayed clear owns one copy, even when the same text is copied again. */
internal fun scheduleClipboardClear(token: String, currentToken: () -> String?, clear: () -> Unit,
                                    schedule: (Long, () -> Unit) -> Unit) {
    schedule(30_000L) { if (currentToken() == token) clear() }
}

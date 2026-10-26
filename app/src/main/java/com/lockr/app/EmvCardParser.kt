package com.lockr.app

/** A small bounded BER-TLV reader for the limited data needed by card import. */
internal fun emvTag(data: ByteArray, target: Int): ByteArray? {
    fun walk(start: Int, end: Int, depth: Int): ByteArray? {
        if (depth > 8) return null
        var cursor = start
        while (cursor < end) {
            val first = data[cursor++].toInt() and 0xFF
            var tag = first
            if (first and 0x1F == 0x1F) {
                var count = 0
                while (cursor < end && count++ < 3) {
                    val next = data[cursor++].toInt() and 0xFF
                    tag = (tag shl 8) or next
                    if (next and 0x80 == 0) break
                }
            }
            if (cursor >= end) return null
            val lengthByte = data[cursor++].toInt() and 0xFF
            val length = if (lengthByte and 0x80 == 0) lengthByte else {
                val count = lengthByte and 0x7F
                if (count !in 1..2 || cursor + count > end) return null
                var parsed = 0
                repeat(count) { parsed = (parsed shl 8) or (data[cursor++].toInt() and 0xFF) }
                parsed
            }
            if (length < 0 || cursor + length > end) return null
            if (tag == target) return data.copyOfRange(cursor, cursor + length)
            if (first and 0x20 != 0) {
                walk(cursor, cursor + length, depth + 1)?.let { return it }
            }
            cursor += length
        }
        return null
    }
    return walk(0, data.size, 0)
}

internal fun emvPdolLength(pdol: ByteArray): Int? {
    var cursor = 0
    var total = 0
    while (cursor < pdol.size) {
        val first = pdol[cursor++].toInt() and 0xFF
        if (first and 0x1F == 0x1F) {
            var count = 0
            while (cursor < pdol.size && count++ < 3) {
                if (pdol[cursor++].toInt() and 0x80 == 0) break
            }
        }
        if (cursor >= pdol.size) return null
        total += pdol[cursor++].toInt() and 0xFF
        if (total > 96) return null
    }
    return total
}

internal fun emvAfl(gpo: ByteArray): ByteArray? {
    emvTag(gpo, 0x94)?.let { return it }
    if (gpo.size < 4 || gpo[0].toInt() and 0xFF != 0x80) return null
    val length = gpo[1].toInt() and 0xFF
    if (length + 2 > gpo.size || length < 2) return null
    return gpo.copyOfRange(4, length + 2)
}

internal fun emvRecords(afl: ByteArray): List<Pair<Int, Int>> {
    if (afl.size % 4 != 0) return emptyList()
    val records = mutableListOf<Pair<Int, Int>>()
    for (offset in afl.indices step 4) {
        val sfi = (afl[offset].toInt() and 0xFF) shr 3
        val first = afl[offset + 1].toInt() and 0xFF
        val last = afl[offset + 2].toInt() and 0xFF
        if (sfi !in 1..30 || first !in 1..16 || last !in first..16) continue
        for (record in first..last) {
            if (records.size >= 20) return records
            records += sfi to record
        }
    }
    return records
}

internal fun emvCardDraft(aid: ByteArray, payloads: List<ByteArray>): CardDraft? {
    val pan = payloads.firstNotNullOfOrNull { payload ->
        emvTag(payload, 0x5A)?.toHex()?.trimEnd('F')?.takeIf(::isPlausibleCardNumber)
    } ?: payloads.firstNotNullOfOrNull { payload ->
        emvTag(payload, 0x57)?.toHex()?.substringBefore('D')?.takeIf(::isPlausibleCardNumber)
    } ?: return null
    val expiryTag = payloads.firstNotNullOfOrNull { emvTag(it, 0x5F24) }
    val expiry = if (expiryTag != null && expiryTag.size >= 2) {
        val hex = expiryTag.toHex()
        val month = hex.substring(2, 4)
        if (month.toIntOrNull() in 1..12) "$month/${hex.substring(0, 2)}" else ""
    } else payloads.firstNotNullOfOrNull { emvTag(it, 0x57) }
        ?.toHex()?.substringAfter('D', "")?.take(4)?.let { yyMm ->
            if (yyMm.length == 4 && yyMm.substring(2).toIntOrNull() in 1..12)
                "${yyMm.substring(2)}/${yyMm.substring(0, 2)}" else ""
        }.orEmpty()
    val holder = payloads.firstNotNullOfOrNull { emvTag(it, 0x5F20) }
        ?.toString(Charsets.US_ASCII)?.trim()?.filter { it.isLetter() || it == ' ' || it == '-' }
        ?.take(60).orEmpty()
    val network = cardNetwork(pan).ifBlank { networkFromAid(aid) }
    return CardDraft(
        title = if (network.isBlank()) "Contactless card" else "$network card",
        cardNumber = pan, cardholder = holder, expiry = expiry,
        network = network
    )
}

private fun networkFromAid(aid: ByteArray): String = when {
    aid.toHex().startsWith("A000000003") -> "Visa"
    aid.toHex().startsWith("A000000004") -> "Mastercard"
    aid.toHex().startsWith("A000000025") -> "Amex"
    aid.toHex().startsWith("A000000065") -> "JCB"
    aid.toHex().startsWith("A000000152") -> "Discover"
    else -> ""
}

internal fun ByteArray.toHex(): String = joinToString("") { "%02X".format(it.toInt() and 0xFF) }

package com.lockr.app

import android.nfc.Tag
import android.nfc.tech.IsoDep
import java.io.IOException

/** Reads only card identity fields exposed by an ISO-DEP contactless card. Never requests a PIN or CVV. */
internal fun readSupportedNfcCard(tag: Tag): CardDraft {
    val iso = IsoDep.get(tag) ?: throw IOException("This tap is not a supported contactless card.")
    iso.use { channel ->
        channel.connect()
        channel.timeout = 3500
        val ppse = channel.exchange(selectApdu("2PAY.SYS.DDF01".toByteArray(Charsets.US_ASCII)))
            ?: throw IOException("This card does not expose a readable payment application.")
        val aid = emvTag(ppse, 0x4F)
            ?: throw IOException("No supported payment application was found. Try scanning the card.")
        val selected = channel.exchange(selectApdu(aid))
            ?: throw IOException("The card did not allow its application to be opened.")
        val pdol = emvTag(selected, 0x9F38)
        val pdolLength = pdol?.let(::emvPdolLength)
            ?: if (pdol == null) 0 else throw IOException("This card requests unsupported NFC data.")
        val template = byteArrayOf(0x83.toByte(), pdolLength.toByte()) + ByteArray(pdolLength)
        val gpo = channel.exchange(byteArrayOf(0x80.toByte(), 0xA8.toByte(), 0x00, 0x00,
            template.size.toByte()) + template + byteArrayOf(0x00))
            ?: throw IOException("This card did not expose readable details. Try the camera instead.")
        val afl = emvAfl(gpo)
            ?: throw IOException("This card did not list readable details. Try the camera instead.")
        val payloads = mutableListOf(selected, gpo)
        for ((sfi, record) in emvRecords(afl)) {
            val command = byteArrayOf(0x00, 0xB2.toByte(), record.toByte(),
                ((sfi shl 3) or 4).toByte(), 0x00)
            channel.exchange(command)?.let(payloads::add)
        }
        return emvCardDraft(aid, payloads)
            ?: throw IOException("This card does not expose a usable number. Scan it or enter details manually.")
    }
}

private fun selectApdu(name: ByteArray): ByteArray =
    byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00, name.size.toByte()) + name + byteArrayOf(0x00)

private fun IsoDep.exchange(command: ByteArray): ByteArray? {
    var response = transceive(command)
    if (response.size < 2) return null
    var status1 = response[response.lastIndex - 1].toInt() and 0xFF
    var status2 = response.last().toInt() and 0xFF
    if (status1 == 0x6C) {
        response = transceive(command.copyOf().apply { this[lastIndex] = status2.toByte() })
        if (response.size < 2) return null
        status1 = response[response.lastIndex - 1].toInt() and 0xFF
        status2 = response.last().toInt() and 0xFF
    }
    if (status1 == 0x61) {
        val first = response.copyOfRange(0, response.size - 2)
        val next = transceive(byteArrayOf(0x00, 0xC0.toByte(), 0x00, 0x00, status2.toByte()))
        if (next.size < 2 || next[next.lastIndex - 1] != 0x90.toByte() || next.last() != 0x00.toByte()) return null
        return first + next.copyOfRange(0, next.size - 2)
    }
    return if (status1 == 0x90 && status2 == 0x00) response.copyOfRange(0, response.size - 2) else null
}

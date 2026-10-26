package com.lockr.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CardImportTest {
    @Test fun ocrSuggestsOnlyPlausibleNumbersAndRedactsSearchText() {
        val draft = recognizeCardFields("VISA\n4111 1111 1111 1111\n12/27\nCVV 123")
        assertEquals("4111111111111111", draft.cardNumber)
        assertEquals("12/27", draft.expiry)
        assertEquals("Visa", draft.network)
        assertFalse(draft.ocrText.contains("4111"))
        assertFalse(draft.ocrText.contains("123"))
        assertFalse(isPlausibleCardNumber("4111111111111112"))
    }

    @Test fun nfcParserReadsOnlyRequestedCardFields() {
        val aid = hex("A0000000031010")
        val record = hex("70215A0841111111111111115F24032712315F20084A414E4520444F459F1003010203")
        val draft = emvCardDraft(aid, listOf(record))
        assertEquals("4111111111111111", draft?.cardNumber)
        assertEquals("12/27", draft?.expiry)
        assertEquals("JANE DOE", draft?.cardholder)
        assertEquals("Visa", draft?.network)
        assertEquals("Visa card", draft?.title)
        assertEquals(listOf(1 to 1), emvRecords(emvAfl(hex("8006123408010100"))!!))
    }

    @Test fun malformedOrMissingCardDataIsRejected() {
        assertNull(emvTag(hex("5A084111"), 0x5A))
        assertNull(emvCardDraft(hex("A0000000031010"), listOf(hex("70035F2400"))))
        assertTrue(emvRecords(hex("08050100")).isEmpty())
    }

    private fun hex(value: String): ByteArray = value.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}

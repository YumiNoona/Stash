package com.lockr.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ReadableExportFilterTest {
    @Test
    fun cvcNeverAppearsInReadableMetadata() {
        val item = VaultItem("Card", VaultCategory.CARD, "Credit", "", metadata = mapOf(
            "cardCvc" to "123", "CardCVC" to "456", "cvv" to "789",
            "securityCode" to "000", "cardNumber" to "4111111111111111"))
        val publicFields = visibleMetadata(item, includePrivate = false)
        val privateFields = visibleMetadata(item, includePrivate = true)
        assertEquals(emptyMap<String, String>(), publicFields)
        assertEquals("4111111111111111", privateFields["cardNumber"])
        assertFalse(privateFields.keys.any { it.lowercase().contains("cvc") || it.lowercase().contains("cvv") || it.lowercase() == "securitycode" })
    }

    @Test
    fun extractedIdentityFieldsNeedPrivateExportOptIn() {
        val item = VaultItem("Identity document", VaultCategory.DOCUMENT, "", "", metadata = mapOf(
            "idNumber" to "synthetic-id", "address" to "synthetic-address", "extractedSnippet" to "synthetic-snippet"))
        assertEquals(emptyMap<String, String>(), visibleMetadata(item, includePrivate = false))
        assertEquals(3, visibleMetadata(item, includePrivate = true).size)
    }
}

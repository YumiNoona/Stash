package com.lockr.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VaultLogicTest {
    @Test
    fun redactsCardNumbersAndLabeledSecurityCodesFromOcr() {
        val text = "Card number 4111 1111 1111 1111\nCVV: 123"

        val safeText = redactPaymentSecrets(text)

        assertFalse(safeText.contains("4111"))
        assertFalse(safeText.contains("123"))
        assertTrue(safeText.contains("[card number hidden]"))
        assertTrue(safeText.contains("CVV [hidden]"))
    }

    @Test
    fun suggestsCardEndingWithoutKeepingFullNumber() {
        assertEquals("1111", suggestedCardLastFour("4111 1111 1111 1111"))
        assertEquals("", suggestedCardLastFour("No card number here"))
    }

    @Test
    fun suggestsCategoriesAndReadableTitlesFromImportedFiles() {
        assertEquals(VaultCategory.CARD, suggestedVaultCategory("front.jpg", "Visa credit card"))
        assertEquals(VaultCategory.DOCUMENT, suggestedVaultCategory("passport.pdf", "Passport"))
        assertEquals("Identity", suggestedDocumentGroup("passport.pdf", ""))
        assertEquals("family passport", suggestedItemTitle("family_passport.pdf", ""))
    }

    @Test
    fun searchIncludesLocalDocumentTextButNeverPasswordValues() {
        val document = VaultItem(
            title = "Passport",
            category = VaultCategory.DOCUMENT,
            detail = "",
            time = "today",
            metadata = mapOf("ocrText" to "sample searchable phrase")
        )
        val password = VaultItem(
            title = "Mail account",
            category = VaultCategory.PASSWORD,
            detail = "secret-value-should-not-be-indexed",
            time = "today",
            metadata = mapOf("password" to "another-secret-value")
        )

        assertTrue(searchableText(document).contains("sample searchable phrase"))
        assertFalse(searchableText(password).contains("secret-value-should-not-be-indexed"))
        assertFalse(searchableText(password).contains("another-secret-value"))
    }
}

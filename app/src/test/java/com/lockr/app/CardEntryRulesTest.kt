package com.lockr.app

import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CardEntryRulesTest {
    private val now = YearMonth.of(2026, 10)

    @Test
    fun formatsAndCapsPaymentNumbersAndExpiry() {
        assertEquals("4111 1111 1111 1111", formatPaymentCardNumber("41111111111111119", "Visa"))
        assertEquals("3782 8224 6310 005", formatPaymentCardNumber("3782822463100059", "Amex"))
        assertEquals("12/27", formatCardExpiry("12-2027"))
        assertEquals("12/27", formatCardExpiry("1227"))
        assertEquals("1227", cardExpiryDigits("12/27"))
    }

    @Test
    fun paymentCardExplainsEveryMissingOrInvalidField() {
        val missing = validateCardEntry("", "Credit", "", "", "", now)
        assertEquals(4, missing.messages.size)
        assertEquals("Enter a 16-digit card number", missing.numberError)
        assertEquals("Choose a card network", missing.networkError)

        val warned = validateCardEntry("My card", "Debit", "2222 2222 2222 2222", "09/26", "Mastercard", now)
        assertNull(warned.numberError)
        assertNull(warned.expiryError)
        assertEquals(2, warned.warnings.size)
        assertTrue(warned.isValid)

        val invalid = validateCardEntry("My card", "Debit", "2222 2222 2222 2222", "52/36", "Mastercard", now, "12")
        assertEquals("Enter a valid expiry (MM/YY)", invalid.expiryError)
        assertEquals("Enter a 3-digit CVC, or leave it blank", invalid.cvcError)
        assertFalse(invalid.isValid)
    }

    @Test
    fun acceptsValidPaymentCardsAndOptionalIdDetails() {
        assertTrue(validateCardEntry("Travel card", "Credit", "4111 1111 1111 1111", "12/27", "Visa", now).isValid)
        assertTrue(validateCardEntry("Travel card", "Credit", "4111 1111 1111 1111", "12/27", "Visa", now, "123").isValid)
        assertTrue(validateCardEntry("Amex", "Credit", "3782 8224 6310 005", "12/27", "Amex", now).isValid)
        assertTrue(validateCardEntry("Amex", "Credit", "3782 8224 6310 005", "12/27", "Amex", now, "1234").isValid)
        assertTrue(validateCardEntry("Passport card", "ID Card", "A12345", "", "", now).isValid)
        assertNull(validateCardEntry("ID", "ID Card", "", "", "", now).numberError)
        assertEquals("RuPay", recognizeCardFields("RuPay 4111 1111 1111 1111").network)
    }
}

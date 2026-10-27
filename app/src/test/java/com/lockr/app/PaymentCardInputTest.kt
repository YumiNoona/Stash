package com.lockr.app

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Test

class PaymentCardInputTest {
    @Test
    fun typingAcrossEveryGroupKeepsDigitsInOrder() {
        var value = TextFieldValue("", TextRange(0))
        "1234567890123456".forEach { digit ->
            val cursor = value.selection.end
            value = normalizedPaymentCardValue(TextFieldValue(
                value.text.substring(0, cursor) + digit + value.text.substring(cursor),
                TextRange(cursor + 1)), "Visa")
        }
        assertEquals("1234567890123456", value.text)
        assertEquals(16, value.selection.end)
        val transformed = PaymentCardVisualTransformation(masked = false).filter(AnnotatedString(value.text))
        assertEquals("1234 5678 9012 3456", transformed.text.text)
        assertEquals(19, transformed.offsetMapping.originalToTransformed(16))
        assertEquals(4, transformed.offsetMapping.transformedToOriginal(5))
        assertEquals(5, transformed.offsetMapping.transformedToOriginal(6))
    }

    @Test
    fun pastedSpacesAndMidStringEditsPreserveCursor() {
        val pasted = normalizedPaymentCardValue(TextFieldValue("1234 5678 9012 3456", TextRange(7)), "Visa")
        assertEquals("1234567890123456", pasted.text)
        assertEquals(6, pasted.selection.end)
        val inserted = normalizedPaymentCardValue(TextFieldValue("123499567890123456", TextRange(6)), "Visa")
        assertEquals("1234995678901234", inserted.text)
        assertEquals(6, inserted.selection.end)
        val masked = PaymentCardVisualTransformation(masked = true).filter(AnnotatedString("12345"))
        assertEquals("•••• •", masked.text.text)
        assertEquals(5, masked.offsetMapping.transformedToOriginal(6))
    }
}

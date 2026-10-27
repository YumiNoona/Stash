package com.lockr.app

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/** Keep the editable value as digits. Spaces are display-only, so the IME cursor never enters a separator. */
internal fun normalizedPaymentCardValue(input: TextFieldValue, network: String): TextFieldValue {
    val digits = input.text.filter(Char::isDigit).take(paymentCardDigitLimit(network))
    fun cursorAt(offset: Int) = input.text.take(offset.coerceIn(0, input.text.length))
        .count(Char::isDigit).coerceAtMost(digits.length)
    return TextFieldValue(digits, TextRange(cursorAt(input.selection.start), cursorAt(input.selection.end)))
}

internal class PaymentCardVisualTransformation(private val masked: Boolean) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        val visible = digits.chunked(4).joinToString(" ") { group ->
            if (masked) "•".repeat(group.length) else group
        }
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val safe = offset.coerceIn(0, digits.length)
                return safe + if (safe == 0) 0 else (safe - 1) / 4
            }

            override fun transformedToOriginal(offset: Int): Int =
                visible.take(offset.coerceIn(0, visible.length)).count { it != ' ' }
                    .coerceIn(0, digits.length)
        }
        return TransformedText(AnnotatedString(visible), mapping)
    }
}

package com.lockr.app

import java.time.YearMonth

val CARD_NETWORKS = listOf("Visa", "Mastercard", "RuPay", "Amex", "Discover", "JCB", "UnionPay", "Other")

fun isPaymentCardType(cardType: String): Boolean = cardType == "Credit" || cardType == "Debit"

fun paymentCardDigitLimit(network: String): Int = if (network == "Amex") 15 else 16

fun formatPaymentCardNumber(input: String, network: String): String = input.filter(Char::isDigit)
    .take(paymentCardDigitLimit(network)).chunked(4).joinToString(" ")

fun cardExpiryDigits(input: String): String {
    val raw = input.filter(Char::isDigit)
    return if (raw.length >= 6 && raw.substring(2, 4) == "20") raw.take(2) + raw.takeLast(2)
        else raw.take(4)
}

fun formatCardExpiry(input: String): String {
    val digits = cardExpiryDigits(input)
    return if (digits.length > 2) "${digits.take(2)}/${digits.drop(2)}" else digits
}

data class CardEntryValidation(
    val titleError: String? = null,
    val numberError: String? = null,
    val expiryError: String? = null,
    val networkError: String? = null
) {
    val messages: List<String> get() = listOfNotNull(titleError, numberError, expiryError, networkError)
    val isValid: Boolean get() = messages.isEmpty()
}

fun validateCardEntry(
    title: String,
    cardType: String,
    number: String,
    expiry: String,
    network: String,
    currentMonth: YearMonth = YearMonth.now()
): CardEntryValidation {
    val payment = isPaymentCardType(cardType)
    val digits = number.filter(Char::isDigit)
    val expectedLength = paymentCardDigitLimit(network)
    val numberError = when {
        payment && digits.length != expectedLength -> "Enter a $expectedLength-digit card number"
        payment && !isPlausibleCardNumber(digits) -> "Check the card number"
        else -> null
    }
    val expiryError = when {
        !payment && expiry.isBlank() -> null
        expiry.isBlank() -> "Enter an expiry date (MM/YY)"
        !Regex("^(0[1-9]|1[0-2])/(\\d{2})$").matches(expiry) -> "Enter a valid expiry (MM/YY)"
        else -> {
            val month = expiry.take(2).toInt()
            val year = 2000 + expiry.takeLast(2).toInt()
            if (YearMonth.of(year, month).isBefore(currentMonth)) "This card has expired" else null
        }
    }
    return CardEntryValidation(
        titleError = if (title.isBlank()) "Enter a card name" else null,
        numberError = numberError,
        expiryError = expiryError,
        networkError = if (payment && network.isBlank()) "Choose a card network" else null
    )
}

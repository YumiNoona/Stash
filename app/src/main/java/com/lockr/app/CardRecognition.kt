package com.lockr.app

data class CardDraft(
    val title: String = "",
    val cardType: String = "Other",
    val cardNumber: String = "",
    val cardholder: String = "",
    val expiry: String = "",
    val issuer: String = "",
    val network: String = "",
    val ocrText: String = ""
)

/** Suggestions only. The user reviews every field before any card data is persisted. */
fun recognizeCardFields(text: String): CardDraft {
    val number = Regex("(?<!\\d)(?:\\d[ -]?){12,19}(?!\\d)")
        .findAll(text)
        .map { it.value.filter(Char::isDigit) }
        .firstOrNull(::isPlausibleCardNumber).orEmpty()
    val expiryMatch = Regex("(?<!\\d)(0[1-9]|1[0-2])[/\\- ](\\d{2}|20\\d{2})(?!\\d)").find(text)
    val expiry = expiryMatch?.let { "${it.groupValues[1]}/${it.groupValues[2].takeLast(2)}" }.orEmpty()
    val network = if (text.contains("rupay", true)) "RuPay" else cardNetwork(number).ifBlank {
        when {
            text.contains("mastercard", true) -> "Mastercard"
            text.contains("visa", true) -> "Visa"
            text.contains("american express", true) || text.contains("amex", true) -> "Amex"
            text.contains("discover", true) -> "Discover"
            text.contains("unionpay", true) -> "UnionPay"
            text.contains("jcb", true) -> "JCB"
            else -> ""
        }
    }
    return CardDraft(
        cardType = suggestedCardType(text), cardNumber = number,
        expiry = expiry, network = network, ocrText = redactPaymentSecrets(text)
    )
}

fun isPlausibleCardNumber(value: String): Boolean {
    if (value.length !in 12..19 || !value.all(Char::isDigit)) return false
    var sum = 0
    var doubleDigit = false
    for (index in value.indices.reversed()) {
        var digit = value[index] - '0'
        if (doubleDigit) {
            digit *= 2
            if (digit > 9) digit -= 9
        }
        sum += digit
        doubleDigit = !doubleDigit
    }
    return sum % 10 == 0
}

fun cardNetwork(number: String): String = when {
    number.startsWith("4") -> "Visa"
    number.length >= 2 && number.take(2).toIntOrNull() in 51..55 -> "Mastercard"
    number.length >= 4 && number.take(4).toIntOrNull() in 2221..2720 -> "Mastercard"
    number.startsWith("34") || number.startsWith("37") -> "Amex"
    number.startsWith("35") -> "JCB"
    number.startsWith("6011") || number.startsWith("65") -> "Discover"
    else -> ""
}

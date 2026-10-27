package com.lockr.app

/** OCR is only a suggestion. Users review and edit a field before copying or saving it. */
data class SuggestedDocumentField(val key: String, val label: String, val value: String)

private val aadhaarContext = Regex("(?i)\\b(?:aadhaar|aadhar|uidai|unique identification|government of india)\\b|आधार|भारत सरकार")
private val aadhaarCandidate = Regex("(?<!\\d)(?:\\d{4}[\\s-]?){2}\\d{4}(?!\\d)")
private val addressStart = Regex("(?i)^\\s*(?:address|addr\\.?|पता)\\s*[:：-]?\\s*(.*)$")
private val addressEnd = Regex("(?i)^(?:dob|date of birth|yob|gender|male|female|vid|aadhaar|aadhar|uidai|phone|mobile|www\\.|http|download date)\\b")
private val nameLine = Regex("(?i)^\\s*(?:name|full name)\\s*[:：-]\\s*(.+)$")
private val birthLine = Regex("(?i)^\\s*(?:dob|date of birth|yob)\\s*[:：-]?\\s*(.+)$")

fun suggestDocumentFields(recognizedText: String): List<SuggestedDocumentField> {
    val lines = recognizedText.replace('\u00a0', ' ').lineSequence()
        .map { it.trim().replace(Regex("[ \\t]+"), " ") }
        .filter { it.isNotBlank() }.take(150).toList()
    val fields = mutableListOf<SuggestedDocumentField>()
    if (aadhaarContext.containsMatchIn(recognizedText)) {
        val match = lines.asSequence().filterNot { it.startsWith("VID", ignoreCase = true) }
            .mapNotNull { aadhaarCandidate.find(it)?.value?.filter(Char::isDigit) }
            .firstOrNull { it.length == 12 && it.toSet().size > 2 }
        if (match != null) fields += SuggestedDocumentField("idNumber", "Aadhaar number", match.chunked(4).joinToString(" "))
    }
    val addressIndex = lines.indexOfFirst { addressStart.matches(it) }
    if (addressIndex >= 0) {
        val parts = mutableListOf<String>()
        addressStart.matchEntire(lines[addressIndex])?.groupValues?.getOrNull(1)?.trim()?.takeIf(String::isNotBlank)?.let(parts::add)
        for (line in lines.drop(addressIndex + 1).take(6)) {
            if (addressEnd.containsMatchIn(line) || aadhaarCandidate.containsMatchIn(line)) break
            parts += line
            if (Regex("(?<!\\d)\\d{6}(?!\\d)").containsMatchIn(line)) break
        }
        val value = parts.joinToString("\n").trim().take(500)
        if (value.isNotBlank()) fields += SuggestedDocumentField("address", "Address", value)
    }
    lines.firstNotNullOfOrNull { nameLine.matchEntire(it)?.groupValues?.getOrNull(1)?.trim() }
        ?.takeIf { it.length in 2..80 }
        ?.let { fields += SuggestedDocumentField("holderName", "Name", it) }
    lines.firstNotNullOfOrNull { birthLine.matchEntire(it)?.groupValues?.getOrNull(1)?.trim() }
        ?.takeIf { it.length in 4..40 }
        ?.let { fields += SuggestedDocumentField("dateOfBirth", "Date of birth", it) }
    return fields
}

/** Keep identity numbers out of the searchable OCR index; selected fields remain encrypted separately. */
fun redactAadhaarNumbers(recognizedText: String): String =
    if (aadhaarContext.containsMatchIn(recognizedText))
        aadhaarCandidate.replace(recognizedText, "[Aadhaar number hidden]")
    else recognizedText

/** Individual OCR lines allow a precise extraction when no known document field was detected. */
fun selectableOcrLines(recognizedText: String): List<String> = recognizedText.lineSequence()
    .map { it.trim().replace(Regex("[ \\t]+"), " ") }
    .filter { it.length in 3..160 && !it.startsWith("Page ", ignoreCase = true) }
    .distinct().take(40).toList()

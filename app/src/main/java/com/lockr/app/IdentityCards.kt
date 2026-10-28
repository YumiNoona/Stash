package com.lockr.app

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.util.Locale

enum class IdentityType(val label: String, val issuer: String, val numberLabel: String) {
    AADHAAR("Aadhaar", "Unique Identification Authority of India", "Aadhaar number"),
    PAN("PAN", "Income Tax Department", "PAN number"),
    VOTER("Voter ID", "Election Commission of India", "EPIC number"),
    LICENCE("Driving licence", "Transport Department", "Licence number"),
    OTHER("Other ID", "", "ID number")
}

enum class IdentityAsset(val key: String, val label: String) {
    PHOTO("identityPhoto", "Portrait"), QR("identityQr", "Original QR"),
    SIGNATURE("identitySignature", "Signature"), LOGO("identityLogo", "Issuer logo")
}

data class IdentityField(val key: String, val label: String, val multiline: Boolean = false)

fun identityType(item: VaultItem): IdentityType? = IdentityType.entries.firstOrNull { it.name == item.metadata["identityType"] }
internal fun newIdentityTypeFields(type: IdentityType, previous: Map<String,String>): Map<String,String> =
    previous.filterKeys { it in setOf("cardholder","localName","dateOfBirth","gender","relativeName","address") } + ("issuer" to type.issuer)

fun identityFields(type: IdentityType): List<IdentityField> = buildList {
    add(IdentityField("cardholder", "Full name *"))
    add(IdentityField("idNumber", type.numberLabel + " *"))
    add(IdentityField("localName", "Name in local language"))
    add(IdentityField("dateOfBirth", "Date / year of birth"))
    if (type != IdentityType.PAN) add(IdentityField("gender", "Gender"))
    if (type != IdentityType.AADHAAR) add(IdentityField("relativeName", if (type == IdentityType.PAN) "Parent's name" else "Relative's name"))
    add(IdentityField("address", "Address", true))
    if (type == IdentityType.LICENCE) {
        add(IdentityField("issuer", "Issuing state / authority"))
        add(IdentityField("issueDate", "Date of issue"))
        add(IdentityField("validTill", "Valid until (non-transport)"))
        add(IdentityField("transportValidTill", "Valid until (transport)"))
        add(IdentityField("vehicleClasses", "Vehicle classes · LMV, MCWG…"))
        add(IdentityField("bloodGroup", "Blood group"))
    }
    if (type == IdentityType.VOTER) {
        add(IdentityField("age", "Age (if date of birth is unavailable)"))
        add(IdentityField("constituency", "Assembly constituency"))
        add(IdentityField("partNumber", "Part number"))
        add(IdentityField("serialNumber", "Serial number"))
        add(IdentityField("issueDate", "Date of issue"))
    }
    if (type == IdentityType.OTHER) {
        add(IdentityField("issuer", "Issuer / organisation"))
        add(IdentityField("validTill", "Valid until"))
    }
}

internal fun normalizeIdentityNumber(type: IdentityType, value: String): String =
    if (type == IdentityType.AADHAAR) value.filter(Char::isDigit).take(12)
    else value.uppercase(Locale.ROOT).filter { it.isLetterOrDigit() || (type == IdentityType.LICENCE && it == '-') }.take(30)

internal fun identityNumberError(type: IdentityType, value: String): String? {
    val number = normalizeIdentityNumber(type, value)
    return when {
        number.isBlank() -> "Enter the ${type.numberLabel.lowercase()}."
        type == IdentityType.AADHAAR && !Regex("[2-9][0-9]{11}").matches(number) -> "Use the 12-digit Aadhaar number from your original."
        type == IdentityType.PAN && !Regex("[A-Z]{5}[0-9]{4}[A-Z]").matches(number) -> "PAN uses 5 letters, 4 digits and 1 letter."
        type == IdentityType.VOTER && number.length !in 8..20 -> "Check the EPIC number on your original card."
        type == IdentityType.LICENCE && number.length !in 8..25 -> "Check the licence number, including its state code."
        else -> null
    }
}

internal fun identityDateError(value: String): String? {
    if (value.isBlank()) return null
    if (Regex("(?:19|20)[0-9]{2}").matches(value.trim())) return null
    return try {
        LocalDate.parse(value.trim().replace('-', '/').replace('.', '/'), DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT))
        null
    } catch (_: Exception) { "Use DD/MM/YYYY or a four-digit year." }
}

internal fun identityNumberText(type: IdentityType, raw: String, visible: Boolean): String {
    val number = normalizeIdentityNumber(type, raw)
    if (!visible) return if (number.isBlank()) "Number not added" else "••••  ${number.takeLast(4)}"
    return if (type == IdentityType.AADHAAR) number.chunked(4).joinToString(" ") else number
}

internal fun identityMetadata(type: IdentityType, fields: Map<String, String>, assets: Map<IdentityAsset, VaultAttachment>, tags: Set<String>): Map<String, String> = buildMap {
    put("identityType", type.name); put("cardType", "ID Card"); put("issuer", type.issuer)
    identityFields(type).forEach { field -> fields[field.key]?.trim()?.takeIf { it.isNotBlank() }?.let { put(field.key, it) } }
    val number = normalizeIdentityNumber(type, fields["idNumber"].orEmpty())
    put("idNumber", number); put("cardNumber", number); put("lastFour", number.takeLast(4))
    put("tags", encodeTags(tags))
    assets.forEach { (role, file) -> put(role.key, file.name) }
}

internal fun identityTitle(type: IdentityType, fields: Map<String, String>): String =
    "${type.label} · ${fields["cardholder"].orEmpty().trim()}"

internal fun identityAsset(item: VaultItem, role: IdentityAsset): VaultAttachment? =
    item.attachments.firstOrNull { it.name == item.metadata[role.key] }

internal fun identityOriginals(item: VaultItem): List<VaultAttachment> {
    val assets = IdentityAsset.entries.mapNotNull { item.metadata[it.key] }.toSet()
    return item.attachments.filterNot { it.name in assets || it.name == item.metadata["cardArtAttachment"] }
}

data class IdentitySuggestion(val type: IdentityType, val fields: Map<String, String>)

/** Deliberately conservative: unlabelled names and dates are left for the user to review. */
internal fun recognizeIdentity(text: String): IdentitySuggestion? {
    val upper = text.uppercase(Locale.ROOT)
    val pan = Regex("\\b[A-Z]{5}[0-9]{4}[A-Z]\\b").find(upper)?.value
    val aadhaar = Regex("(?<![0-9])[2-9][0-9]{3}[ -]?[0-9]{4}[ -]?[0-9]{4}(?![0-9])").find(upper)?.value
    val type = when {
        upper.contains("INCOME TAX") || upper.contains("PERMANENT ACCOUNT") || pan != null -> IdentityType.PAN
        upper.contains("DRIVING LICEN") || upper.contains("DRIVER LICEN") || upper.contains("MCWG") -> IdentityType.LICENCE
        upper.contains("ELECTION COMMISSION") || upper.contains("ELECTOR") || upper.contains("EPIC") -> IdentityType.VOTER
        upper.contains("AADHAAR") || upper.contains("UIDAI") || text.contains("आधार") -> IdentityType.AADHAAR
        else -> return null
    }
    val lines = text.lines().map(String::trim).filter(String::isNotBlank)
    fun labelled(pattern: String): String? {
        val regex = Regex("^(?:$pattern)\\s*[:：-]\\s*(.+)$", RegexOption.IGNORE_CASE)
        return lines.firstNotNullOfOrNull { regex.find(it)?.groupValues?.get(1) }
    }
    return IdentitySuggestion(type, buildMap {
        val number = when (type) {
            IdentityType.PAN -> pan
            IdentityType.AADHAAR -> aadhaar
            IdentityType.VOTER -> Regex("\\b[A-Z]{3}[0-9]{7}\\b").find(upper)?.value
            IdentityType.LICENCE -> labelled("DL No\\.?|DL Number|Licence No\\.?|License No\\.?")
                ?: Regex("\\b[A-Z]{2}[ -]?[0-9]{2}[ -]?[0-9]{4}[ -]?[0-9]{7}\\b").find(upper)?.value
            else -> null
        }
        number?.let { put("idNumber", normalizeIdentityNumber(type, it)) }
        labelled("Name|Full Name|Elector.s Name")?.let { put("cardholder", it.take(80)) }
        labelled("DOB|Date of Birth|Birth|Year of Birth")?.let { value ->
            Regex("[0-9]{2}[/.-][0-9]{2}[/.-][0-9]{4}|(?:19|20)[0-9]{2}").find(value)?.value?.let { put("dateOfBirth", it.replace('-', '/').replace('.', '/')) }
        }
        labelled("Father.s Name|Mother.s Name|Relative.s Name|S/D/W of")?.let { put("relativeName", it.take(100)) }
        Regex("\\b(FEMALE|MALE|TRANSGENDER)\\b").find(upper)?.value?.let { put("gender", it.lowercase().replaceFirstChar(Char::uppercase)) }
        labelled("Blood Group")?.let { put("bloodGroup", it.take(5)) }
        labelled("Address")?.let { put("address", it.take(500)) }
        labelled("DOI|Date of Issue")?.let { if (identityDateError(it) == null) put("issueDate", it) }
        labelled("Valid Till|Validity\\s*\\(NT\\)|Valid Until")?.let { if (identityDateError(it) == null) put("validTill", it) }
    })
}

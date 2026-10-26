package com.lockr.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CardReviewScreen(
    attachments: List<VaultAttachment>,
    initial: CardDraft,
    onBack: () -> Unit,
    onSave: (String, Map<String, String>, List<VaultAttachment>) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var title by remember(initial, attachments) { mutableStateOf(initial.title.ifBlank {
        attachments.firstOrNull()?.name?.let { name ->
            if (name.startsWith("stash-scan-")) "New card" else suggestedItemTitle(name, "")
        }.orEmpty()
    }) }
    var titleTouched by remember(initial, attachments) { mutableStateOf(false) }
    var cardType by remember(initial) { mutableStateOf(initial.cardType) }
    var number by remember(initial) { mutableStateOf(if (isPaymentCardType(initial.cardType))
        formatPaymentCardNumber(initial.cardNumber, initial.network) else initial.cardNumber) }
    var holder by remember(initial) { mutableStateOf(initial.cardholder) }
    var expiryDigits by remember(initial) { mutableStateOf(cardExpiryDigits(initial.expiry)) }
    var issuer by remember(initial) { mutableStateOf(initial.issuer) }
    var network by remember(initial) { mutableStateOf(initial.network) }
    var recognizedText by remember(initial) { mutableStateOf(initial.ocrText) }
    var recognizing by remember(attachments) { mutableStateOf(attachments.isNotEmpty()) }
    var showNumber by remember { mutableStateOf(true) }
    var networkSelectedByUser by remember(initial) { mutableStateOf(false) }
    var saveAttempted by remember { mutableStateOf(false) }
    var saveNotice by remember { mutableStateOf("") }

    LaunchedEffect(attachments) {
        if (attachments.isEmpty()) return@LaunchedEffect
        recognizing = true
        val raw = buildString {
            attachments.forEach { attachment ->
                try {
                    val text = recognizeAttachmentText(context, attachment)
                    if (text.isNotBlank()) {
                        if (isNotEmpty()) append("\n\n")
                        append(text)
                    }
                } catch (_: Exception) {
                    // The original image or PDF can still be saved without OCR.
                }
            }
        }
        if (raw.isNotBlank()) {
            val suggestion = recognizeCardFields(raw)
            if (number.isBlank()) number = formatPaymentCardNumber(suggestion.cardNumber, suggestion.network)
            if (expiryDigits.isBlank()) expiryDigits = cardExpiryDigits(suggestion.expiry)
            if (network.isBlank()) network = suggestion.network
            if (cardType == "Other") cardType = suggestion.cardType
            if (!titleTouched && (title.isBlank() || title == "New card") && suggestion.network.isNotBlank()) {
                title = "${suggestion.network} card"
            }
            recognizedText = suggestion.ocrText.take(120_000)
        }
        recognizing = false
    }

    LaunchedEffect(recognizing) {
        if (!recognizing) saveNotice = ""
    }

    val payment = isPaymentCardType(cardType)
    val expiry = formatCardExpiry(expiryDigits)
    val validation = validateCardEntry(title.trim(), cardType, number, expiry, network)
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = Color(0xF7FFFFFF), unfocusedContainerColor = Color(0xECFFFFFF),
        errorContainerColor = Color(0xF7FFFFFF),
        focusedTextColor = LockrColors.TextPrimary, unfocusedTextColor = LockrColors.TextPrimary,
        errorTextColor = LockrColors.TextPrimary,
        focusedLabelColor = LockrColors.Accent, unfocusedLabelColor = LockrColors.TextSecondary,
        focusedBorderColor = LockrColors.Accent, unfocusedBorderColor = Color(0xFFBACBD8),
        cursorColor = LockrColors.Accent)
    val metadata = buildMap {
        put("cardType", cardType)
        if (number.isNotBlank()) put("cardNumber", if (payment) number.filter(Char::isDigit) else number.trim())
        if (number.any(Char::isDigit)) put("lastFour", number.filter(Char::isDigit).takeLast(4))
        if (holder.isNotBlank()) put("cardholder", holder.trim())
        if (expiry.isNotBlank()) put("expiry", expiry.trim())
        if (issuer.isNotBlank()) put("issuer", issuer.trim())
        if (network.isNotBlank()) put("network", network.trim())
        if (recognizedText.isNotBlank()) put("ocrText", recognizedText)
    }
    val preview = VaultItem(title.ifBlank { "Your card" }, VaultCategory.CARD, cardType, "",
        lastFour = number.filter(Char::isDigit).takeLast(4), metadata = metadata, id = "card-draft")

    Column(modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        StashTopBar(if (attachments.isEmpty()) "Add a card" else "Review card",
            subtitle = if (attachments.isEmpty()) "Enter the details you want to keep" else "Check the scan before saving",
            onBack = onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(13.dp)) {
            CardDetailPreview(preview, height = 188.dp)
            if (attachments.isNotEmpty()) {
                Text("Original scan", style = MaterialTheme.typography.titleSmall, color = LockrColors.TextPrimary)
                VaultAttachmentPreview(attachments.first(), Modifier.fillMaxWidth().height(160.dp))
                if (attachments.size > 1) Text("${attachments.size} files attached", style = MaterialTheme.typography.bodySmall,
                    color = LockrColors.TextSecondary)
                Text(if (recognizing) "Reading the image on this device…" else if (recognizedText.isBlank())
                    "No card details detected. Fill in what you want to save." else "Detected details are suggestions. Check each field.",
                    style = MaterialTheme.typography.bodySmall, color = LockrColors.TextSecondary)
            }
            OutlinedTextField(title, { title = it.take(80); titleTouched = true; saveNotice = "" },
                label = { Text("Card name *") }, singleLine = true,
                isError = saveAttempted && validation.titleError != null,
                supportingText = validation.titleError.takeIf { saveAttempted }?.let { error -> { Text(error) } },
                colors = fieldColors, shape = RoundedCornerShape(17.dp), modifier = Modifier.fillMaxWidth())
            Column {
                Text("Card type", style = MaterialTheme.typography.titleSmall, color = LockrColors.TextPrimary)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Credit", "Debit", "ID Card", "Other").forEach { type ->
                        FilterChip(selected = cardType == type, onClick = {
                            cardType = type
                            if (isPaymentCardType(type)) number = formatPaymentCardNumber(number, network)
                            saveNotice = ""
                        },
                            label = { Text(type, maxLines = 1, softWrap = false) })
                    }
                }
            }
            OutlinedTextField(issuer, { issuer = it.take(60) }, label = { Text("Issuer or bank (optional)") },
                singleLine = true, colors = fieldColors, shape = RoundedCornerShape(17.dp), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(number, {
                if (payment) {
                    val digits = it.filter(Char::isDigit)
                    if (!networkSelectedByUser) network = cardNetwork(digits)
                    number = formatPaymentCardNumber(digits, network)
                } else number = it.take(40)
                saveNotice = ""
            },
                label = { Text(if (payment) "Card number *" else if (cardType == "ID Card") "ID number (optional)" else "Card number (optional)") },
                placeholder = if (payment) ({ Text("0000 0000 0000 0000") }) else null,
                singleLine = true, isError = saveAttempted && validation.numberError != null,
                supportingText = validation.numberError.takeIf { saveAttempted }?.let { error -> { Text(error) } },
                visualTransformation = if (showNumber) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = if (payment) KeyboardType.Number else KeyboardType.Text),
                trailingIcon = { IconButton(onClick = { showNumber = !showNumber }) {
                    Icon(if (showNumber) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (showNumber) "Hide number" else "Show number")
                } }, colors = fieldColors, shape = RoundedCornerShape(17.dp), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(holder, { holder = it.take(60) }, label = { Text("Name on card (optional)") },
                singleLine = true, colors = fieldColors, shape = RoundedCornerShape(17.dp), modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                OutlinedTextField(expiryDigits, { expiryDigits = cardExpiryDigits(it); saveNotice = "" },
                    label = { Text(if (payment) "Expiry *" else "Expiry") },
                    placeholder = { Text("MM/YY") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = ExpiryVisualTransformation,
                    isError = saveAttempted && validation.expiryError != null,
                    supportingText = validation.expiryError.takeIf { saveAttempted }?.let { error -> { Text(error) } },
                    singleLine = true, colors = fieldColors, shape = RoundedCornerShape(17.dp), modifier = Modifier.weight(1f))
                CardNetworkDropdown(network, payment, saveAttempted && validation.networkError != null,
                    fieldColors, Modifier.weight(1f)) { selected ->
                    networkSelectedByUser = true
                    network = selected
                    if (payment) number = formatPaymentCardNumber(number, network)
                    saveNotice = ""
                }
            }
            Spacer(Modifier.height(6.dp))
        }
        if ((saveAttempted && !validation.isValid) || saveNotice.isNotBlank()) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp))
                .background(Color(0xF7FFFFFF)).border(1.dp, Color(0xFFFFD5D5), RoundedCornerShape(17.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text("Before saving", style = MaterialTheme.typography.labelLarge, color = LockrColors.TextPrimary)
                if (saveNotice.isNotBlank()) Text(saveNotice, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error)
                else validation.messages.forEach { message ->
                    Text("• $message", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        }
        Button(onClick = {
            if (recognizing) saveNotice = "Wait for the card scan to finish"
            else if (!validation.isValid) saveAttempted = true
            else onSave(title.trim(), metadata, attachments)
        },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 10.dp).height(54.dp)) {
            Text("Save card to Stash")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CardNetworkDropdown(
    value: String,
    required: Boolean,
    isError: Boolean,
    fieldColors: TextFieldColors,
    modifier: Modifier = Modifier,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val options = if (required) CARD_NETWORKS else listOf("Not specified") + CARD_NETWORKS
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(value = value, onValueChange = {}, readOnly = true,
            label = { Text(if (required) "Network *" else "Network") },
            placeholder = { Text("Choose") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            isError = isError,
            supportingText = if (isError) ({ Text("Choose a network") }) else null,
            singleLine = true, colors = fieldColors, shape = RoundedCornerShape(17.dp),
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable))
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false },
            containerColor = Color(0xFFF7FBFF), shape = RoundedCornerShape(19.dp)) {
            options.forEach { option ->
                val selected = value == option || (option == "Not specified" && value.isBlank())
                val accent = when (option) {
                    "Visa" -> Color(0xFF3458A7)
                    "Mastercard" -> Color(0xFFE87555)
                    "RuPay" -> Color(0xFF428A75)
                    "Amex" -> Color(0xFF458EC0)
                    "JCB" -> Color(0xFF8B61AF)
                    else -> Color(0xFF657893)
                }
                DropdownMenuItem(
                    text = { Text(option, color = LockrColors.TextPrimary,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp)) },
                    leadingIcon = {
                        Box(Modifier.size(29.dp).clip(CircleShape).background(accent.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center) {
                            Text(option.take(1), color = accent,
                                style = MaterialTheme.typography.labelSmall)
                        }
                    },
                    trailingIcon = if (selected) ({ Icon(Icons.Outlined.Check, contentDescription = null,
                        tint = LockrColors.Accent, modifier = Modifier.size(18.dp)) }) else null,
                    onClick = {
                        onSelect(if (option == "Not specified") "" else option)
                        expanded = false
                    }
                )
            }
        }
    }
}

internal object ExpiryVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        val hasSeparator = digits.length > 2
        val visible = if (hasSeparator) "${digits.take(2)}/${digits.drop(2)}" else digits
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val safe = offset.coerceIn(0, digits.length)
                return safe + if (hasSeparator && safe > 2) 1 else 0
            }

            override fun transformedToOriginal(offset: Int): Int {
                val safe = offset.coerceIn(0, visible.length)
                return (safe - if (hasSeparator && safe > 2) 1 else 0).coerceIn(0, digits.length)
            }
        }
        return TransformedText(AnnotatedString(visible), mapping)
    }
}

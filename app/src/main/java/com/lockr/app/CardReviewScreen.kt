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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
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
    var cardType by remember(initial) { mutableStateOf(initial.cardType) }
    var numberField by remember(initial) { mutableStateOf(TextFieldValue(
        if (isPaymentCardType(initial.cardType)) initial.cardNumber.filter(Char::isDigit)
        else initial.cardNumber)) }
    var holder by remember(initial) { mutableStateOf(initial.cardholder) }
    var expiryDigits by remember(initial) { mutableStateOf(cardExpiryDigits(initial.expiry)) }
    var partner by remember(initial) { mutableStateOf("") }
    var motion by remember(initial) { mutableStateOf(CardMotion.NONE) }
    var customArt by remember(initial) { mutableStateOf<VaultAttachment?>(null) }
    var lightText by remember(initial) { mutableStateOf(true) }
    var issuer by remember(initial) { mutableStateOf(initial.issuer) }
    var network by remember(initial) { mutableStateOf(initial.network) }
    var cvc by remember(initial) { mutableStateOf("") }
    var cardArt by remember(initial) { mutableStateOf(CardArtStyle.AURORA) }
    var showCvc by remember { mutableStateOf(false) }
    var recognizedText by remember(initial) { mutableStateOf(initial.ocrText) }
    var recognizing by remember(attachments) { mutableStateOf(attachments.isNotEmpty()) }
    var showNumber by remember { mutableStateOf(true) }
    var networkSelectedByUser by remember(initial) { mutableStateOf(false) }
    var saveAttempted by remember { mutableStateOf(false) }
    var saveNotice by remember { mutableStateOf("") }
    val number = numberField.text
    val title = cardDisplayTitle(issuer, partner, network, cardType, number)
    var tags by remember { mutableStateOf<Set<String>>(emptySet()) }
    val ocrEnabled = LocalOcrOnImport.current

    LaunchedEffect(attachments) {
        if (attachments.isEmpty() || !ocrEnabled) { recognizing = false; return@LaunchedEffect }
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
            if (number.isBlank()) {
                val suggested = suggestion.cardNumber.filter(Char::isDigit)
                numberField = TextFieldValue(suggested, TextRange(suggested.length))
            }
            if (expiryDigits.isBlank()) expiryDigits = cardExpiryDigits(suggestion.expiry)
            if (network.isBlank()) network = suggestion.network
            if (cardType == "Other") cardType = suggestion.cardType
            recognizedText = suggestion.ocrText.take(120_000)
        }
        recognizing = false
    }

    LaunchedEffect(recognizing) {
        if (!recognizing) saveNotice = ""
    }

    val payment = isPaymentCardType(cardType)
    val expiry = formatCardExpiry(expiryDigits)
    val validation = validateCardEntry(title.trim(), cardType, number, expiry, network, cvc = cvc)
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = LockrColors.Surface, unfocusedContainerColor = LockrColors.Surface,
        errorContainerColor = LockrColors.Surface,
        focusedTextColor = LockrColors.TextPrimary, unfocusedTextColor = LockrColors.TextPrimary,
        errorTextColor = LockrColors.TextPrimary,
        focusedLabelColor = LockrColors.Accent, unfocusedLabelColor = LockrColors.TextSecondary,
        focusedBorderColor = LockrColors.Accent, unfocusedBorderColor = Color(0xFFBACBD8),
        cursorColor = LockrColors.Accent)
    val metadata = buildMap {
        put("cardType", cardType)
        put("tags", encodeTags(tags))
        if (number.isNotBlank()) put("cardNumber", if (payment) number.filter(Char::isDigit) else number.trim())
        if (number.any(Char::isDigit)) put("lastFour", number.filter(Char::isDigit).takeLast(4))
        if (holder.isNotBlank()) put("cardholder", holder.trim())
        if (expiry.isNotBlank()) put("expiry", expiry.trim())
        if (issuer.isNotBlank()) put("issuer", issuer.trim())
        if (network.isNotBlank()) put("network", network.trim())
        if (payment && cvc.isNotBlank()) put("cardCvc", cvc)
        put("cardArt", cardArt.name)
        if (partner.isNotBlank()) put("partner", partner.trim())
        put("cardMotion", motion.name)
        put("cardArtLightText", lightText.toString())
        customArt?.let { put("cardArtAttachment", it.name) }
    }
    val preview = VaultItem(title.ifBlank { "Your card" }, VaultCategory.CARD, cardType, "",
        lastFour = number.filter(Char::isDigit).takeLast(4), metadata = metadata, attachments = listOfNotNull(customArt), id = "card-draft")

    Column(modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        StashTopBar(if (attachments.isEmpty()) "Add a card" else "Review card",
            subtitle = if (attachments.isEmpty()) "Enter the details you want to keep" else "Check the scan before saving",
            onBack = onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(13.dp)) {
            CardDetailPreview(preview, showFullNumber = showNumber)
            CardCustomization(cardArt, { cardArt = it }, motion, { motion = it },
                customArt, { customArt = it }, lightText, { lightText = it })
            if (attachments.isNotEmpty()) {
                Text("Original scan", style = MaterialTheme.typography.titleSmall, color = LockrColors.TextPrimary)
                VaultAttachmentPreview(attachments.first(), Modifier.fillMaxWidth().height(160.dp))
                if (attachments.size > 1) Text("${attachments.size} files attached", style = MaterialTheme.typography.bodySmall,
                    color = LockrColors.TextSecondary)
                Text(if (recognizing) "Reading the image on this device…" else if (recognizedText.isBlank())
                    "No card details detected. Fill in what you want to save." else "Detected details are suggestions. Check each field.",
                    style = MaterialTheme.typography.bodySmall, color = LockrColors.TextSecondary)
            }
            Column {
                Text("Card type", style = MaterialTheme.typography.titleSmall, color = LockrColors.TextPrimary)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Credit", "Debit", "ID Card", "Other").forEach { type ->
                        FilterChip(selected = cardType == type, onClick = {
                            cardType = type
                            if (isPaymentCardType(type)) numberField = normalizedPaymentCardValue(numberField, network)
                            saveNotice = ""
                        },
                            label = { Text(type, maxLines = 1, softWrap = false) })
                    }
                }
            }
            OutlinedTextField(holder, { holder = it.take(60) }, label = { Text("Name on card (optional)") },
                singleLine = true, colors = fieldColors, shape = RoundedCornerShape(17.dp), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(issuer, { issuer = it.take(60) }, label = { Text("Issuer or bank (optional)") },
                singleLine = true, colors = fieldColors, shape = RoundedCornerShape(17.dp), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(partner, { partner = it.take(60) }, label = { Text("Partner brand (optional)") },
                placeholder = { Text("Amazon Pay, for example") }, singleLine = true,
                colors = fieldColors, shape = RoundedCornerShape(17.dp), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(numberField, {
                if (payment) {
                    val digits = it.text.filter(Char::isDigit)
                    if (!networkSelectedByUser) network = cardNetwork(digits)
                    numberField = normalizedPaymentCardValue(it, network)
                } else numberField = it.copy(text = it.text.take(40),
                    selection = TextRange(it.selection.start.coerceAtMost(40), it.selection.end.coerceAtMost(40)))
                saveNotice = ""
            },
                label = { Text(if (payment) "Card number *" else if (cardType == "ID Card") "ID number (optional)" else "Card number (optional)") },
                placeholder = if (payment) ({ Text("0000 0000 0000 0000") }) else null,
                singleLine = true, isError = saveAttempted && validation.numberError != null,
                supportingText = validation.numberError.takeIf { saveAttempted }?.let { error -> { Text(error) } },
                visualTransformation = if (payment) PaymentCardVisualTransformation(masked = !showNumber)
                    else if (showNumber) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = if (payment) KeyboardType.Number else KeyboardType.Text),
                trailingIcon = { IconButton(onClick = { showNumber = !showNumber }) {
                    Icon(if (showNumber) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (showNumber) "Hide number" else "Show number")
                } }, colors = fieldColors, shape = RoundedCornerShape(17.dp), modifier = Modifier.fillMaxWidth())

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
                    if (payment) numberField = normalizedPaymentCardValue(numberField, network)
                    saveNotice = ""
                }
            }
            if (payment) {
                OutlinedTextField(cvc, { cvc = it.filter(Char::isDigit).take(4); saveNotice = "" },
                    label = { Text("CVC (optional)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = if (showCvc) VisualTransformation.None else PasswordVisualTransformation(),
                    isError = saveAttempted && validation.cvcError != null,
                    supportingText = validation.cvcError.takeIf { saveAttempted }?.let { error -> { Text(error) } },
                    trailingIcon = { IconButton(onClick = { showCvc = !showCvc }) {
                        Icon(if (showCvc) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (showCvc) "Hide CVC" else "Show CVC")
                    } }, colors = fieldColors, shape = RoundedCornerShape(17.dp), modifier = Modifier.fillMaxWidth())
            }
            TagEditor(VaultCategory.CARD, tags, { tags = it })
            validation.warnings.forEach { warning ->
                Text(warning, style = MaterialTheme.typography.bodySmall, color = Color(0xFF436A8A))
            }
            Spacer(Modifier.height(28.dp))
        }
        if ((saveAttempted && !validation.isValid) || saveNotice.isNotBlank()) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp))
                .background(LockrColors.Surface).border(1.dp, Color(0xFFFFD5D5), RoundedCornerShape(17.dp))
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
            else {
                val allAttachments = withCardArt(attachments, null, customArt)
                if (allAttachments.size > MAX_VAULT_ATTACHMENT_COUNT || allAttachments.sumOf { it.byteCount } > MAX_VAULT_TOTAL_ATTACHMENT_BYTES)
                    saveNotice = "Original scans and artwork together must fit within 20 files and 24 MB. Choose smaller artwork."
                else onSave(title.trim(), metadata, allAttachments)
            }
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
            containerColor = LockrColors.Surface, shape = RoundedCornerShape(19.dp)) {
            options.forEach { option ->
                val selected = value == option || (option == "Not specified" && value.isBlank())
                DropdownMenuItem(
                    text = { Text(option, color = LockrColors.TextPrimary,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp)) },
                    leadingIcon = { CardNetworkMark(option) },
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

@Composable
private fun CardNetworkMark(network: String) {
    Box(Modifier.size(width = 38.dp, height = 30.dp).clip(RoundedCornerShape(9.dp))
        .background(Color.White).border(1.dp, Color(0xFFE6ECF4), RoundedCornerShape(9.dp)),
        contentAlignment = Alignment.Center) {
        when (network) {
            "Mastercard" -> {
                Box(Modifier.size(18.dp).align(Alignment.CenterStart).padding(start = 2.dp)
                    .background(Color(0xFFEB4F42), CircleShape))
                Box(Modifier.size(18.dp).align(Alignment.CenterEnd).padding(end = 2.dp)
                    .background(Color(0xFFF9B33D).copy(alpha = 0.88f), CircleShape))
            }
            "Visa" -> Text("VISA", color = Color(0xFF17479E), fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic, fontSize = 9.sp)
            "RuPay" -> Text("RuPay", color = Color(0xFF2352A2), fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic, fontSize = 7.sp)
            "Amex" -> Box(Modifier.size(width = 30.dp, height = 23.dp).background(Color(0xFF2887C7), RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center) {
                Text("AMEX", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 7.sp)
            }
            "JCB" -> Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                listOf(Color(0xFF2A70B6), Color(0xFFD64F66), Color(0xFF53A96A)).forEachIndexed { index, color ->
                    Box(Modifier.size(width = 9.dp, height = 21.dp).background(color, RoundedCornerShape(3.dp)),
                        contentAlignment = Alignment.Center) {
                        Text("JCB"[index].toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 6.sp)
                    }
                }
            }
            "UnionPay" -> Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                listOf(Color(0xFFD44D53), Color(0xFF277DB9), Color(0xFF4BAF81)).forEach { color ->
                    Box(Modifier.size(width = 8.dp, height = 21.dp).background(color, RoundedCornerShape(3.dp)))
                }
            }
            "Discover" -> Text("DISC", color = Color(0xFFE5753E), fontWeight = FontWeight.Black, fontSize = 8.sp)
            else -> Text(if (network == "Not specified") "—" else "•••", color = Color(0xFF64748B), fontSize = 10.sp)
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

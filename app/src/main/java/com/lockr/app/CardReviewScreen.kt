package com.lockr.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

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
    var number by remember(initial) { mutableStateOf(initial.cardNumber) }
    var holder by remember(initial) { mutableStateOf(initial.cardholder) }
    var expiry by remember(initial) { mutableStateOf(initial.expiry) }
    var issuer by remember(initial) { mutableStateOf(initial.issuer) }
    var network by remember(initial) { mutableStateOf(initial.network) }
    var recognizedText by remember(initial) { mutableStateOf(initial.ocrText) }
    var recognizing by remember(attachments) { mutableStateOf(attachments.isNotEmpty()) }
    var showNumber by remember { mutableStateOf(false) }

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
            if (number.isBlank()) number = suggestion.cardNumber
            if (expiry.isBlank()) expiry = suggestion.expiry
            if (network.isBlank()) network = suggestion.network
            if (cardType == "Other") cardType = suggestion.cardType
            if (!titleTouched && (title.isBlank() || title == "New card") && suggestion.network.isNotBlank()) {
                title = "${suggestion.network} card"
            }
            recognizedText = suggestion.ocrText.take(120_000)
        }
        recognizing = false
    }

    val metadata = buildMap {
        put("cardType", cardType)
        if (number.isNotBlank()) put("cardNumber", number.trim())
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
            OutlinedTextField(title, { title = it.take(80); titleTouched = true }, label = { Text("Card name *") },
                singleLine = true, shape = RoundedCornerShape(17.dp), modifier = Modifier.fillMaxWidth())
            Column {
                Text("Card type", style = MaterialTheme.typography.titleSmall, color = LockrColors.TextPrimary)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Credit", "Debit", "ID Card", "Other").forEach { type ->
                        FilterChip(selected = cardType == type, onClick = { cardType = type },
                            label = { Text(type, maxLines = 1, softWrap = false) })
                    }
                }
            }
            OutlinedTextField(issuer, { issuer = it.take(60) }, label = { Text("Issuer or bank (optional)") },
                singleLine = true, shape = RoundedCornerShape(17.dp), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(number, {
                number = it.filterNot(Char::isWhitespace).take(40)
                if (network.isBlank()) network = cardNetwork(number.filter(Char::isDigit))
            },
                label = { Text("Card or ID number (optional)") }, singleLine = true,
                visualTransformation = if (showNumber) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = if (cardType == "ID Card") KeyboardType.Text else KeyboardType.Number),
                trailingIcon = { IconButton(onClick = { showNumber = !showNumber }) {
                    Icon(if (showNumber) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (showNumber) "Hide number" else "Show number")
                } }, shape = RoundedCornerShape(17.dp), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(holder, { holder = it.take(60) }, label = { Text("Name on card (optional)") },
                singleLine = true, shape = RoundedCornerShape(17.dp), modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                OutlinedTextField(expiry, { expiry = it.take(10) }, label = { Text("Expiry") },
                    singleLine = true, shape = RoundedCornerShape(17.dp), modifier = Modifier.weight(1f))
                OutlinedTextField(network, { network = it.take(20) }, label = { Text("Network") },
                    singleLine = true, shape = RoundedCornerShape(17.dp), modifier = Modifier.weight(1f))
            }
            Text("Security codes are never saved. Card details and original files stay encrypted on this device.",
                style = MaterialTheme.typography.bodySmall, color = LockrColors.TextSecondary)
            Spacer(Modifier.height(6.dp))
        }
        Button(onClick = { onSave(title.trim(), metadata, attachments) },
            enabled = title.isNotBlank() && !recognizing,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 10.dp).height(54.dp)) {
            Text("Save card to Stash")
        }
    }
}

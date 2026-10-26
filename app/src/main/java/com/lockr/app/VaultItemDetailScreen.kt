package com.lockr.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun VaultItemDetailScreen(
    item: VaultItem,
    autoClearClipboard: Boolean,
    revealTimeout: String,
    authorized: Boolean,
    onAuthenticate: () -> Unit,
    onAuthorizationExpired: () -> Unit,
    onBack: () -> Unit,
    onEdit: (VaultItem) -> Unit,
    onDelete: (VaultItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var revealed by rememberSaveable(item.id) { mutableStateOf(false) }
    var confirmDelete by rememberSaveable(item.id) { mutableStateOf(false) }
    var editing by rememberSaveable(item.id) { mutableStateOf(false) }
    var editValidationAttempted by rememberSaveable(item.id) { mutableStateOf(false) }
    val editCardType = item.metadata["cardType"].orEmpty().ifBlank { item.detail }
    val paymentCard = isPaymentCardType(editCardType)
    var editTitle by rememberSaveable(item.id) { mutableStateOf(item.title) }
    var editCategory by remember(item.id) { mutableStateOf(item.metadata["documentCategory"].orEmpty()) }
    var editCardholder by rememberSaveable(item.id) { mutableStateOf(item.metadata["cardholder"].orEmpty()) }
    var editCardNumber by rememberSaveable(item.id) { mutableStateOf(if (paymentCard)
        formatPaymentCardNumber(item.metadata["cardNumber"].orEmpty(), item.metadata["network"].orEmpty())
        else item.metadata["cardNumber"].orEmpty()) }
    var editExpiryDigits by rememberSaveable(item.id) { mutableStateOf(cardExpiryDigits(item.metadata["expiry"].orEmpty())) }
    var editIssuer by rememberSaveable(item.id) { mutableStateOf(item.metadata["issuer"].orEmpty()) }
    var editNetwork by rememberSaveable(item.id) { mutableStateOf(item.metadata["network"].orEmpty()) }
    var editNetworkSelectedByUser by rememberSaveable(item.id) { mutableStateOf(false) }
    val editExpiry = formatCardExpiry(editExpiryDigits)
    val editValidation = validateCardEntry(editTitle.trim(), editCardType, editCardNumber, editExpiry, editNetwork)
    var showMenu by rememberSaveable(item.id) { mutableStateOf(false) }
    var showOriginal by rememberSaveable(item.id) { mutableStateOf(false) }
    var feedback by rememberSaveable(item.id) { mutableStateOf("") }
    BackHandler(onBack = onBack)

    LaunchedEffect(authorized, revealTimeout) {
        if (authorized) {
            val timeout = when {
                revealTimeout.startsWith("10") -> 10_000L
                revealTimeout.startsWith("1 minute") -> 60_000L
                revealTimeout.startsWith("5 minutes") -> 300_000L
                else -> 30_000L
            }
            delay(timeout)
            revealed = false
            onAuthorizationExpired()
        }
    }

    Column(modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        if (item.category == VaultCategory.CARD) {
            Row(Modifier.fillMaxWidth().height(69.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(45.dp).shadow(6.dp, CircleShape).clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.82f))
                    .border(1.dp, Color.White.copy(alpha = 0.96f), CircleShape)
                    .clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.ChevronLeft, contentDescription = "Back", modifier = Modifier.size(27.dp))
                }
                Spacer(Modifier.weight(1f))
                Box {
                    Box(Modifier.size(45.dp).shadow(6.dp, CircleShape).clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.82f))
                        .border(1.dp, Color.White.copy(alpha = 0.96f), CircleShape)
                        .clickable { showMenu = true }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "Card options")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        if (item.attachments.isNotEmpty()) DropdownMenuItem(text = { Text("View original file") },
                            onClick = { showMenu = false; showOriginal = true; if (!authorized) onAuthenticate() })
                        DropdownMenuItem(text = { Text("Edit card") }, leadingIcon = { Icon(Icons.Outlined.Edit, null) },
                            onClick = {
                                showMenu = false
                                if (item.metadata["cardNumber"].isNullOrBlank() || authorized) {
                                    editValidationAttempted = false
                                    editing = true
                                } else onAuthenticate()
                            })
                        DropdownMenuItem(text = { Text("Delete card") }, leadingIcon = { Icon(Icons.Outlined.DeleteOutline, null) },
                            onClick = { showMenu = false; confirmDelete = true })
                    }
                }
            }
        } else StashTopBar(
            item.title,
            subtitle = "${item.category.label} · saved ${item.time}",
            onBack = onBack,
            trailing = {
                IconButton(onClick = { editing = true }) { Icon(Icons.Outlined.Edit, contentDescription = "Edit item", tint = LockrColors.TextPrimary) }
                IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete item", tint = LockrColors.TextPrimary) }
            }
        )
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (item.category == VaultCategory.CARD) {
                CardDetailPreview(item, Modifier.fillMaxWidth().padding(top = 15.dp, bottom = 22.dp), height = 255.dp)
            }
            if (item.category == VaultCategory.CARD) {
                GlassPanel(Modifier.fillMaxWidth(), radius = 26.dp) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 19.dp, vertical = 6.dp)) {
                        val number = item.metadata["cardNumber"].orEmpty()
                        val lastFour = item.lastFour.ifBlank { number.takeLast(4).ifBlank { item.metadata["lastFour"].orEmpty() } }
                        DetailField("Card Number", "••••  ••••  ${lastFour.ifBlank { "••••" }}",
                            onCopy = {
                                if (number.isNotBlank()) {
                                    if (authorized) { copyPrivateText(context, number, autoClearClipboard); feedback = "Card number copied" }
                                    else onAuthenticate()
                                } else feedback = "Full card number was not saved"
                            }, onReveal = null, revealed = false)
                        CardDetailDivider()
                        DetailField("Card Holder Name", item.metadata["cardholder"].orEmpty().ifBlank { "Not added" },
                            onCopy = { item.metadata["cardholder"]?.takeIf { it.isNotBlank() }?.let { copyPrivateText(context, it, autoClearClipboard); feedback = "Name copied" } },
                            onReveal = null, revealed = false)
                        CardDetailDivider()
                        DetailField("Expiry Date", item.metadata["expiry"].orEmpty().ifBlank { "Not added" },
                            onCopy = { item.metadata["expiry"]?.takeIf { it.isNotBlank() }?.let { copyPrivateText(context, it, autoClearClipboard); feedback = "Expiry copied" } },
                            onReveal = null, revealed = false)
                        CardDetailDivider()
                        DetailField("Security Code", "Not stored", onCopy = { feedback = "Security code was not saved" },
                            onReveal = null, revealed = false)
                    }
                }
            } else if (item.metadata.isNotEmpty()) {
                GlassPanel(Modifier.fillMaxWidth(), radius = 22.dp) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 15.dp)) {
                        item.metadata.entries
                            .filter { it.key !in setOf("ocrText", "password", "securityCode", "cvv", "cvc", "content", "lastFour") && it.value.isNotBlank() }
                            .forEach { (key, value) ->
                                val sensitive = key in setOf("cardNumber", "idNumber", "documentNumber")
                                DetailField(
                                    key.toFieldLabel(),
                                    if (sensitive && !revealed) "••••  ••••  ••••  ${value.takeLast(4)}" else value,
                                    onCopy = {
                                        if (sensitive && !authorized) onAuthenticate()
                                        else {
                                            copyPrivateText(context, value, autoClearClipboard)
                                            feedback = if (autoClearClipboard) "Copied · clipboard clears in 30 seconds" else "Copied"
                                        }
                                    },
                                    onReveal = if (sensitive) {
                                        {
                                            if (authorized) revealed = !revealed else onAuthenticate()
                                        }
                                    } else null,
                                    revealed = revealed
                                )
                            }
                        val password = item.metadata["password"].orEmpty()
                        if (password.isNotBlank()) {
                            DetailField(
                                "Password",
                                if (revealed && authorized) password else "••••••••••••",
                                onCopy = {
                                    if (authorized) {
                                        copyPrivateText(context, password, autoClearClipboard)
                                        feedback = if (autoClearClipboard) "Copied · clipboard clears in 30 seconds" else "Copied"
                                    } else onAuthenticate()
                                },
                                onReveal = { if (authorized) revealed = !revealed else onAuthenticate() },
                                revealed = revealed && authorized
                            )
                        }
                        val note = item.metadata["content"] ?: item.metadata["notes"]
                        note?.takeIf { it.isNotBlank() }?.let { text ->
                            Text("Note", style = MaterialTheme.typography.labelLarge, color = LockrColors.TextSecondary, modifier = Modifier.padding(top = 13.dp))
                            Text(text, style = MaterialTheme.typography.bodyLarge, color = LockrColors.TextPrimary, modifier = Modifier.padding(top = 3.dp, bottom = 13.dp))
                        }
                    }
                }
            }
            item.metadata["ocrText"]?.takeIf { it.isNotBlank() }?.let { text ->
                OcrTextCard(text, onCopy = {
                    copyPrivateText(context, text, autoClearClipboard)
                    feedback = if (autoClearClipboard) "Extracted text copied · clears in 30 seconds" else "Extracted text copied"
                })
            }
            if (item.attachments.isNotEmpty() && (item.category !in setOf(VaultCategory.CARD, VaultCategory.PASSWORD) || (authorized && (item.category != VaultCategory.CARD || showOriginal)))) {
                Column {
                    Text("Original files", style = MaterialTheme.typography.titleMedium, color = LockrColors.TextPrimary, modifier = Modifier.padding(bottom = 7.dp))
                    VaultAttachmentsSection(item.attachments, Modifier.fillMaxWidth())
                }
            } else if (item.attachments.isNotEmpty() && item.category != VaultCategory.CARD) {
                Button(onClick = onAuthenticate, modifier = Modifier.fillMaxWidth()) { Text("Unlock attached file") }
            }
            if (feedback.isNotBlank()) Text(feedback, style = MaterialTheme.typography.bodySmall, color = LockrColors.Accent)
            Spacer(Modifier.height(20.dp))
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = Color(0xF8FFFFFF),
            title = { Text("Delete ${item.title}?") },
            text = { Text("This removes the item and its attached files from this device.", color = LockrColors.TextSecondary) },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete(item) }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            }
        )
    }
    if (editing) {
        val editFieldColors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White, unfocusedContainerColor = Color.White,
            errorContainerColor = Color.White,
            focusedTextColor = LockrColors.TextPrimary, unfocusedTextColor = LockrColors.TextPrimary,
            errorTextColor = LockrColors.TextPrimary,
            focusedBorderColor = LockrColors.Accent, unfocusedBorderColor = Color(0xFFBACBD8))
        AlertDialog(
            onDismissRequest = { editing = false },
            containerColor = Color(0xF8FFFFFF),
            title = {
                Column {
                    Text(if (item.category == VaultCategory.CARD) "Edit card" else "Edit item")
                    if (item.category == VaultCategory.CARD && editValidationAttempted && !editValidation.isValid) {
                        Text(editValidation.messages.joinToString(" · "),
                            color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            text = {
                Column(Modifier.heightIn(max = 470.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = editTitle, onValueChange = { editTitle = it.take(80) },
                        label = { Text(if (item.category == VaultCategory.CARD) "Card name" else "Title") }, singleLine = true,
                        isError = item.category == VaultCategory.CARD && editValidationAttempted && editValidation.titleError != null,
                        colors = editFieldColors, shape = RoundedCornerShape(15.dp), modifier = Modifier.fillMaxWidth())
                    if (item.category == VaultCategory.CARD) {
                        OutlinedTextField(value = editIssuer, onValueChange = { editIssuer = it.take(60) },
                            label = { Text("Issuer") }, singleLine = true, colors = editFieldColors,
                            shape = RoundedCornerShape(15.dp), modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = editCardholder, onValueChange = { editCardholder = it.take(60) },
                            label = { Text("Name on card") }, singleLine = true, colors = editFieldColors,
                            shape = RoundedCornerShape(15.dp), modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = editCardNumber, onValueChange = { input ->
                            if (paymentCard) {
                                val digits = input.filter(Char::isDigit)
                                if (!editNetworkSelectedByUser) editNetwork = cardNetwork(digits)
                                editCardNumber = formatPaymentCardNumber(digits, editNetwork)
                            } else editCardNumber = input.take(40)
                        }, label = { Text(if (paymentCard) "Card number *" else "Card or ID number") },
                            keyboardOptions = KeyboardOptions(keyboardType = if (paymentCard) KeyboardType.Number else KeyboardType.Text),
                            isError = editValidationAttempted && editValidation.numberError != null,
                            singleLine = true, colors = editFieldColors, shape = RoundedCornerShape(15.dp),
                            modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = editExpiryDigits,
                            onValueChange = { editExpiryDigits = cardExpiryDigits(it) },
                            label = { Text(if (paymentCard) "Expiry *" else "Expiry") },
                            placeholder = { Text("MM/YY") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            visualTransformation = ExpiryVisualTransformation,
                            isError = editValidationAttempted && editValidation.expiryError != null,
                            singleLine = true, colors = editFieldColors, shape = RoundedCornerShape(15.dp),
                            modifier = Modifier.fillMaxWidth())
                        CardNetworkDropdown(editNetwork, paymentCard,
                            editValidationAttempted && editValidation.networkError != null,
                            editFieldColors, Modifier.fillMaxWidth()) { selected ->
                            editNetworkSelectedByUser = true
                            editNetwork = selected
                            if (paymentCard) editCardNumber = formatPaymentCardNumber(editCardNumber, editNetwork)
                        }
                    }
                    if (item.category == VaultCategory.DOCUMENT) {
                        Text("Document group", style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            listOf("Identity", "Medical", "Bills", "Insurance", "Other").forEach { option ->
                                TextButton(onClick = { editCategory = option }) { Text(if (editCategory == option) "✓ $option" else option, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            }
                        }
                    }
                }
            },
            dismissButton = { TextButton(onClick = { editing = false }) { Text("Cancel") } },
            confirmButton = {
                TextButton(onClick = {
                    if (item.category == VaultCategory.CARD && !editValidation.isValid) {
                        editValidationAttempted = true
                        return@TextButton
                    }
                    val digits = editCardNumber.filter(Char::isDigit)
                    val lastFour = if (paymentCard) digits.takeLast(4) else editCardNumber.takeLast(4)
                    val metadata = when (item.category) {
                        VaultCategory.DOCUMENT -> item.metadata + ("documentCategory" to editCategory.ifBlank { "Other" })
                        VaultCategory.CARD -> item.metadata + mapOf("issuer" to editIssuer.trim(), "cardholder" to editCardholder.trim(),
                            "cardNumber" to (if (paymentCard) digits else editCardNumber.trim()),
                            "expiry" to editExpiry, "network" to editNetwork.trim(),
                            "lastFour" to lastFour.ifBlank { item.lastFour })
                        else -> item.metadata
                    }
                    onEdit(item.copy(title = editTitle.trim().ifBlank { item.title }, metadata = metadata,
                        lastFour = if (item.category == VaultCategory.CARD) lastFour.ifBlank { item.lastFour } else item.lastFour))
                    editing = false
                }) { Text("Save") }
            }
        )
    }
}

@Composable
private fun CardDetailDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x1992A2BB)))
}

@Composable
private fun DetailField(label: String, value: String, onCopy: () -> Unit, onReveal: (() -> Unit)?, revealed: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = LockrColors.TextSecondary)
            Text(value, style = MaterialTheme.typography.bodyLarge, color = LockrColors.TextPrimary)
        }
        if (onReveal != null) IconButton(onClick = onReveal, modifier = Modifier.size(42.dp)) {
            Icon(if (revealed) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, contentDescription = if (revealed) "Hide $label" else "Reveal $label", modifier = Modifier.size(20.dp))
        }
        IconButton(onClick = onCopy, modifier = Modifier.size(42.dp)) { Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy $label", modifier = Modifier.size(20.dp)) }
    }
}

@Composable
private fun OcrTextCard(text: String, onCopy: () -> Unit) {
    var expanded by rememberSaveable(text) { mutableStateOf(false) }
    GlassPanel(Modifier.fillMaxWidth(), radius = 22.dp) {
        Column(Modifier.fillMaxWidth().padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Extracted text", style = MaterialTheme.typography.titleMedium)
                    Text("Recognized on this device", color = LockrColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = onCopy) { Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy extracted text") }
            }
            Text(text, style = MaterialTheme.typography.bodyMedium, color = LockrColors.TextPrimary,
                maxLines = if (expanded) Int.MAX_VALUE else 5, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 9.dp))
            if (text.length > 240 || text.count { it == '\n' } > 5) {
                TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Show less" else "Show all text") }
            }
        }
    }
}

private fun String.toFieldLabel(): String = when (this) {
    "cardNumber" -> "Card number"
    "idNumber" -> "ID number"
    "documentNumber" -> "Document number"
    "holderName" -> "Name"
    "cardholder" -> "Name on card"
    "issuer" -> "Issuer"
    "expiry" -> "Expiry"
    "website" -> "Website or app"
    "username" -> "Username"
    "documentCategory" -> "Document group"
    "notes" -> "Notes"
    else -> replace(Regex("([a-z])([A-Z])"), "$1 $2").replaceFirstChar(Char::uppercase)
}

private fun copyPrivateText(context: Context, value: String, autoClear: Boolean) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Stash", value))
    if (autoClear) {
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (clipboard.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString() == value) {
                clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
            }
        }, 30_000L)
    }
}

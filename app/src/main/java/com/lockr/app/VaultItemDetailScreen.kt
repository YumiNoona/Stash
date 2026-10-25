package com.lockr.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
    var editTitle by rememberSaveable(item.id) { mutableStateOf(item.title) }
    var editCategory by remember(item.id) { mutableStateOf(item.metadata["documentCategory"].orEmpty()) }
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
        StashTopBar(
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
                CardDetailPreview(item, Modifier.fillMaxWidth().padding(top = 3.dp))
            }
            if (item.metadata.isNotEmpty()) {
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
            if (item.attachments.isNotEmpty() && (item.category !in setOf(VaultCategory.CARD, VaultCategory.PASSWORD) || authorized)) {
                Column {
                    Text("Original files", style = MaterialTheme.typography.titleMedium, color = LockrColors.TextPrimary, modifier = Modifier.padding(bottom = 7.dp))
                    VaultAttachmentsSection(item.attachments, Modifier.fillMaxWidth())
                }
            } else if (item.attachments.isNotEmpty()) {
                Button(onClick = onAuthenticate, modifier = Modifier.fillMaxWidth()) { Text("Unlock attached file") }
            }
            if (feedback.isNotBlank()) Text(feedback, style = MaterialTheme.typography.bodySmall, color = LockrColors.Accent)
            Spacer(Modifier.height(20.dp))
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ${item.title}?") },
            text = { Text("This removes the item and its attached files from this device.", color = LockrColors.TextSecondary) },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete(item) }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            }
        )
    }
    if (editing) {
        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text("Edit item") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = editTitle, onValueChange = { editTitle = it.take(80) }, label = { Text("Title") }, singleLine = true)
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
                    val metadata = if (item.category == VaultCategory.DOCUMENT) item.metadata + ("documentCategory" to editCategory.ifBlank { "Other" }) else item.metadata
                    onEdit(item.copy(title = editTitle.trim().ifBlank { item.title }, metadata = metadata))
                    editing = false
                }) { Text("Save") }
            }
        )
    }
}

@Composable
private fun DetailField(label: String, value: String, onCopy: () -> Unit, onReveal: (() -> Unit)?, revealed: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = LockrColors.TextSecondary)
            Text(value, style = MaterialTheme.typography.bodyLarge, color = LockrColors.TextPrimary)
        }
        if (onReveal != null) IconButton(onClick = onReveal) {
            Icon(if (revealed) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, contentDescription = if (revealed) "Hide $label" else "Reveal $label")
        }
        IconButton(onClick = onCopy) { Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy $label") }
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

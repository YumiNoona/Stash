package com.lockr.app

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun ReviewItemScreen(
    attachments: List<VaultAttachment>,
    onBack: () -> Unit,
    onSave: (String, VaultCategory, Map<String, String>, List<VaultAttachment>) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var title by remember(attachments) { mutableStateOf(suggestedItemTitle(attachments.firstOrNull()?.name.orEmpty(), "")) }
    var documentGroup by remember(attachments) { mutableStateOf("Other") }
    var extractedText by remember(attachments) { mutableStateOf("") }
    var isRecognizing by remember(attachments) { mutableStateOf(true) }
    var ocrMessage by remember(attachments) { mutableStateOf("Reading text on this device…") }
    var showExtractedText by remember { mutableStateOf(false) }

    LaunchedEffect(attachments) {
        isRecognizing = true
        val extracted = mutableListOf<String>()
        var anySupported = false
        attachments.forEach { attachment ->
            try {
                val text = recognizeAttachmentText(context, attachment)
                if (text.isNotBlank()) {
                    anySupported = true
                    extracted += "${attachment.name}\n${redactPaymentSecrets(text)}"
                } else if (attachment.mimeType.startsWith("image/") || attachment.mimeType == "application/pdf" || attachment.mimeType.startsWith("text/")) {
                    anySupported = true
                }
            } catch (_: Exception) {
                // The original attachment remains savable if local OCR cannot process it.
            }
        }
        extractedText = extracted.joinToString("\n\n").take(120_000)
        documentGroup = suggestedDocumentGroup(attachments.firstOrNull()?.name.orEmpty(), extractedText)
        title = suggestedItemTitle(attachments.firstOrNull()?.name.orEmpty(), extractedText)
        ocrMessage = when {
            extractedText.isNotBlank() -> "Text extracted locally · review before saving"
            anySupported -> "No readable text found. The original file can still be saved."
            else -> "Text recognition is not available for this file type. The original can still be saved."
        }
        isRecognizing = false
    }

    Column(modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        StashTopBar(title = "Review document", subtitle = "Check the file before saving", onBack = onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            attachments.firstOrNull()?.let { first ->
                VaultAttachmentPreview(first, Modifier.fillMaxWidth().height(272.dp))
            }
            if (attachments.size > 1) {
                Text("${attachments.size} pages or files", color = LockrColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
                VaultAttachmentsSection(attachments.drop(1), Modifier.fillMaxWidth())
            }
            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(80) },
                label = { Text("Title") },
                singleLine = true,
                shape = RoundedCornerShape(17.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Column {
                Text("Document group", style = MaterialTheme.typography.titleSmall, color = LockrColors.TextPrimary)
                Column(Modifier.padding(top = 5.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    listOf("Identity", "Medical", "Bills", "Insurance", "Other").chunked(3).forEach { groupRow ->
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            groupRow.forEach { group ->
                                FilterChip(selected = documentGroup == group, onClick = { documentGroup = group }, label = { Text(group, maxLines = 1, softWrap = false) })
                            }
                        }
                    }
                }
            }
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = 0.67f)).padding(14.dp)
            ) {
                Text("On-device recognition", style = MaterialTheme.typography.titleSmall, color = LockrColors.TextPrimary)
                Text(ocrMessage, style = MaterialTheme.typography.bodySmall, color = LockrColors.TextSecondary, modifier = Modifier.padding(top = 4.dp))
                if (extractedText.isNotBlank()) {
                    Text(
                        if (showExtractedText) "Hide extracted text" else "Review extracted text",
                        color = LockrColors.Accent,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.clickable { showExtractedText = !showExtractedText }.padding(top = 10.dp)
                    )
                    if (showExtractedText) {
                        Text(extractedText, style = MaterialTheme.typography.bodySmall, color = LockrColors.TextPrimary, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
            Text("Recognition is a suggestion. Your original files stay attached to the saved item.", color = LockrColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
        }
        Button(
            onClick = {
                val metadata = buildMap {
                    if (extractedText.isNotBlank()) put("ocrText", extractedText)
                    put("documentCategory", documentGroup)
                }
                onSave(title.trim().ifBlank { "Imported document" }, VaultCategory.DOCUMENT, metadata, attachments)
            },
            enabled = title.isNotBlank() && !isRecognizing,
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 10.dp).height(54.dp)
        ) {
            Text(if (isRecognizing) "Recognizing…" else "Save document")
        }
    }
}

@Composable
fun ManualItemScreen(
    category: VaultCategory,
    onBack: () -> Unit,
    onSave: (String, VaultCategory, String, Map<String, String>) -> Unit,
    modifier: Modifier = Modifier
) {
    var title by remember(category) { mutableStateOf("") }
    var website by remember(category) { mutableStateOf("") }
    var username by remember(category) { mutableStateOf("") }
    var password by remember(category) { mutableStateOf("") }
    var note by remember(category) { mutableStateOf("") }
    var showPassword by remember(category) { mutableStateOf(false) }
    val isNote = category == VaultCategory.NOTE

    Column(modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        StashTopBar(title = if (isNote) "New note" else "New password", subtitle = "Saved only in your encrypted vault", onBack = onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            OutlinedTextField(value = title, onValueChange = { title = it.take(80) }, label = { Text(if (isNote) "Note title" else "Account name") }, singleLine = true, shape = RoundedCornerShape(17.dp), modifier = Modifier.fillMaxWidth())
            if (isNote) {
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Note") }, minLines = 7, shape = RoundedCornerShape(17.dp), modifier = Modifier.fillMaxWidth())
            } else {
                OutlinedTextField(value = website, onValueChange = { website = it }, label = { Text("Website or app (optional)") }, singleLine = true, shape = RoundedCornerShape(17.dp), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Username (optional)") }, singleLine = true, shape = RoundedCornerShape(17.dp), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = if (showPassword) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    trailingIcon = {
                        androidx.compose.material3.IconButton(onClick = { showPassword = !showPassword }) {
                            androidx.compose.material3.Icon(
                                if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = if (showPassword) "Hide password" else "Show password"
                            )
                        }
                    },
                    shape = RoundedCornerShape(17.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Passwords are encrypted and excluded from vault search.", color = LockrColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
        }
        Button(
            onClick = {
                val metadata = if (isNote) mapOf("content" to note) else mapOf("website" to website, "username" to username, "password" to password).filterValues(String::isNotBlank)
                onSave(title.trim(), category, if (isNote) "Private note" else "Password", metadata)
            },
            enabled = title.isNotBlank() && if (isNote) note.isNotBlank() else password.isNotBlank(),
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 10.dp).height(54.dp)
        ) { Text("Save to vault") }
    }
}

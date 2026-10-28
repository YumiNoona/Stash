package com.lockr.app

import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.TextSnippet
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewItemScreen(
    attachments: List<VaultAttachment>,
    initialTitle: String?,
    initialGroup: String?,
    onDraftChange: (String?, String?) -> Unit,
    onBack: () -> Unit,
    onAddPhotos: () -> Unit,
    onAddFiles: () -> Unit,
    onAddCamera: () -> Unit,
    onRemovePage: (Int) -> Unit,
    onCorrectPage: (Int, VaultAttachment) -> Unit,
    onSave: (String, VaultCategory, Map<String, String>, List<VaultAttachment>) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val ocrEnabled = LocalOcrOnImport.current
    var title by remember { mutableStateOf(initialTitle ?: suggestedItemTitle(attachments.firstOrNull()?.name.orEmpty(), "")) }
    var titleTouched by remember { mutableStateOf(initialTitle != null) }
    var documentGroup by remember { mutableStateOf(initialGroup ?: "Other") }
    var groupTouched by remember { mutableStateOf(initialGroup != null) }
    var extractedText by remember { mutableStateOf("") }
    var rawRecognizedText by remember { mutableStateOf("") }
    var isRecognizing by remember { mutableStateOf(true) }
    var showAddPages by remember { mutableStateOf(false) }
    var correctIndex by remember { mutableIntStateOf(-1) }

    LaunchedEffect(attachments) {
        if (!ocrEnabled) { isRecognizing = false; return@LaunchedEffect }
        isRecognizing = true
        val extracted = mutableListOf<String>()
        attachments.forEach { attachment ->
            try {
                val text = recognizeAttachmentText(context, attachment)
                if (text.isNotBlank()) {
                    extracted += "${attachment.name}\n$text"
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) {
                // The original attachment remains savable if local OCR cannot process it.
            }
        }
        rawRecognizedText = extracted.joinToString("\n\n").take(120_000)
        extractedText = redactPaymentSecrets(redactAadhaarNumbers(rawRecognizedText))
        if (!groupTouched) documentGroup = suggestedDocumentGroup(attachments.firstOrNull()?.name.orEmpty(), extractedText)
        if (!titleTouched) title = suggestedItemTitle(attachments.firstOrNull()?.name.orEmpty(), extractedText)
        isRecognizing = false
    }

    Box(modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 88.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                val pageState = rememberPagerState(pageCount = { attachments.size })
                if (attachments.isNotEmpty()) {
                    HorizontalPager(pageState, Modifier.fillMaxWidth().height(285.dp)) { index ->
                        DocumentPreviewCanvas(attachments[index], Modifier.fillMaxSize(), showPageControls = false)
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text("${pageState.currentPage + 1} / ${attachments.size} pages", color = LockrColors.TextSecondary,
                            style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                        IconButton(onClick = { showAddPages = true }) {
                            Icon(Icons.Outlined.Add, "Add more pages", tint = LockrColors.Accent)
                        }
                        IconButton(onClick = { correctIndex = pageState.currentPage.coerceIn(0, attachments.lastIndex) },
                            enabled = attachments[pageState.currentPage.coerceIn(0, attachments.lastIndex)].mimeType.startsWith("image/")) {
                            Icon(Icons.Outlined.Crop, "Correct page edges", tint = LockrColors.Accent)
                        }
                        IconButton(onClick = { onRemovePage(pageState.currentPage.coerceIn(0, attachments.lastIndex)) },
                            enabled = attachments.size > 1) {
                            Icon(Icons.Outlined.DeleteOutline, "Remove this page",
                                tint = if (attachments.size > 1) MaterialTheme.colorScheme.error else LockrColors.TextSecondary)
                        }
                    }
                }
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(25.dp))
                    .background(LockrColors.Surface).border(1.dp, LockrColors.Surface, RoundedCornerShape(25.dp))
                    .padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Document name", style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp),
                        color = LockrColors.TextPrimary)
                    OutlinedTextField(value = title, onValueChange = { title = it.take(80); titleTouched = true; onDraftChange(title, if (groupTouched) documentGroup else null) },
                        placeholder = { Text("Name this document", color = LockrColors.TextSecondary) },
                        singleLine = true, textStyle = MaterialTheme.typography.bodyLarge,
                        shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = LockrColors.Surface, unfocusedContainerColor = LockrColors.Surface,
                            focusedTextColor = LockrColors.TextPrimary, unfocusedTextColor = LockrColors.TextPrimary,
                            focusedBorderColor = LockrColors.Accent, unfocusedBorderColor = Color(0xFFBECBD8),
                            cursorColor = LockrColors.Accent))
                    TagEditor(VaultCategory.DOCUMENT, setOf(documentGroup),
                        onChange = { documentGroup = it.firstOrNull() ?: "Other"; groupTouched = true; onDraftChange(if (titleTouched) title else null, documentGroup) }, single = true)

                }
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
                Text(if (isRecognizing) "Reading file…" else "Save document")
            }

        }
        StashTopBar(title = "Review document", onBack = onBack)
    }

    if (showAddPages) ModalBottomSheet(onDismissRequest = { showAddPages = false },
        containerColor = LockrColors.Surface, shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)) {
        Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Add pages", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))
            listOf(Triple(Icons.Outlined.CameraAlt, "Scan another page", onAddCamera),
                Triple(Icons.Outlined.AddPhotoAlternate, "Choose photos", onAddPhotos),
                Triple(Icons.Outlined.InsertDriveFile, "Choose files", onAddFiles)).forEach { (icon, label, action) ->
                TextButton(onClick = { showAddPages = false; action() }, modifier = Modifier.fillMaxWidth()) {
                    Icon(icon, null); Text(label, modifier = Modifier.weight(1f).padding(start = 14.dp))
                }
            }
        }
    }
    if (correctIndex in attachments.indices) DocumentEdgeCorrectionSheet(attachments[correctIndex],
        onDismiss = { correctIndex = -1 }, onApply = { corrected ->
            onCorrectPage(correctIndex, corrected)
            correctIndex = -1
        })

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
    var showGenerator by remember(category) { mutableStateOf(false) }
    val isNote = category == VaultCategory.NOTE

    Box(modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 88.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
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
                    TextButton(onClick = { showGenerator = true }) {
                        Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("Generate password", modifier = Modifier.padding(start = 8.dp))
                    }
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
        StashTopBar(title = if (isNote) "New note" else "New password", subtitle = "Saved only in your encrypted vault", onBack = onBack)
    }
    if (showGenerator) {
        PasswordGeneratorDialog(
            onDismiss = { showGenerator = false },
            onUse = { generated -> password = generated; showPassword = false; showGenerator = false }
        )
    }
}

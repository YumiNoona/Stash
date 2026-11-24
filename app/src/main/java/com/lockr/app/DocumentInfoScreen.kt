package com.lockr.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.IosShare
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DocumentInfoScreen(item: VaultItem, onBack: () -> Unit) {
    val date = if (item.createdAt > 0) Date(item.createdAt) else null
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var target by remember(item.id) { mutableStateOf<VaultAttachment?>(null) }
    var message by remember(item.id) { mutableStateOf("") }
    val download = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val attachment = target
        target = null
        if (uri != null && attachment != null) scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val bytes = android.util.Base64.decode(attachment.base64Data, android.util.Base64.DEFAULT)
                    try { writeExportDestination({ context.contentResolver.openOutputStream(uri) }) { it.write(bytes) } }
                    finally { bytes.fill(0) }
                }
                message = "Original file saved."
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) { message = "Could not save this copy. The original in Stash is unchanged." }
        }
    }
    val saveOriginal: (VaultAttachment) -> Unit = { target = it; download.launch(it.name) }
    Box(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 88.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                GlassPanel(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(17.dp)) {
                        Text(item.title, style = MaterialTheme.typography.titleMedium, color = LockrColors.TextPrimary)
                        item.attachments.firstOrNull()?.let {
                            InfoEntry("File name", it.name); InfoEntry("Type", attachmentMime(it))
                            OriginalFileActions(it, { saveOriginal(it) }, { shareVaultAttachment(context, it) })
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            InfoEntry("Date added", date?.let { DateFormat.getDateInstance(DateFormat.MEDIUM).format(it) }
                                ?: item.time.ifBlank { "Not recorded" }, Modifier.weight(1f))
                            InfoEntry("Time added", date?.let { DateFormat.getTimeInstance(DateFormat.SHORT).format(it) }
                                ?: "Not recorded", Modifier.weight(1f))
                        }
                        InfoEntry("Size", documentSizeMb(item.attachments.sumOf { it.byteCount }))
                        InfoEntry("Storage", "Encrypted on this device")
                        if (item.attachments.size > 1) InfoEntry("Files", item.attachments.size.toString())
                    }
                }
                if (item.attachments.size > 1) item.attachments.drop(1).forEachIndexed { index, attachment ->
                    GlassPanel(Modifier.fillMaxWidth(), radius = 20.dp) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("File ${index + 2}", style = MaterialTheme.typography.labelLarge, color = LockrColors.Accent)
                            InfoEntry(attachment.name, "${attachmentMime(attachment)} · ${documentSizeMb(attachment.byteCount)}")
                            OriginalFileActions(attachment, { saveOriginal(attachment) }, { shareVaultAttachment(context, attachment) })
                        }
                    }
                }
                if (message.isNotBlank()) Text(message, color = LockrColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(16.dp))
            }

        }
        StashTopBar("File information", onBack = onBack)
    }
}

private fun documentSizeMb(bytes: Long): String = String.format(Locale.getDefault(), "%.2f MB", bytes.coerceAtLeast(0) / 1_000_000.0)

@Composable
private fun InfoEntry(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, color = LockrColors.TextSecondary, style = MaterialTheme.typography.labelMedium)
        Text(value, color = LockrColors.TextPrimary, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun OriginalFileActions(attachment: VaultAttachment, download: () -> Unit, share: () -> Unit) {
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text("Original file", Modifier.weight(1f), color = LockrColors.TextSecondary, style = MaterialTheme.typography.labelMedium)
        IconButton(onClick = download) { Icon(Icons.Outlined.Download, "Download original ${attachment.name}", tint = LockrColors.TextPrimary) }
        IconButton(onClick = share) { Icon(Icons.Outlined.IosShare, "Share original ${attachment.name}", tint = LockrColors.TextPrimary) }
    }
}

package com.lockr.app

import android.text.format.Formatter
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.text.DateFormat
import java.util.Date

@Composable
fun DocumentInfoScreen(item: VaultItem, onBack: () -> Unit, onGroupChange: (String) -> Unit) {
    val context = LocalContext.current
    val added = if (item.createdAt > 0) DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(item.createdAt)) else item.time
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        StashTopBar("File information", onBack = onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            GlassPanel(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Text(item.title, style = MaterialTheme.typography.titleMedium, color = LockrColors.TextPrimary)
                    InfoEntry("Added to Stash", added.ifBlank { "Date unavailable" })
                    InfoEntry("Total size", Formatter.formatFileSize(context, item.attachments.sumOf { it.byteCount }))
                    InfoEntry("Files", item.attachments.size.toString())
                    InfoEntry("Storage", "Encrypted on this device")
                }
            }
            TagEditor(VaultCategory.DOCUMENT, setOf(item.metadata["documentCategory"].orEmpty().ifBlank { "Other" }),
                onChange = { onGroupChange(it.firstOrNull() ?: "Other") }, single = true)
            item.attachments.forEachIndexed { index, attachment ->
                GlassPanel(Modifier.fillMaxWidth(), radius = 20.dp) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("File ${index + 1}", style = MaterialTheme.typography.labelLarge, color = LockrColors.Accent)
                        InfoEntry("Name", attachment.name)
                        InfoEntry("Type", attachment.mimeType)
                        InfoEntry("Size", Formatter.formatFileSize(context, attachment.byteCount))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun InfoEntry(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, color = LockrColors.TextSecondary, style = MaterialTheme.typography.labelMedium)
        Text(value, color = LockrColors.TextPrimary, style = MaterialTheme.typography.bodyMedium)
    }
}

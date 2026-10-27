package com.lockr.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DocumentInfoScreen(item: VaultItem, onBack: () -> Unit) {
    val date = if (item.createdAt > 0) Date(item.createdAt) else null
    Box(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 88.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                GlassPanel(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(17.dp)) {
                        Text(item.title, style = MaterialTheme.typography.titleMedium, color = LockrColors.TextPrimary)
                        item.attachments.firstOrNull()?.let { InfoEntry("File name", it.name); InfoEntry("Type", it.mimeType) }
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
                            InfoEntry(attachment.name, "${attachment.mimeType} · ${documentSizeMb(attachment.byteCount)}")
                        }
                    }
                }
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

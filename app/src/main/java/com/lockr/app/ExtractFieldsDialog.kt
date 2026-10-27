package com.lockr.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun ExtractFieldsDialog(recognizedText: String, onDismiss: () -> Unit, onCopy: (String) -> Unit,
                        onSave: (String, String) -> Unit) {
    val suggestions = remember(recognizedText) { suggestDocumentFields(recognizedText) }
    val lines = remember(recognizedText) { selectableOcrLines(recognizedText).take(60) }
    val values = remember(recognizedText) {
        mutableStateMapOf<String, String>().apply { suggestions.forEach { this[it.key] = it.value }; this["extractedSnippet"] = "" }
    }
    var key by remember(recognizedText) { mutableStateOf(suggestions.firstOrNull()?.key ?: "extractedSnippet") }
    var feedback by remember { mutableStateOf("") }
    val label = suggestions.firstOrNull { it.key == key }?.label ?: "Selected text"
    val value = values[key].orEmpty()
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.padding(16.dp).widthIn(max = 560.dp).fillMaxWidth().fillMaxHeight(0.87f).imePadding()
            .clip(RoundedCornerShape(28.dp)).background(LockrColors.Surface)) {
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Extract fields", color = LockrColors.TextPrimary, style = MaterialTheme.typography.titleLarge)
                    Text("Select a value, then check it", color = LockrColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, "Close extraction") }
            }
            HorizontalDivider(Modifier.padding(top = 12.dp), color = LockrColors.Line)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (suggestions.isNotEmpty()) {
                    Text("Detected fields", style = MaterialTheme.typography.labelLarge, color = LockrColors.TextSecondary)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        suggestions.forEach { field -> FilterChip(key == field.key,
                            { key = field.key; feedback = "" }, label = { Text(field.label) }) }
                        FilterChip(key == "extractedSnippet", { key = "extractedSnippet" }, label = { Text("Text line") })
                    }
                }
                OutlinedTextField(value, { values[key] = it.take(1000); feedback = "" },
                    label = { Text(label) }, placeholder = { Text("Choose a line below or enter a value") },
                    minLines = 2, maxLines = 5, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth())
                Text("Check the selected value against your document before saving.", style = MaterialTheme.typography.bodySmall, color = LockrColors.TextSecondary)
                if (lines.isNotEmpty()) {
                    Text("Recognized lines · ${lines.size}", style = MaterialTheme.typography.labelLarge, color = LockrColors.TextSecondary)
                    lines.forEachIndexed { index, line ->
                        val selected = key == "extractedSnippet" && value == line
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else LockrColors.Raised)
                            .clickable { key = "extractedSnippet"; values[key] = line; feedback = "" }
                            .padding(horizontal = 12.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text((index + 1).toString().padStart(2, '0'), color = LockrColors.Accent,
                                style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(32.dp))
                            Text(line, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                                color = LockrColors.TextPrimary, style = MaterialTheme.typography.bodyMedium)
                            if (selected) Icon(Icons.Outlined.Check, "Selected", Modifier.padding(start = 8.dp).size(18.dp), tint = LockrColors.Accent)
                        }
                    }
                } else Text("No readable lines found. You can enter a value above.", color = LockrColors.TextSecondary)
            }
            if (feedback.isNotBlank()) Text(feedback, color = LockrColors.Accent, style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 20.dp))
            HorizontalDivider(color = LockrColors.Line)
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(enabled = value.isNotBlank(), onClick = { onCopy(value.trim()); feedback = "$label copied" },
                    modifier = Modifier.weight(1f).height(48.dp), contentPadding = PaddingValues(horizontal = 12.dp)) {
                    Icon(Icons.Outlined.ContentCopy, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Copy")
                }
                Button(enabled = value.isNotBlank(), onClick = { onSave(key, value.trim()) },
                    modifier = Modifier.weight(1f).height(48.dp), contentPadding = PaddingValues(horizontal = 12.dp)) {
                    Icon(Icons.Outlined.SaveAlt, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Save")
                }
            }
        }
    }
}

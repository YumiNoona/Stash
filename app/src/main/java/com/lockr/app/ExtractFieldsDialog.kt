package com.lockr.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** Explicit review: OCR never silently copies or saves an entire document. */
@Composable
fun ExtractFieldsDialog(
    recognizedText: String,
    onDismiss: () -> Unit,
    onCopy: (String) -> Unit,
    onSave: (String, String) -> Unit
) {
    val suggestions = remember(recognizedText) { suggestDocumentFields(recognizedText) }
    val ocrLines = remember(recognizedText) { selectableOcrLines(recognizedText) }
    val values = remember(recognizedText) {
        mutableStateMapOf<String, String>().apply {
            suggestions.forEach { this[it.key] = it.value }
            this["extractedSnippet"] = ""
        }
    }
    var selectedKey by remember(recognizedText) { mutableStateOf(suggestions.firstOrNull()?.key ?: "extractedSnippet") }
    var showLines by remember(recognizedText) { mutableStateOf(suggestions.isEmpty()) }
    var showFullText by remember(recognizedText) { mutableStateOf(false) }
    var feedback by remember(recognizedText) { mutableStateOf("") }
    val selectedLabel = suggestions.firstOrNull { it.key == selectedKey }?.label ?: "Selected text"
    val selectedValue = values[selectedKey].orEmpty()

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier.fillMaxWidth().fillMaxHeight(0.77f).padding(horizontal = 14.dp)
                .clip(RoundedCornerShape(29.dp))
                .background(Color(0xFFFAFBFF))
                .border(1.dp, Color.White, RoundedCornerShape(29.dp))
                .padding(18.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Extract a field", style = MaterialTheme.typography.titleLarge, color = LockrColors.TextPrimary)
                    Text("Choose, check, then copy or save", style = MaterialTheme.typography.bodySmall,
                        color = LockrColors.TextSecondary)
                }
                IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, contentDescription = "Close extraction") }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 15.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (suggestions.isNotEmpty()) {
                    Text("FOUND IN THIS FILE", style = MaterialTheme.typography.labelSmall, color = LockrColors.TextSecondary)
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        suggestions.forEach { field ->
                            ExtractionChoice(field.label, selectedKey == field.key) {
                                selectedKey = field.key; feedback = ""
                            }
                        }
                    }
                } else {
                    Text("No known fields detected. Choose a recognized line below or edit your own selection.",
                        style = MaterialTheme.typography.bodySmall, color = LockrColors.TextSecondary)
                }
                ExtractionChoice("Choose a line", selectedKey == "extractedSnippet") {
                    selectedKey = "extractedSnippet"; showLines = true; feedback = ""
                }
                OutlinedTextField(
                    value = selectedValue,
                    onValueChange = { values[selectedKey] = it.take(1000); feedback = "" },
                    label = { Text(selectedLabel) },
                    supportingText = { Text("Check OCR carefully before using this value.") },
                    minLines = if (selectedKey == "address") 3 else 1,
                    maxLines = 7,
                    shape = RoundedCornerShape(17.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                if (showLines && ocrLines.isNotEmpty()) {
                    Text("TAP A RECOGNIZED LINE", style = MaterialTheme.typography.labelSmall, color = LockrColors.TextSecondary)
                    ocrLines.forEach { line ->
                        Text(line, style = MaterialTheme.typography.bodySmall, color = LockrColors.TextPrimary,
                            maxLines = 2, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp))
                                .background(Color.White.copy(alpha = 0.72f))
                                .clickable { selectedKey = "extractedSnippet"; values[selectedKey] = line; feedback = "" }
                                .padding(horizontal = 12.dp, vertical = 10.dp))
                    }
                }
                TextButton(onClick = { showFullText = !showFullText }) {
                    Text(if (showFullText) "Hide full OCR text" else "Review full OCR text")
                }
                if (showFullText) Text(recognizedText.take(10_000), style = MaterialTheme.typography.bodySmall,
                    color = LockrColors.TextSecondary,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)
                        .clip(RoundedCornerShape(13.dp)).background(Color.White.copy(alpha = 0.72f)).padding(12.dp))
            }
            if (feedback.isNotBlank()) Text(feedback, style = MaterialTheme.typography.bodySmall,
                color = LockrColors.Accent, modifier = Modifier.padding(top = 8.dp))
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                OutlinedButton(onClick = { onCopy(selectedValue.trim()); feedback = "$selectedLabel copied" },
                    enabled = selectedValue.isNotBlank(), modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = null)
                    Text("Copy", modifier = Modifier.padding(start = 6.dp))
                }
                Button(onClick = { onSave(selectedKey, selectedValue.trim()); feedback = "" },
                    enabled = selectedValue.isNotBlank(), modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.SaveAlt, contentDescription = null)
                    Text("Save field", modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
    }
}

@Composable
private fun ExtractionChoice(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(13.dp)
    Text(label, style = MaterialTheme.typography.labelMedium,
        color = if (selected) Color(0xFF0C625D) else LockrColors.TextPrimary,
        modifier = Modifier.clip(shape)
            .background(if (selected) Color(0xFFDDF4F1) else Color.White.copy(alpha = 0.78f))
            .border(1.dp, if (selected) Color(0xFF70BDB5) else Color(0xFFD2DFE8), shape)
            .clickable(onClick = onClick).padding(horizontal = 13.dp, vertical = 10.dp))
}

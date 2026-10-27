package com.lockr.app

import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp

fun normalizedTag(value: String): String = value.trim().replace(Regex("\\s+"), " ").take(24)
fun cardTags(item: VaultItem): Set<String> = item.metadata["tags"].orEmpty().split('\u001f').filter(String::isNotBlank).toSet()
fun encodeTags(tags: Set<String>): String = tags.map(::normalizedTag).filter(String::isNotBlank).distinctBy(String::lowercase).sorted().joinToString("\u001f")
fun withoutTag(item: VaultItem, category: VaultCategory, tag: String): VaultItem =
    if (item.category != category) item else item.copy(metadata = when (category) {
        VaultCategory.CARD -> item.metadata + ("tags" to encodeTags(cardTags(item).filterNot { it.equals(tag, true) }.toSet()))
        VaultCategory.DOCUMENT -> if (item.metadata["documentCategory"].equals(tag, true)) item.metadata + ("documentCategory" to "Other") else item.metadata
        else -> item.metadata
    })

@Stable
class VaultTagCatalog(private val preferences: SharedPreferences) {
    var cards by mutableStateOf(preferences.getStringSet("card_tags", emptySet()).orEmpty().sorted())
        private set
    var documents by mutableStateOf(preferences.getStringSet("document_tags", emptySet()).orEmpty().sorted())
        private set
    var deleting by mutableStateOf(false)
    var onDelete: (VaultCategory, String) -> Unit = { _, _ -> }
    fun custom(category: VaultCategory) = if (category == VaultCategory.CARD) cards else documents
    fun groups() = listOf("Identity", "Medical", "Bills", "Insurance", "Other") + documents
    fun add(category: VaultCategory, value: String): Boolean {
        val tag = normalizedTag(value)
        val reserved = listOf("All", "Credit", "Debit", "ID Cards", "Identity", "Medical", "Bills", "Insurance", "Other")
        if (tag.isBlank() || tag.any(Char::isISOControl) || (reserved + custom(category)).any { it.equals(tag, true) }) return false
        save(category, custom(category) + tag)
        return true
    }
    fun remove(category: VaultCategory, tag: String) = save(category, custom(category).filterNot { it.equals(tag, true) })
    private fun save(category: VaultCategory, labels: List<String>) {
        val sorted = labels.sortedBy(String::lowercase)
        if (category == VaultCategory.CARD) cards = sorted else documents = sorted
        preferences.edit().putStringSet(if (category == VaultCategory.CARD) "card_tags" else "document_tags", sorted.toSet()).apply()
    }
}

val LocalTagCatalog = staticCompositionLocalOf<VaultTagCatalog> { error("Tag catalog unavailable") }

@Composable
fun CollectionEmptyState(title: String, message: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    GlassPanel(Modifier.fillMaxWidth().height(235.dp).padding(top = 8.dp), radius = 24.dp) {
        Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            Box(Modifier.size(78.dp), contentAlignment = Alignment.Center) {
                IridescentOrb(orbSize = 74.dp)
                Icon(icon, null, Modifier.size(31.dp), tint = LockrColors.Accent)
            }
            Text(title, color = LockrColors.TextPrimary, style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 13.dp))
            Text(message, color = LockrColors.TextSecondary, style = MaterialTheme.typography.bodySmall,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(top = 5.dp))
        }
    }
}

@Composable
fun CollectionFilters(options: List<String>, selected: String, onSelect: (String) -> Unit,
                      category: VaultCategory? = null) {
    // Tag and folder management lives in Settings so filters remain focused on browsing.
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            options.forEach { label ->
                val active = label == selected
                val shape = RoundedCornerShape(14.dp)
                Box(Modifier.height(44.dp).shadow(if (active) 5.dp else 1.dp, shape)
                    .clip(shape).background(if (active) MaterialTheme.colorScheme.primaryContainer else LockrColors.Surface)
                    .border(1.dp, if (active) LockrColors.Accent.copy(alpha = 0.55f) else LockrColors.Line, shape)
                    .clickable { onSelect(label) }.padding(horizontal = 17.dp), contentAlignment = Alignment.Center) {
                    Text(label, style = MaterialTheme.typography.labelLarge,
                        color = if (active) MaterialTheme.colorScheme.onPrimaryContainer else LockrColors.TextSecondary, maxLines = 1)
                }
            }
        }
    }
}

@Composable
fun TagEditor(category: VaultCategory, selected: Set<String>, onChange: (Set<String>) -> Unit,
              single: Boolean = false) {
    val catalog = LocalTagCatalog.current
    val choices = if (category == VaultCategory.DOCUMENT) catalog.groups() else catalog.cards
    LaunchedEffect(choices) {
        val valid = selected.intersect(choices.toSet()).ifEmpty {
            if (single) setOf("Other") else emptySet()
        }
        if (valid != selected) onChange(valid)
    }
    var manage by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(if (single) "Document group" else "Tags", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            TextButton(onClick = { manage = true }) { Icon(Icons.Outlined.Add, null, Modifier.size(17.dp)); Text("Manage") }
        }
        if (choices.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                choices.forEach { tag ->
                    FilterChip(tag in selected, {
                        onChange(if (single) setOf(tag) else if (tag in selected) selected - tag else selected + tag)
                    }, label = { Text(tag) }, shape = RoundedCornerShape(13.dp))
                }
            }
        } else Text("Create tags to organize your cards.", color = LockrColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
    }
    if (manage) TagManagerDialog(category, onDismiss = { manage = false })
}

@Composable
fun TagManagerDialog(category: VaultCategory, onDismiss: () -> Unit) {
    val catalog = LocalTagCatalog.current
    var name by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (category == VaultCategory.CARD) "Card tags" else "Document tags") },
        text = {
            Column(Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Removing a tag clears its assignments. Your saved items remain in the vault.", color = LockrColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(name, { name = it.take(24); error = "" }, label = { Text("New tag") }, singleLine = true)
                if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
                catalog.custom(category).forEach { tag ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(tag, Modifier.weight(1f))
                        IconButton(enabled = !catalog.deleting, onClick = { catalog.onDelete(category, tag) }) {
                            Icon(Icons.Outlined.DeleteOutline, "Delete tag $tag")
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = {
            if (catalog.add(category, name)) { name = ""; error = "" }
            else error = "Enter a unique tag name."
        }) { Text("Add tag") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Done") } })
}

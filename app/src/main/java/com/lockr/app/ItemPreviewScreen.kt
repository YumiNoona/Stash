package com.lockr.app

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ManageSearch
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun ItemPreviewScreen(
    item: VaultItem,
    onBack: () -> Unit,
    onRename: (String) -> Unit,
    onExtract: () -> Unit,
    onSend: () -> Unit,
    onDelete: () -> Unit,
    onDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    var dialog by remember { mutableStateOf("") }
    var title by remember(item.title) { mutableStateOf(item.title) }
    Column(modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        StashTopBar(item.title, item.category.label, onBack = onBack)
        GlassPanel(Modifier.fillMaxWidth().weight(1f).padding(vertical = 12.dp), radius = 25.dp) {
            item.attachments.firstOrNull()?.let { attachment ->
                AttachmentViewer(attachment, Modifier.fillMaxSize().padding(12.dp))
            }
        }
        GlassPanel(Modifier.fillMaxWidth().padding(bottom = 14.dp), radius = 27.dp) {
            Row(
                Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PreviewAction(Icons.Outlined.Edit, "Rename") { title = item.title; dialog = "rename" }
                PreviewAction(Icons.AutoMirrored.Outlined.ManageSearch, "Extract text", onExtract)
                PreviewAction(Icons.Outlined.IosShare, "Send", onSend)
                PreviewAction(Icons.Outlined.MoreHoriz, "Details", onDetails)
                PreviewAction(Icons.Outlined.DeleteOutline, "Delete") { dialog = "delete" }
            }
        }
    }
    if (dialog == "rename") AlertDialog(
        onDismissRequest = { dialog = "" },
        title = { Text("Rename item") },
        text = { OutlinedTextField(value = title, onValueChange = { title = it.take(80) }, singleLine = true, label = { Text("Name") }) },
        dismissButton = { TextButton(onClick = { dialog = "" }) { Text("Cancel") } },
        confirmButton = { TextButton(onClick = { if (title.isNotBlank()) { onRename(title.trim()); dialog = "" } }) { Text("Save") } }
    )
    if (dialog == "delete") AlertDialog(
        onDismissRequest = { dialog = "" },
        title = { Text("Delete this item?") },
        text = { Text("This removes the item and its files from Stash.") },
        dismissButton = { TextButton(onClick = { dialog = "" }) { Text("Cancel") } },
        confirmButton = { TextButton(onClick = { dialog = ""; onDelete() }) { Text("Delete") } }
    )
}

@Composable
private fun AttachmentViewer(attachment: VaultAttachment, modifier: Modifier = Modifier) {
    if (attachment.mimeType != "application/pdf" && !attachment.name.endsWith(".pdf", true)) {
        VaultAttachmentPreview(attachment, modifier)
        return
    }
    val context = LocalContext.current
    var pageIndex by remember(attachment.base64Data) { mutableStateOf(0) }
    var pageCount by remember(attachment.base64Data) { mutableStateOf(0) }
    var bitmap by remember(attachment.base64Data) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(attachment.base64Data, pageIndex) {
        val result = renderPdfAttachmentPage(attachment, pageIndex, context)
        bitmap = result.first
        pageCount = result.second
    }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            if (bitmap != null) Image(bitmap!!.asImageBitmap(), contentDescription = "${attachment.name}, page ${pageIndex + 1}",
                modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            else Text("Opening PDF…", style = MaterialTheme.typography.bodySmall, color = LockrColors.TextSecondary)
        }
        if (pageCount > 1) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { pageIndex-- }, enabled = pageIndex > 0) {
                Icon(Icons.Outlined.ChevronLeft, contentDescription = "Previous page")
            }
            Text("${pageIndex + 1} / $pageCount", style = MaterialTheme.typography.labelLarge)
            IconButton(onClick = { pageIndex++ }, enabled = pageIndex < pageCount - 1) {
                Icon(Icons.Outlined.ChevronRight, contentDescription = "Next page")
            }
        }
    }
}

@Composable
private fun PreviewAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(49.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.72f)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = label, tint = LockrColors.TextPrimary, modifier = Modifier.size(24.dp))
    }
}

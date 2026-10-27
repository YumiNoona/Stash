package com.lockr.app

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun VaultVisualCard(item: VaultItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    if (item.category == VaultCategory.CARD) {
        CardDetailPreview(item, modifier, onClick = onClick)
        return
    }
    val (icon, tint, colors) = when (item.category) {
        VaultCategory.DOCUMENT -> Triple(Icons.Outlined.Description, Color(0xFF6A75B1), listOf(Color(0xFFE8E6FB), Color(0xFFD9F1F7)))
        VaultCategory.PASSWORD -> Triple(Icons.Outlined.Key, Color(0xFF278C82), listOf(Color(0xFFDDF5EF), Color(0xFFE6E8FF)))
        VaultCategory.NOTE -> Triple(Icons.AutoMirrored.Outlined.Article, Color(0xFFC18B42), listOf(Color(0xFFFFEBD7), Color(0xFFF5E2F6)))
        VaultCategory.CARD -> error("Card handled above")
    }
    GlassPanel(modifier.fillMaxWidth().height(132.dp), radius = 23.dp) {
        Row(Modifier.fillMaxSize().clip(RoundedCornerShape(23.dp)).clickable(onClick = onClick).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(108.dp).clip(RoundedCornerShape(17.dp)).background(Brush.linearGradient(colors)),
                contentAlignment = Alignment.Center
            ) {
                val first = item.attachments.firstOrNull()
                if (first != null && item.category != VaultCategory.PASSWORD &&
                    (first.mimeType.startsWith("image/") || first.mimeType == "application/pdf")) {
                    VaultAttachmentPreview(first, Modifier.fillMaxSize())
                } else Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(42.dp))
            }
            Column(Modifier.weight(1f).padding(start = 14.dp), verticalArrangement = Arrangement.Center) {
                Text(item.title, style = MaterialTheme.typography.titleMedium, color = LockrColors.TextPrimary,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(item.category.label, style = MaterialTheme.typography.bodySmall, color = LockrColors.TextSecondary,
                    modifier = Modifier.padding(top = 4.dp))
                Text(item.time, style = MaterialTheme.typography.labelSmall, color = LockrColors.TextSecondary,
                    modifier = Modifier.padding(top = 2.dp))
            }
            Icon(Icons.AutoMirrored.Outlined.ArrowForwardIos, contentDescription = "Open ${item.title}",
                tint = LockrColors.TextSecondary, modifier = Modifier.size(15.dp))
        }
    }
}

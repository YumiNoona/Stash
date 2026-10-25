package com.lockr.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val cardPalettes = listOf(
    listOf(Color(0xFFE2EAFE), Color(0xFFC6EDE7), Color(0xFFF6E5F7)),
    listOf(Color(0xFFFFE8D8), Color(0xFFF4D2E7), Color(0xFFE6E9FF)),
    listOf(Color(0xFFDDF4EB), Color(0xFFCFE7FA), Color(0xFFEDE2FF)),
    listOf(Color(0xFFE7EAF1), Color(0xFFCAD4E5), Color(0xFFF3E5E2))
)

@Composable
fun CardsScreen(
    items: List<VaultItem>,
    autoClearClipboard: Boolean,
    revealTimeout: String,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    var filter by rememberSaveable { mutableStateOf("All") }
    var selectedCard by remember { mutableStateOf<VaultItem?>(null) }
    val cards = items.filter { it.category == VaultCategory.CARD }
    val visibleCards = cards.filter { item ->
        filter == "All" || when (filter) {
            "ID Cards" -> item.detail.contains("ID Card", ignoreCase = true)
            else -> item.detail.equals(filter, ignoreCase = true)
        }
    }

    if (selectedCard != null) {
        CardDetailScreen(
            item = selectedCard!!,
            colors = cardPalettes[cards.indexOf(selectedCard).coerceAtLeast(0) % cardPalettes.size],
            autoClearClipboard = autoClearClipboard,
            revealTimeout = revealTimeout,
            onBack = { selectedCard = null }
        )
    } else {
        Column(modifier.fillMaxSize().padding(horizontal = 22.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RoundIconButton(Icons.AutoMirrored.Outlined.ArrowBack, "Back", onBack)
                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                    Text("Cards", style = MaterialTheme.typography.headlineMedium)
                    Text("Your cards, kept private on this device.", color = LockrColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
                Box(Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onAdd), contentAlignment = Alignment.Center) {
                    IridescentOrb(orbSize = 42.dp)
                    Icon(Icons.Outlined.Add, contentDescription = "Add card", tint = LockrColors.TextPrimary)
                }
            }

            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Credit", "Debit", "ID Cards").forEach { option ->
                    FilterChip(selected = filter == option, onClick = { filter = option }, label = { Text(option) })
                }
            }

            if (visibleCards.isEmpty()) {
                EmptyCardsState(
                    filtered = cards.isNotEmpty(),
                    onAdd = onAdd,
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 18.dp)
                )
            } else {
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(13.dp)
                ) {
                    visibleCards.forEach { card ->
                        VaultCard(card, cardPalettes[cards.indexOf(card) % cardPalettes.size], onClick = { selectedCard = card })
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun CardDetailScreen(
    item: VaultItem,
    colors: List<Color>,
    autoClearClipboard: Boolean,
    revealTimeout: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showOptions by rememberSaveable { mutableStateOf(false) }
    var revealLastFour by rememberSaveable { mutableStateOf(false) }
    var copyStatus by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(revealLastFour, item.title) {
        if (revealLastFour) {
            delay(revealTimeoutMillis(revealTimeout))
            revealLastFour = false
        }
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RoundIconButton(Icons.AutoMirrored.Outlined.ArrowBack, "Back to cards", onBack)
            Spacer(Modifier.weight(1f))
            RoundIconButton(Icons.Outlined.MoreVert, "More options", onClick = { showOptions = true })
        }
        Text("Card details", style = MaterialTheme.typography.headlineMedium)
        Text("Private and encrypted on this device", color = LockrColors.TextSecondary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 2.dp, bottom = 14.dp))
        VaultCard(item = item, colors = colors, onClick = {}, height = 220.dp)

        GlassPanel(Modifier.fillMaxWidth().padding(top = 15.dp), radius = 24.dp) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 15.dp)) {
                CardDetailField("Card Name", item.title) {
                    copyToClipboard(context, item.title, autoClearClipboard)
                    copyStatus = if (autoClearClipboard) "Copied · clears in 30 seconds" else "Copied to clipboard"
                }
                CardDetailField("Card Type", item.detail.ifBlank { "Card" }) {
                    copyToClipboard(context, item.detail.ifBlank { "Card" }, autoClearClipboard)
                    copyStatus = if (autoClearClipboard) "Copied · clears in 30 seconds" else "Copied to clipboard"
                }
                val isIdCard = item.detail == "ID Card"
                val savedNumber = item.metadata[if (isIdCard) "idNumber" else "cardNumber"].orEmpty()
                if (savedNumber.isNotBlank()) {
                    CardDetailField(
                        if (isIdCard) "ID Number" else "Card Number",
                        if (revealLastFour) savedNumber.chunked(if (isIdCard) 5 else 4).joinToString(" ")
                        else if (isIdCard) "••••••••${savedNumber.takeLast(4)}"
                        else "••••  ••••  ••••  ${item.lastFour}",
                        actionIcon = if (revealLastFour) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        actionDescription = if (revealLastFour) "Hide number" else "Reveal number",
                        onAction = { revealLastFour = !revealLastFour }
                    )
                }
                item.metadata[if (isIdCard) "holderName" else "cardholder"]?.takeIf { it.isNotBlank() }?.let {
                    CardDetailField(if (isIdCard) "Name on ID" else "Name on card", it) {
                        copyToClipboard(context, it, autoClearClipboard)
                        copyStatus = if (autoClearClipboard) "Copied · clears in 30 seconds" else "Copied to clipboard"
                    }
                }
                item.metadata["issuer"]?.takeIf { it.isNotBlank() }?.let {
                    CardDetailField(if (isIdCard) "Issuing authority" else "Bank or issuer", it) {
                        copyToClipboard(context, it, autoClearClipboard)
                        copyStatus = if (autoClearClipboard) "Copied · clears in 30 seconds" else "Copied to clipboard"
                    }
                }
                item.metadata["expiry"]?.takeIf { it.isNotBlank() }?.let {
                    CardDetailField("Expiry date", it) {
                        copyToClipboard(context, it, autoClearClipboard)
                        copyStatus = if (autoClearClipboard) "Copied · clears in 30 seconds" else "Copied to clipboard"
                    }
                }
                item.metadata["notes"]?.takeIf { it.isNotBlank() }?.let {
                    CardDetailField("Notes", it) {
                        copyToClipboard(context, it, autoClearClipboard)
                        copyStatus = if (autoClearClipboard) "Copied · clears in 30 seconds" else "Copied to clipboard"
                    }
                }
                item.metadata["securityCode"]?.takeIf { it.isNotBlank() }?.let {
                    CardDetailField(
                        "Security code",
                        if (revealLastFour) it else "•••",
                        actionIcon = if (revealLastFour) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        actionDescription = if (revealLastFour) "Hide security code" else "Reveal security code",
                        onAction = { revealLastFour = !revealLastFour }
                    )
                }
                CardDetailField("Saved", item.time, last = true)
            }
        }
        if (item.attachments.isNotEmpty()) {
            VaultAttachmentsSection(item.attachments, Modifier.fillMaxWidth().padding(top = 14.dp))
        }
        if (copyStatus.isNotBlank()) {
            Text(copyStatus, color = LockrColors.Accent, style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        }
        Text(
            "Sensitive card data is stored locally in your encrypted vault.",
            color = LockrColors.TextSecondary,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 13.dp, bottom = 20.dp)
        )
    }
    if (showOptions) {
        AlertDialog(
            onDismissRequest = { showOptions = false },
            title = { Text("Card options") },
            text = { Text("Copy the saved card name to your device clipboard?", color = LockrColors.TextSecondary) },
            dismissButton = { TextButton(onClick = { showOptions = false }) { Text("Cancel") } },
            confirmButton = {
                TextButton(onClick = {
                    copyToClipboard(context, item.title, autoClearClipboard)
                    showOptions = false
                }) { Text("Copy name") }
            }
        )
    }
}

@Composable
private fun CardDetailField(
    label: String,
    value: String,
    last: Boolean = false,
    actionIcon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Outlined.ContentCopy,
    actionDescription: String = "Copy $label",
    onAction: (() -> Unit)? = null
) {
    Row(Modifier.fillMaxWidth().height(68.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, color = LockrColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
            Text(value, color = LockrColors.TextPrimary, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (onAction != null) {
            Box(
                Modifier.size(38.dp).clip(CircleShape).background(Color(0x66E9EEF7)).clickable(onClick = onAction),
                contentAlignment = Alignment.Center
            ) {
                Icon(actionIcon, contentDescription = actionDescription, tint = LockrColors.TextPrimary, modifier = Modifier.size(18.dp))
            }
        }
    }
    if (!last) androidx.compose.material3.HorizontalDivider(color = LockrColors.Line)
}

@Composable
private fun RoundIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(42.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.78f))
            .border(1.dp, Color.White.copy(alpha = 0.8f), CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = label, tint = LockrColors.TextPrimary, modifier = Modifier.size(21.dp))
    }
}

@Composable
private fun EmptyCardsState(filtered: Boolean, onAdd: () -> Unit, modifier: Modifier = Modifier) {
    GlassPanel(modifier, radius = 25.dp) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 25.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(Modifier.size(100.dp, 72.dp), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.size(77.dp, 51.dp).offset(x = (-7).dp, y = 5.dp).rotate(-12f)
                        .clip(RoundedCornerShape(15.dp)).background(Brush.linearGradient(listOf(Color(0xFFE8DDF8), Color(0xFFC7E5F2))))
                        .border(1.dp, Color.White, RoundedCornerShape(15.dp))
                )
                Box(
                    Modifier.size(77.dp, 51.dp).offset(x = 7.dp, y = (-3).dp).rotate(8f)
                        .clip(RoundedCornerShape(15.dp)).background(Brush.linearGradient(listOf(Color(0xFFF5E3DE), Color(0xFFCDEBE5))))
                        .border(1.dp, Color.White, RoundedCornerShape(15.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.CreditCard, contentDescription = null, tint = Color(0xFF6884A8), modifier = Modifier.size(30.dp))
                }
                IridescentOrb(Modifier.align(Alignment.TopEnd).offset(x = (-2).dp, y = 2.dp), 23.dp)
            }
            Text(
                if (filtered) "No cards in this filter" else "Your wallet is waiting",
                style = MaterialTheme.typography.titleLarge,
                color = LockrColors.TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp)
            )
            Text(
                if (filtered) "Choose another type or add a card."
                else "Add a card to see its private details here.",
                style = MaterialTheme.typography.bodySmall,
                color = LockrColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp)
            )
            Row(
                Modifier.padding(top = 16.dp).clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(Color(0xFF344B58), Color(0xFF1E303D))))
                    .clickable(onClick = onAdd).padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Text("Add a card", color = Color.White, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 5.dp))
            }
        }
    }
}

@Composable
private fun VaultCard(item: VaultItem, colors: List<Color>, onClick: () -> Unit, height: androidx.compose.ui.unit.Dp = 194.dp) {
    val shape = RoundedCornerShape(25.dp)
    Box(
        Modifier.fillMaxWidth().height(height)
            .shadow(14.dp, shape, ambientColor = Color(0x333A4A64))
            .clip(shape)
            .background(Brush.linearGradient(colors))
            .border(1.dp, Color.White.copy(alpha = 0.9f), shape)
            .clickable(onClick = onClick)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val ribbon = Path().apply {
                moveTo(size.width * 0.55f, -size.height * 0.08f)
                cubicTo(size.width * 0.92f, size.height * 0.08f, size.width * 0.36f, size.height * 0.55f, size.width * 0.8f, size.height * 0.67f)
                cubicTo(size.width * 1.02f, size.height * 0.75f, size.width * 0.68f, size.height * 1.05f, size.width * 1.08f, size.height * 1.06f)
                lineTo(size.width * 1.1f, -size.height * 0.1f)
                close()
            }
            drawPath(ribbon, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.68f), Color(0x77D8DDFB), Color(0x22FFFFFF))))
            drawCircle(Color.White.copy(alpha = 0.24f), radius = size.width * 0.34f, center = Offset(size.width * 0.96f, size.height * 0.9f))
        }

        Column(Modifier.fillMaxSize().padding(horizontal = 19.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(29.dp).clip(RoundedCornerShape(9.dp)).background(Color.White.copy(alpha = 0.58f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.CreditCard, contentDescription = null, tint = Color(0xFF4D6889), modifier = Modifier.size(18.dp))
                }
                Text(
                    item.title,
                    color = LockrColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f).padding(start = 9.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    item.detail.takeIf { it.isNotBlank() && it != VaultCategory.CARD.label } ?: "Card",
                    color = LockrColors.TextPrimary,
                    modifier = Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.34f))
                        .border(1.dp, Color.White.copy(alpha = 0.66f), CircleShape).padding(horizontal = 10.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelMedium
                )
            }

            Row(Modifier.fillMaxWidth().weight(1f), verticalAlignment = Alignment.CenterVertically) {
                CardChip()
                Spacer(Modifier.weight(1f))
                Text(
                    if (item.lastFour.isBlank()) "PRIVATE CARD" else "••••  ${item.lastFour}",
                    color = Color(0xCC21374C),
                    style = MaterialTheme.typography.titleMedium,
                    letterSpacing = 1.4.sp
                )
            }

            Row(verticalAlignment = Alignment.Bottom) {
                Text("LOCAL ENCRYPTION", color = Color(0xCC26364E), style = MaterialTheme.typography.labelSmall, letterSpacing = 0.6.sp)
                Spacer(Modifier.weight(1f))
                Text("STASH", color = Color(0xAA21374C), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, letterSpacing = 1.1.sp)
            }
        }
    }
}

@Composable
private fun CardChip() {
    Box(Modifier.size(38.dp, 29.dp).clip(RoundedCornerShape(8.dp)).background(Brush.linearGradient(listOf(Color(0xFFFFE7AD), Color(0xFFD9AF6B)))).border(1.dp, Color.White.copy(alpha = 0.75f), RoundedCornerShape(8.dp))) {
        Canvas(Modifier.fillMaxSize().padding(4.dp)) {
            val stroke = 1.dp.toPx()
            drawLine(Color(0x889C743C), Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), stroke)
            drawLine(Color(0x889C743C), Offset(0f, size.height / 2), Offset(size.width, size.height / 2), stroke)
            drawRoundRect(Color.Transparent, topLeft = Offset(size.width * .22f, size.height * .2f), size = androidx.compose.ui.geometry.Size(size.width * .56f, size.height * .6f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
        }
    }
}

private fun copyToClipboard(context: Context, value: String, autoClear: Boolean) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Stash", value))
    if (autoClear) {
        Handler(Looper.getMainLooper()).postDelayed({
            val currentText = clipboard.primaryClip?.takeIf { it.itemCount > 0 }
                ?.getItemAt(0)?.coerceToText(context)?.toString()
            if (currentText == value) clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
        }, 30_000L)
    }
}

private fun revealTimeoutMillis(timeout: String): Long = when {
    timeout.startsWith("10") -> 10_000L
    timeout.startsWith("1 minute") -> 60_000L
    timeout.startsWith("5 minutes") -> 300_000L
    else -> 30_000L
}

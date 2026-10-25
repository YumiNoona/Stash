package com.lockr.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Contactless
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val cardPalettes = listOf(
    listOf(Color(0xFFE2EAFE), Color(0xFFC6EDE7), Color(0xFFF6E5F7)),
    listOf(Color(0xFFFFE8D8), Color(0xFFF4D2E7), Color(0xFFE6E9FF)),
    listOf(Color(0xFFDDF4EB), Color(0xFFCFE7FA), Color(0xFFEDE2FF)),
    listOf(Color(0xFFE7EAF1), Color(0xFFCAD4E5), Color(0xFFF3E5E2))
)

@Composable
fun CardsScreen(items: List<VaultItem>, onBack: () -> Unit, onSearch: () -> Unit, onOpenItem: (VaultItem) -> Unit, modifier: Modifier = Modifier) {
    var filter by rememberSaveable { mutableStateOf("All") }
    val cards = items.filter { it.category == VaultCategory.CARD }
    val visibleCards = cards.filter { item ->
        filter == "All" || when (filter) {
            "ID Cards" -> (item.metadata["cardType"] ?: item.detail).contains("ID Card", ignoreCase = true)
            else -> (item.metadata["cardType"] ?: item.detail).equals(filter, ignoreCase = true)
        }
    }
    Column(modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        StashTopBar("Cards", "${cards.size} saved", onBack = onBack,
            trailing = { IconButton(onClick = onSearch) { Icon(Icons.Outlined.Search, contentDescription = "Search vault") } })
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("All", "Credit", "Debit", "ID Cards").forEach { option ->
                FilterChip(selected = filter == option, onClick = { filter = option }, label = { Text(option) })
            }
        }
        if (visibleCards.isEmpty()) {
            GlassPanel(Modifier.fillMaxWidth().height(230.dp).padding(top = 12.dp), radius = 24.dp) {
                Column(Modifier.fillMaxSize().padding(26.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Box(Modifier.size(88.dp), contentAlignment = Alignment.Center) {
                        IridescentOrb(orbSize = 84.dp)
                        Icon(Icons.Outlined.CreditCard, contentDescription = null, tint = Color(0xFF54799A), modifier = Modifier.size(35.dp))
                    }
                    Text(if (cards.isEmpty()) "Your cards will live here" else "No cards in this filter", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
                    Text(if (cards.isEmpty()) "Import a card image or document using Add below." else "Choose a different card type.", color = LockrColors.TextSecondary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 5.dp))
                }
            }
        } else {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                Spacer(Modifier.height(2.dp))
                visibleCards.forEach { card ->
                    CardSurface(card, cardPalettes[(card.id.hashCode().and(Int.MAX_VALUE)) % cardPalettes.size]) { onOpenItem(card) }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun CardDetailPreview(item: VaultItem, modifier: Modifier = Modifier, height: androidx.compose.ui.unit.Dp = 216.dp, onClick: () -> Unit = {}) {
    CardSurface(item, cardPalettes[item.id.hashCode().and(Int.MAX_VALUE) % cardPalettes.size], Modifier.fillMaxWidth().then(modifier), height = height, onClick = onClick)
}

@Composable
private fun CardSurface(item: VaultItem, colors: List<Color>, modifier: Modifier = Modifier, height: androidx.compose.ui.unit.Dp = 194.dp, onClick: () -> Unit) {
    val shape = RoundedCornerShape(27.dp)
    val lastFour = item.lastFour.ifBlank { item.metadata["cardNumber"]?.takeLast(4) ?: item.metadata["lastFour"].orEmpty() }
    val issuer = item.metadata["issuer"].orEmpty().ifBlank { item.title }
    Box(
        modifier.fillMaxWidth().height(height)
            .graphicsLayer { rotationY = -2.5f; rotationX = 1.8f; cameraDistance = 30f * density }
            .shadow(22.dp, shape, ambientColor = Color(0x8878AEE2), spotColor = Color(0x887E86BF))
            .clip(shape).background(Brush.linearGradient(colors))
            .border(1.5.dp, Color.White.copy(alpha = 0.96f), shape)
            .clickable(onClick = onClick)
    ) {
        Image(painterResource(R.drawable.card_aurora_texture), contentDescription = null,
            contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.18f), Color.Transparent, Color.White.copy(alpha = 0.22f)))))
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 21.dp, top = 20.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text(issuer.uppercase(), style = MaterialTheme.typography.titleMedium, color = Color(0xFF071854),
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Text(item.metadata["cardType"].orEmpty().ifBlank { "Card" }, style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF10204E), modifier = Modifier.padding(start = 6.dp))
        }
        Row(Modifier.align(Alignment.CenterStart).padding(start = 22.dp, top = 3.dp),
            verticalAlignment = Alignment.CenterVertically) {
            CardChip()
            Spacer(Modifier.weight(1f))
            Icon(Icons.Outlined.Contactless, contentDescription = null, tint = Color.White.copy(alpha = 0.92f),
                modifier = Modifier.size(30.dp).padding(end = 2.dp))
            Spacer(Modifier.size(19.dp))
        }
        Text("••••  ••••  ${lastFour.ifBlank { "••••" }}", color = Color(0xFF09194E),
            style = MaterialTheme.typography.titleLarge.copy(letterSpacing = 1.1.sp),
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 21.dp, bottom = 54.dp))
        item.metadata["cardholder"]?.takeIf { it.isNotBlank() }?.let { holder ->
            Text(holder.uppercase(), color = Color(0xFF10204E), style = MaterialTheme.typography.labelMedium,
                maxLines = 1, modifier = Modifier.align(Alignment.BottomStart).padding(start = 22.dp, bottom = 21.dp))
        }
        Text(item.metadata["network"].orEmpty().ifBlank { "STASH" }.uppercase(),
            color = Color(0xFF071854), style = MaterialTheme.typography.titleLarge,
            maxLines = 1, modifier = Modifier.align(Alignment.BottomEnd).padding(end = 19.dp, bottom = 17.dp))
    }
}

@Composable
private fun CardChip(modifier: Modifier = Modifier) {
    Box(modifier.size(38.dp, 28.dp).clip(RoundedCornerShape(8.dp)).background(Brush.linearGradient(listOf(Color(0xFFFFE8B4), Color(0xFFE8C879), Color(0xFFFFEDC2)))).border(1.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(8.dp))) {
        Canvas(Modifier.fillMaxSize()) {
            val line = Color(0x887B643A)
            drawLine(line, Offset(size.width * 0.5f, 0f), Offset(size.width * 0.5f, size.height), 1.dp.toPx())
            drawLine(line, Offset(0f, size.height * 0.5f), Offset(size.width, size.height * 0.5f), 1.dp.toPx())
            drawLine(line, Offset(size.width * 0.25f, 0f), Offset(size.width * 0.25f, size.height), 1.dp.toPx())
            drawLine(line, Offset(size.width * 0.75f, 0f), Offset(size.width * 0.75f, size.height), 1.dp.toPx())
        }
    }
}

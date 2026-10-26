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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Contactless
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val LocalHideCardNumber = compositionLocalOf { true }

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
                    Text(if (cards.isEmpty()) "Tap + to scan or import a card." else "Choose a different card type.",
                        color = LockrColors.TextSecondary, style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 5.dp))
                }
            }
        } else {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(top = 4.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)) {
                items(visibleCards, key = { it.id }) { card ->
                    CardSurface(card, onClick = { onOpenItem(card) })
                }
            }
        }
    }
}

@Composable
fun CardDetailPreview(
    item: VaultItem,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 216.dp,
    showFullNumber: Boolean? = null,
    onToggleNumber: (() -> Unit)? = null,
    animate: Boolean = true,
    onClick: () -> Unit = {}
) {
    CardSurface(item, modifier, height, showFullNumber, onToggleNumber, animate, onClick)
}

@Composable
private fun CardSurface(
    item: VaultItem,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 194.dp,
    showFullNumber: Boolean? = null,
    onToggleNumber: (() -> Unit)? = null,
    animate: Boolean = true,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(27.dp)
    val art = cardArtStyle(item)
    val ink = if (art.dark) Color.White else Color(0xFF09194E)
    val secondaryInk = if (art.dark) Color(0xFFDCEAFF) else Color(0xFF243B69)
    val number = item.metadata["cardNumber"].orEmpty()
    val lastFour = item.lastFour.ifBlank { item.metadata["cardNumber"]?.takeLast(4) ?: item.metadata["lastFour"].orEmpty() }
    val numberVisible = showFullNumber ?: !LocalHideCardNumber.current
    val numberText = if (numberVisible && number.isNotBlank())
        if (isPaymentCardType(item.metadata["cardType"].orEmpty())) formatPaymentCardNumber(number, item.metadata["network"].orEmpty()) else number
    else "••••  ••••  ••••  ${lastFour.ifBlank { "••••" }}"
    val issuer = item.metadata["issuer"].orEmpty().ifBlank { item.title }
    val motion = animate && art.animated
    val transition = if (motion) rememberInfiniteTransition(label = "card sheen") else null
    val phase by transition?.animateFloat(-1f, 1f,
        animationSpec = infiniteRepeatable(tween(5000), RepeatMode.Reverse), label = "sheen position")
        ?: remember { mutableStateOf(0f) }
    Box(
        modifier.fillMaxWidth().height(height)
            .graphicsLayer {
                rotationY = -2.5f + if (motion) phase * 1.3f else 0f
                rotationX = 1.8f + if (motion) phase * 0.8f else 0f
                cameraDistance = 30f * density
            }
            .shadow(22.dp, shape, ambientColor = Color(0x8878AEE2), spotColor = Color(0x887E86BF))
            .clip(shape).background(if (art.dark) Color(0xFF101B59) else Color(0xFFE2EAFE))
            .border(1.5.dp, Color.White.copy(alpha = 0.96f), shape)
            .clickable(onClick = onClick)
    ) {
        Image(painterResource(art.image), contentDescription = null,
            contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
            if (art.dark) listOf(Color(0x44102055), Color.Transparent, Color(0x66102055))
            else listOf(Color.White.copy(alpha = 0.16f), Color.Transparent, Color.White.copy(alpha = 0.23f)))))
        if (motion) {
            Box(Modifier.fillMaxHeight().width(100.dp).graphicsLayer {
                translationX = (phase * 260f + 250f) * density
                rotationZ = -17f
            }.background(Brush.horizontalGradient(listOf(Color.Transparent,
                Color.White.copy(alpha = 0.14f), Color.Transparent))))
        }
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.SpaceBetween) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(issuer.uppercase(), style = MaterialTheme.typography.titleMedium, color = ink,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text(item.metadata["cardType"].orEmpty().ifBlank { "Card" },
                    style = MaterialTheme.typography.labelMedium, color = secondaryInk,
                    modifier = Modifier.padding(start = 8.dp))
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                CardChip()
                Spacer(Modifier.weight(1f))
                Icon(Icons.Outlined.Contactless, contentDescription = null,
                    tint = if (art.dark) Color.White else Color(0xFF436CA2), modifier = Modifier.size(25.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(numberText, color = ink,
                        style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 0.7.sp,
                            fontSize = if (height >= 230.dp) 19.sp else 16.sp),
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f))
                    if (onToggleNumber != null) IconButton(onClick = onToggleNumber, modifier = Modifier.size(32.dp)) {
                        Icon(if (numberVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (numberVisible) "Hide card number" else "Show card number",
                            tint = ink, modifier = Modifier.size(21.dp))
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        Text("CARD HOLDER", style = MaterialTheme.typography.labelSmall, color = secondaryInk)
                        Text(item.metadata["cardholder"].orEmpty().ifBlank { "—" }.uppercase(),
                            color = ink, style = MaterialTheme.typography.labelMedium,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Column(Modifier.padding(horizontal = 9.dp)) {
                        Text("EXPIRES", style = MaterialTheme.typography.labelSmall, color = secondaryInk)
                        Text(item.metadata["expiry"].orEmpty().ifBlank { "MM/YY" },
                            color = ink, style = MaterialTheme.typography.labelMedium)
                    }
                    Text(item.metadata["network"].orEmpty().ifBlank { "STASH" }.uppercase(),
                        color = ink, style = MaterialTheme.typography.labelMedium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(73.dp), textAlign = TextAlign.End)
                }
            }
        }
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

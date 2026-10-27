package com.lockr.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
    showFullNumber: Boolean? = null,
    onToggleNumber: (() -> Unit)? = null,
    animate: Boolean = true,
    showCvc: Boolean = false,
    onClick: () -> Unit = {}
) {
    CardSurface(item, modifier, showFullNumber, onToggleNumber, animate, showCvc, onClick)
}

@Composable
private fun CardSurface(
    item: VaultItem,
    modifier: Modifier = Modifier,
    showFullNumber: Boolean? = null,
    onToggleNumber: (() -> Unit)? = null,
    animate: Boolean = true,
    showCvc: Boolean = false,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    val art = cardArtStyle(item)
    val custom = customCardArt(item)
    val dark = if (custom != null) item.metadata["cardArtLightText"] != "false" else art.dark
    val ink = if (dark) Color.White else Color(0xFF102039)
    val secondaryInk = ink.copy(alpha = 0.75f)
    val number = item.metadata["cardNumber"].orEmpty()
    val lastFour = item.lastFour.ifBlank { number.takeLast(4).ifBlank { item.metadata["lastFour"].orEmpty() } }
    val numberVisible = showFullNumber ?: !LocalHideCardNumber.current
    val numberText = if (numberVisible && number.isNotBlank())
        if (isPaymentCardType(item.metadata["cardType"].orEmpty())) formatPaymentCardNumber(number, item.metadata["network"].orEmpty()) else number
    else "••••  ••••  ••••  ${lastFour.ifBlank { "••••" }}"
    val motion = if (animate) cardMotion(item) else CardMotion.NONE
    val transition = if (motion != CardMotion.NONE) rememberInfiniteTransition(label = "card motion") else null
    val phase by transition?.animateFloat(-1f, 1f,
        animationSpec = infiniteRepeatable(tween(4000), RepeatMode.Reverse), label = "card phase")
        ?: remember { mutableStateOf(0f) }
    // Every surface uses the physical card ratio. A single logical canvas scales uniformly,
    // so list, review and detail cannot stretch the card or rearrange its fields.
    BoxWithConstraints(modifier.fillMaxWidth().aspectRatio(1.586f), contentAlignment = Alignment.Center) {
        val scale = maxWidth.value / 340f
        Box(Modifier.requiredSize(340.dp, (340f / 1.586f).dp).graphicsLayer {
            scaleX = scale; scaleY = scale
            if (motion == CardMotion.FLOAT) {
                rotationZ = phase * 0.8f
                translationY = phase * 2f * density
            }
        }.clip(shape).background(if (dark) Color(0xFF111B30) else Color(0xFFDBE9F5))
            .border(1.dp, ink.copy(alpha = 0.15f), shape).clickable(onClick = onClick)) {
            CardArtBackground(art, Modifier.fillMaxSize())
            if (custom != null) CustomCardArtBackground(custom, Modifier.fillMaxSize(), animate)
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
                if (dark) listOf(Color(0x99071222), Color(0x55071222), Color(0xAA071222))
                else listOf(Color(0xCCFFFFFF), Color(0x88FFFFFF), Color(0xCCFFFFFF)))))
            if (motion == CardMotion.SHEEN) {
                Box(Modifier.fillMaxHeight().width(90.dp).graphicsLayer {
                    translationX = (phase * 270f + 160f) * density
                    rotationZ = -20f
                }.background(Brush.horizontalGradient(listOf(Color.Transparent,
                    Color.White.copy(alpha = 0.3f), Color.Transparent))))
            }
            if (motion == CardMotion.PULSE) Box(Modifier.fillMaxSize().background(
                Color.White.copy(alpha = 0.04f + (phase + 1f) * 0.045f)))
            Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(item.metadata["partner"].orEmpty().ifBlank { item.title }, color = ink,
                        style = MaterialTheme.typography.labelLarge, maxLines = 1,
                        overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text(item.metadata["issuer"].orEmpty(), color = ink,
                        style = MaterialTheme.typography.labelMedium, maxLines = 1,
                        overflow = TextOverflow.Ellipsis, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CardChip(Modifier)
                    Icon(Icons.Outlined.Contactless, "Contactless", tint = secondaryInk, modifier = Modifier.size(24.dp))
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(numberText, color = ink, style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = if (numberText.length > 23) 13.sp else 16.sp, letterSpacing = 0.3.sp),
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    if (onToggleNumber != null) IconButton(onClick = onToggleNumber, modifier = Modifier.size(28.dp)) {
                        Icon(if (numberVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            if (numberVisible) "Hide card number" else "Show card number", tint = ink, modifier = Modifier.size(17.dp))
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("EXP  ${item.metadata["expiry"].orEmpty().ifBlank { "MM/YY" }}",
                        color = secondaryInk, style = MaterialTheme.typography.labelMedium)
                    val cvc = item.metadata["cardCvc"].orEmpty()
                    Text("CVV  ${if (cvc.isBlank()) "—" else if (showCvc) cvc else "•••"}",
                        color = secondaryInk, style = MaterialTheme.typography.labelMedium)
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(item.metadata["cardholder"].orEmpty().ifBlank { "CARDHOLDER" }.uppercase(),
                        color = ink, style = MaterialTheme.typography.labelMedium, maxLines = 1,
                        overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text(item.metadata["network"].orEmpty().ifBlank { "STASH" }.uppercase(),
                        color = ink, style = MaterialTheme.typography.labelLarge, maxLines = 1)
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

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
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val LocalHideCardNumber = compositionLocalOf { true }

@Composable
fun CardsScreen(items: List<VaultItem>, onBack: () -> Unit, onSearch: () -> Unit, onOpenItem: (VaultItem) -> Unit, modifier: Modifier = Modifier) {
    var filter by rememberSaveable { mutableStateOf("All") }
    val catalog = LocalTagCatalog.current
    val filters = listOf("All", "Credit", "Debit", "ID Cards") + catalog.cards
    androidx.compose.runtime.LaunchedEffect(filters) { if (filter !in filters) filter = "All" }
    val cards = items.filter { it.category == VaultCategory.CARD }
    val visibleCards = cards.filter { item ->
        filter == "All" || when (filter) {
            "ID Cards" -> (item.metadata["cardType"] ?: item.detail).contains("ID Card", ignoreCase = true)
            else -> (item.metadata["cardType"] ?: item.detail).equals(filter, ignoreCase = true) || cardTags(item).any { it.equals(filter, true) }
        }
    }
    Box(modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 88.dp, bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { CollectionFilters(filters, filter, { filter = it }, VaultCategory.CARD) }
            if (visibleCards.isEmpty()) item {
                CollectionEmptyState(if (cards.isEmpty()) "Your cards will live here" else "No cards in this filter",
                    if (cards.isEmpty()) "Tap + to add your first card." else "Choose another tag.", Icons.Outlined.CreditCard)
            }
            items(visibleCards, key = { it.id }) { card -> CardSurface(card, onClick = { onOpenItem(card) }) }
        }
        StashTopBar("Cards", "${cards.size} saved", onBack = onBack,
            trailing = { IconButton(onClick = onSearch) { Icon(Icons.Outlined.Search, "Search vault") } })
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
    val secondaryInk = ink.copy(alpha = 0.85f)
    val letteringShadow = if (art.illustrated && custom == null) Shadow(
        if (dark) Color.Black.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.8f), Offset(0f, 1f), 5f) else Shadow.None
    val number = item.metadata["cardNumber"].orEmpty()
    val lastFour = item.lastFour.ifBlank { number.takeLast(4).ifBlank { item.metadata["lastFour"].orEmpty() } }
    val numberVisible = showFullNumber ?: !LocalHideCardNumber.current
    val numberText = if (numberVisible && number.isNotBlank())
        if (isPaymentCardType(item.metadata["cardType"].orEmpty())) formatPaymentCardNumber(number, item.metadata["network"].orEmpty()) else number
    else "••••  ••••  ••••  ${lastFour.ifBlank { "••••" }}"
    val motion = if (animate && LocalMotionEnabled.current) cardMotion(item) else CardMotion.NONE
    val transition = if (motion != CardMotion.NONE) rememberInfiniteTransition(label = "card motion") else null
    val phase by transition?.animateFloat(-1f, 1f,
        animationSpec = infiniteRepeatable(tween(6400, easing = androidx.compose.animation.core.LinearEasing), RepeatMode.Restart), label = "card phase")
        ?: remember { mutableStateOf(0f) }
    // Every surface uses the physical card ratio. A single logical canvas scales uniformly,
    // so list, review and detail cannot stretch the card or rearrange its fields.
    BoxWithConstraints(modifier.fillMaxWidth().aspectRatio(1.586f), contentAlignment = Alignment.Center) {
        val scale = maxWidth.value / 340f
        Box(Modifier.requiredSize(340.dp, (340f / 1.586f).dp).graphicsLayer {
            scaleX = scale; scaleY = scale


        }.clip(shape).background(if (dark) Color(0xFF111B30) else Color(0xFFDBE9F5))
            .border(1.dp, ink.copy(alpha = 0.15f), shape).clickable(onClick = onClick)) {
            Box(Modifier.fillMaxSize().graphicsLayer {
                val wave = kotlin.math.sin((phase + 1f) * kotlin.math.PI).toFloat()
                if (motion == CardMotion.FLOAT) { scaleX = 1.035f; scaleY = 1.035f; translationY = wave * 2f * density }
                if (motion == CardMotion.DRIFT) { scaleX = 1.055f; scaleY = 1.055f; translationX = wave * 5f * density; translationY = kotlin.math.cos((phase + 1f) * kotlin.math.PI).toFloat() * 2f * density }
                if (motion == CardMotion.ZOOM) { scaleX = 1.035f + kotlin.math.sin((phase + 1f) * kotlin.math.PI).toFloat() * 0.015f; scaleY = scaleX }
            }) {
                CardArtBackground(art, Modifier.fillMaxSize())
                if (custom != null) CustomCardArtBackground(custom, Modifier.fillMaxSize(), animate && LocalArtworkAutoplay.current)
            }
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
                if (art.illustrated && custom == null) {
                    if (dark) listOf(Color(0x33000000), Color.Transparent, Color(0x44000000))
                    else listOf(Color(0x33FFFFFF), Color.Transparent, Color(0x33FFFFFF))
                } else if (dark) listOf(Color(0x99071222), Color(0x55071222), Color(0xAA071222))
                else listOf(Color(0xCCFFFFFF), Color(0x88FFFFFF), Color(0xCCFFFFFF)))))
            if (motion == CardMotion.PULSE) Box(Modifier.fillMaxSize().background(
                Color(0xFFB7D6FF).copy(alpha = 0.035f + (kotlin.math.sin((phase + 1f) * kotlin.math.PI).toFloat() + 1f) * 0.025f)))
            CardMotionOverlay(motion, phase, Modifier.fillMaxSize())
            Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(item.metadata["partner"].orEmpty().ifBlank { item.metadata["cardType"].orEmpty().ifBlank { "Card" } }, color = ink,
                        style = MaterialTheme.typography.labelLarge.copy(shadow = letteringShadow), maxLines = 1,
                        overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text(item.metadata["issuer"].orEmpty(), color = ink,
                        style = MaterialTheme.typography.labelMedium.copy(shadow = letteringShadow), maxLines = 1,
                        overflow = TextOverflow.Ellipsis, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CardChip(Modifier)
                    Icon(Icons.Outlined.Contactless, "Contactless", tint = secondaryInk, modifier = Modifier.size(24.dp))
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(numberText, color = ink, style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = if (numberText.length > 23) 13.sp else 16.sp, letterSpacing = 0.3.sp, shadow = letteringShadow),
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
                        color = ink, style = MaterialTheme.typography.labelMedium.copy(shadow = letteringShadow), maxLines = 1,
                        overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text(item.metadata["network"].orEmpty().ifBlank { "STASH" }.uppercase(),
                        color = ink, style = MaterialTheme.typography.labelLarge.copy(shadow = letteringShadow), maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun CardChip(modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(6.dp)
    Box(modifier.size(40.dp, 30.dp).clip(shape)
        .background(Brush.linearGradient(listOf(Color(0xFFEFF3EF), Color(0xFFA3B0AD), Color(0xFFE9EFE9), Color(0xFFBCC7C1))))
        .border(0.7.dp, Color(0x99718079), shape)) {
        Canvas(Modifier.fillMaxSize()) {
            val line = Color(0x99616F69)
            val w = size.width; val h = size.height
            val stroke = 0.65.dp.toPx()
            drawRoundRect(line, Offset(w * 0.33f, h * 0.24f), androidx.compose.ui.geometry.Size(w * 0.34f, h * 0.52f),
                androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
            drawLine(line, Offset(w * 0.5f, 0f), Offset(w * 0.5f, h * 0.24f), stroke)
            drawLine(line, Offset(w * 0.5f, h * 0.76f), Offset(w * 0.5f, h), stroke)
            for (y in listOf(0.33f, 0.67f)) {
                drawLine(line, Offset(0f, h * y), Offset(w * 0.33f, h * y), stroke)
                drawLine(line, Offset(w * 0.67f, h * y), Offset(w, h * y), stroke)
            }
            for (x in listOf(0.22f, 0.78f)) {
                drawLine(line, Offset(w * x, 0f), Offset(w * x, h * 0.33f), stroke)
                drawLine(line, Offset(w * x, h * 0.67f), Offset(w * x, h), stroke)
            }
            drawLine(Color.White.copy(alpha = 0.65f), Offset(2.dp.toPx(), 2.dp.toPx()), Offset(w - 2.dp.toPx(), 2.dp.toPx()), stroke)
        }
    }
}

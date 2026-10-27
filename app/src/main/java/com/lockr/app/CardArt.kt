package com.lockr.app

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

/** Stored by enum name so saved card designs survive label and picker changes. */
enum class CardArtStyle(
    val label: String,
    @DrawableRes val image: Int,
    val dark: Boolean = false,
    val animated: Boolean = false
) {
    AURORA("Aurora", R.drawable.card_aurora_art),
    OPAL("Opal", R.drawable.card_opal_art),
    MIDNIGHT("Midnight", R.drawable.card_midnight_art, dark = true, animated = true),
    SOLAR("Solar", R.drawable.card_opal_art),
    LAGOON("Lagoon", R.drawable.card_aurora_art, animated = true),
    BLOOM("Bloom", R.drawable.card_opal_art),
    CHROME("Chrome", R.drawable.card_aurora_art),
    AMETHYST("Amethyst", R.drawable.card_midnight_art, dark = true),
    NEON("Neon", R.drawable.card_midnight_art, dark = true, animated = true),
    COMET("Comet", R.drawable.card_midnight_art, dark = true, animated = true),
    CITRUS("Citrus", R.drawable.card_opal_art),
    ICE("Ice", R.drawable.card_aurora_art),
    EMBER("Ember", R.drawable.card_midnight_art, dark = true),
    MOSAIC("Mosaic", R.drawable.card_opal_art),
    PRISM("Prism", R.drawable.card_aurora_art, animated = true)
}

fun cardArtStyle(item: VaultItem): CardArtStyle =
    CardArtStyle.entries.firstOrNull { it.name.equals(item.metadata["cardArt"], ignoreCase = true) }
        // Older cards used one of these three designs based on their ID. Keep that mapping stable.
        ?: listOf(CardArtStyle.AURORA, CardArtStyle.OPAL, CardArtStyle.MIDNIGHT)
            .let { it[(item.id.hashCode() and Int.MAX_VALUE) % 3] }

private fun cardPath(first: Offset, second: Offset, third: Offset, fourth: Offset? = null): Path = Path().apply {
    moveTo(first.x, first.y)
    lineTo(second.x, second.y)
    lineTo(third.x, third.y)
    if (fourth != null) lineTo(fourth.x, fourth.y)
    close()
}

/** Shared by the card, the picker, and Home thumbnails so every finish looks consistent. */
@Composable
fun CardArtBackground(art: CardArtStyle, modifier: Modifier = Modifier, phase: Float = 0f) {
    if (art == CardArtStyle.AURORA || art == CardArtStyle.OPAL || art == CardArtStyle.MIDNIGHT) {
        Image(painterResource(art.image), contentDescription = null,
            contentScale = ContentScale.Crop, modifier = modifier)
        return
    }
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val p = if (art.animated) phase else 0f
        val palette = when (art) {
            CardArtStyle.SOLAR -> listOf(Color(0xFFFFF8D9), Color(0xFFFFC98B), Color(0xFFF58BAB))
            CardArtStyle.LAGOON -> listOf(Color(0xFFE1FFFC), Color(0xFF83DDE0), Color(0xFF5CA4E0))
            CardArtStyle.BLOOM -> listOf(Color(0xFFFFF0F5), Color(0xFFF1B8DA), Color(0xFFC3AFF6))
            CardArtStyle.CHROME -> listOf(Color(0xFFF5F9FF), Color(0xFF9FB6D5), Color(0xFFE2EBF7))
            CardArtStyle.AMETHYST -> listOf(Color(0xFF31215F), Color(0xFF7142AA), Color(0xFFBB6ECD))
            CardArtStyle.NEON -> listOf(Color(0xFF07152F), Color(0xFF173365), Color(0xFF4E2478))
            CardArtStyle.COMET -> listOf(Color(0xFF10133D), Color(0xFF23457E), Color(0xFF382B72))
            CardArtStyle.CITRUS -> listOf(Color(0xFFFFF8B8), Color(0xFFD9F2A6), Color(0xFF8BD9C4))
            CardArtStyle.ICE -> listOf(Color(0xFFE8F9FF), Color(0xFFA9D2F3), Color(0xFFE6E4FF))
            CardArtStyle.EMBER -> listOf(Color(0xFF4A204B), Color(0xFFAF4267), Color(0xFFE88676))
            CardArtStyle.MOSAIC -> listOf(Color(0xFFEAEAFF), Color(0xFFBFC4EE), Color(0xFFF0C2D7))
            CardArtStyle.PRISM -> listOf(Color(0xFFE4F9FF), Color(0xFFA8C5F4), Color(0xFFEFC5E9))
            else -> listOf(Color.White, Color.White)
        }
        drawRect(Brush.linearGradient(palette, start = Offset.Zero, end = Offset(w, h)))
        when (art) {
            CardArtStyle.SOLAR -> {
                val center = Offset(w * 0.8f, h * 0.28f)
                for (i in 5 downTo 1) {
                    drawCircle(Color.White.copy(alpha = 0.07f + i * 0.025f), w * (0.11f + i * 0.075f), center)
                }
                drawCircle(Brush.radialGradient(listOf(Color(0xFFFFFCEB), Color(0x55FFEAA5), Color.Transparent),
                    center, w * 0.28f), w * 0.28f, center)
                for (i in 0 until 9) {
                    val y = h * (0.45f + i * 0.075f)
                    drawLine(Color.White.copy(alpha = 0.27f), Offset(0f, y), Offset(w, y - w * 0.19f), 1.5.dp.toPx())
                }
            }
            CardArtStyle.LAGOON -> {
                for (i in 0 until 5) {
                    val shift = i * h * 0.13f + p * h * 0.04f
                    val wave = Path().apply {
                        moveTo(-w * 0.1f, h * 0.48f + shift)
                        cubicTo(w * 0.22f, h * 0.17f + shift, w * 0.47f, h * 0.79f + shift,
                            w * 1.1f, h * 0.27f + shift)
                    }
                    drawPath(wave, if (i % 2 == 0) Color.White.copy(alpha = 0.75f)
                        else Color(0xFF59C3E4).copy(alpha = 0.65f), style = Stroke((3 + i).dp.toPx()))
                }
                drawCircle(Color.White.copy(alpha = 0.35f), w * 0.24f, Offset(w * 0.92f, h * 0.89f))
            }
            CardArtStyle.BLOOM -> {
                val cx = w * 0.81f
                val cy = h * 0.43f
                for (i in 0 until 8) {
                    val angle = i * Math.PI * 2.0 / 8.0
                    val x = cx + kotlin.math.cos(angle).toFloat() * w * 0.16f
                    val y = cy + kotlin.math.sin(angle).toFloat() * h * 0.27f
                    drawCircle(Color.White.copy(alpha = 0.33f), w * 0.12f, Offset(x, y))
                    drawCircle(Color(0xFFEE87CB).copy(alpha = 0.13f), w * 0.08f, Offset(x, y))
                }
                drawCircle(Brush.radialGradient(listOf(Color.White, Color(0xFFFAD7EB), Color.Transparent),
                    Offset(cx, cy), w * 0.17f), w * 0.17f, Offset(cx, cy))
                drawLine(Color.White.copy(alpha = 0.75f), Offset(0f, h * 0.84f), Offset(w, h * 0.21f), 2.dp.toPx())
            }
            CardArtStyle.CHROME -> {
                for (i in 0 until 5) {
                    val left = w * (0.28f + i * 0.14f)
                    val band = cardPath(Offset(left, 0f), Offset(left + w * 0.11f, 0f),
                        Offset(left - w * 0.19f, h), Offset(left - w * 0.3f, h))
                    drawPath(band, if (i % 2 == 0) Color.White.copy(alpha = 0.65f)
                        else Color(0xFF6686B9).copy(alpha = 0.2f))
                    drawLine(Color.White.copy(alpha = 0.7f), Offset(left, 0f),
                        Offset(left - w * 0.3f, h), 1.dp.toPx())
                }
            }
            CardArtStyle.AMETHYST -> {
                val facets = listOf(
                    cardPath(Offset(w * 0.46f, 0f), Offset(w, 0f), Offset(w * 0.74f, h * 0.52f)),
                    cardPath(Offset(w, 0f), Offset(w, h), Offset(w * 0.74f, h * 0.52f)),
                    cardPath(Offset(w * 0.74f, h * 0.52f), Offset(w, h), Offset(w * 0.33f, h)),
                    cardPath(Offset(w * 0.46f, 0f), Offset(w * 0.74f, h * 0.52f), Offset(w * 0.33f, h))
                )
                facets.forEachIndexed { i, path ->
                    drawPath(path, listOf(Color(0x4468D6F9), Color(0x55FFB6E8),
                        Color(0x339F8DF1), Color(0x66DF9BFC))[i])
                    drawPath(path, Color.White.copy(alpha = 0.32f), style = Stroke(1.dp.toPx()))
                }
            }
            CardArtStyle.NEON -> {
                for (i in 0 until 7) {
                    val y = h * (0.17f + i * 0.115f)
                    drawLine(if (i % 2 == 0) Color(0xFF4EE8F6) else Color(0xFFF66ADF),
                        Offset(w * 0.33f, y), Offset(w, y - h * 0.15f),
                        (if (i % 2 == 0) 2 else 1).dp.toPx(), alpha = 0.65f)
                }
                for (i in 0 until 5) {
                    val x = w * (0.38f + i * 0.14f)
                    drawCircle(Color(0xFF8AFAFF), 3.dp.toPx(), Offset(x, h * (0.62f - i * 0.09f)))
                }
                drawCircle(Brush.radialGradient(listOf(Color(0xFFB6FFFF), Color(0x8847DFF5), Color.Transparent),
                    Offset(w * (0.74f + p * 0.1f), h * 0.44f), w * 0.18f), w * 0.18f,
                    Offset(w * (0.74f + p * 0.1f), h * 0.44f))
            }
            CardArtStyle.COMET -> {
                for (i in 0 until 28) {
                    val x = w * ((i * 37 % 97) / 97f)
                    val y = h * ((i * 53 % 89) / 89f)
                    drawCircle(Color.White.copy(alpha = if (i % 3 == 0) 0.8f else 0.4f),
                        if (i % 5 == 0) 1.6.dp.toPx() else 0.8.dp.toPx(), Offset(x, y))
                }
                val head = Offset(w * (0.76f + p * 0.07f), h * (0.3f - p * 0.05f))
                drawLine(Brush.linearGradient(listOf(Color.Transparent, Color(0x99C1F8FF), Color.White),
                    start = Offset(w * 0.25f, h * 0.8f), end = head),
                    Offset(w * 0.25f, h * 0.8f), head, 14.dp.toPx())
                drawCircle(Brush.radialGradient(listOf(Color.White, Color(0xFF74D5F3), Color.Transparent),
                    head, w * 0.12f), w * 0.12f, head)
            }
            CardArtStyle.CITRUS -> {
                for (i in 0 until 5) {
                    val center = Offset(w * (0.66f + (i % 2) * 0.2f), h * (0.05f + i * 0.23f))
                    val radius = w * (0.18f + (i % 2) * 0.04f)
                    drawCircle(Color.White.copy(alpha = 0.3f), radius, center)
                    drawCircle(Color(0xFF52BE99).copy(alpha = 0.35f), radius,
                        center, style = Stroke(2.dp.toPx()))
                    drawCircle(Color.White.copy(alpha = 0.5f), radius * 0.62f,
                        center, style = Stroke(1.dp.toPx()))
                }
            }
            CardArtStyle.ICE -> {
                val shards = listOf(
                    cardPath(Offset(w * 0.68f, 0f), Offset(w, 0f), Offset(w * 0.83f, h * 0.62f)),
                    cardPath(Offset(w, h * 0.1f), Offset(w, h), Offset(w * 0.83f, h * 0.62f)),
                    cardPath(Offset(w * 0.34f, h), Offset(w * 0.83f, h * 0.62f), Offset(w, h)),
                    cardPath(Offset(w * 0.68f, 0f), Offset(w * 0.83f, h * 0.62f), Offset(w * 0.34f, h))
                )
                shards.forEachIndexed { i, shard ->
                    drawPath(shard, if (i % 2 == 0) Color.White.copy(alpha = 0.54f)
                        else Color(0xFF66A9DA).copy(alpha = 0.23f))
                    drawPath(shard, Color.White.copy(alpha = 0.65f), style = Stroke(1.dp.toPx()))
                }
            }
            CardArtStyle.EMBER -> {
                for (i in 0 until 5) {
                    val yy = h * (0.28f + i * 0.19f)
                    val flow = Path().apply {
                        moveTo(-w * 0.1f, yy + h * 0.3f)
                        cubicTo(w * 0.28f, yy - h * 0.45f, w * 0.5f, yy + h * 0.43f,
                            w * 1.1f, yy - h * 0.2f)
                    }
                    drawPath(flow, if (i % 2 == 0) Color(0xFFFFD399).copy(alpha = 0.75f)
                        else Color(0xFFFF8C9F).copy(alpha = 0.65f),
                        style = Stroke((5 - i).dp.toPx()))
                }
                drawCircle(Brush.radialGradient(listOf(Color(0x88FFF2C4), Color.Transparent),
                    Offset(w, h), w * 0.65f), w * 0.65f, Offset(w, h))
            }
            CardArtStyle.MOSAIC -> {
                for (row in 0 until 4) for (col in 0 until 6) {
                    val x = w * (0.43f + col * 0.14f)
                    val y = h * (row * 0.3f - 0.08f)
                    val diamond = cardPath(Offset(x, y - h * 0.13f), Offset(x + w * 0.07f, y),
                        Offset(x, y + h * 0.13f), Offset(x - w * 0.07f, y))
                    drawPath(diamond, if ((row + col) % 3 == 0) Color.White.copy(alpha = 0.72f)
                        else Color(0xFF9B91C9).copy(alpha = 0.18f))
                    drawPath(diamond, Color.White.copy(alpha = 0.44f), style = Stroke(0.8.dp.toPx()))
                }
            }
            CardArtStyle.PRISM -> {
                val center = Offset(w * (0.77f + p * 0.025f), h * 0.42f)
                val rim = listOf(Offset(w * 0.37f, 0f), Offset(w * 0.77f, 0f),
                    Offset(w, h * 0.16f), Offset(w, h * 0.82f), Offset(w * 0.73f, h),
                    Offset(w * 0.28f, h))
                val colors = listOf(Color(0x7799F7ED), Color(0x88B7ADFF), Color(0x77FFAFDA),
                    Color(0x88FFE3B6), Color(0x889FD9FF), Color(0x77C7A7F2))
                rim.forEachIndexed { i, point ->
                    val next = rim[(i + 1) % rim.size]
                    val shard = cardPath(center, point, next)
                    drawPath(shard, colors[i])
                    drawLine(Color.White.copy(alpha = 0.72f), center, point, 1.2.dp.toPx())
                }
                drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.7f), Color.Transparent),
                    center, w * 0.17f), w * 0.17f, center)
            }
            else -> Unit
        }
    }
}

@Composable
fun CardArtPicker(selected: CardArtStyle, onSelect: (CardArtStyle) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Card art  ·  ${CardArtStyle.entries.size} finishes",
                style = MaterialTheme.typography.titleSmall, color = LockrColors.TextPrimary)
            Spacer(Modifier.weight(1f))
            Text("SWIPE →", style = MaterialTheme.typography.labelSmall,
                color = LockrColors.Accent)
        }
        LazyHorizontalGrid(rows = GridCells.Fixed(2), modifier = Modifier.fillMaxWidth().height(175.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(CardArtStyle.entries) { art ->
                Column(Modifier.width(98.dp).clickable { onSelect(art) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val shape = RoundedCornerShape(13.dp)
                    Box(Modifier.width(96.dp).height(60.dp).clip(shape)) {
                        CardArtBackground(art, Modifier.fillMaxSize())
                        Box(Modifier.fillMaxSize().border(if (selected == art) 3.dp else 1.dp,
                            if (selected == art) LockrColors.Accent else Color.White, shape))
                        if (art.animated) {
                            Text("MOTION", style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.align(Alignment.TopEnd)
                                    .background(Color(0x991D2057), RoundedCornerShape(bottomStart = 7.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                    }
                    Text(art.label, style = MaterialTheme.typography.labelSmall,
                        color = if (selected == art) LockrColors.Accent else LockrColors.TextSecondary)
                }
            }
        }
    }
}

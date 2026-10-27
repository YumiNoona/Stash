package com.lockr.app

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

enum class CardArtStyle(val label: String, @DrawableRes val image: Int, val dark: Boolean = false) {
    AURORA("Aurora", R.drawable.card_aurora_art),
    MIDNIGHT("Midnight", R.drawable.card_midnight_art, dark = true),
    OPAL("Opal", R.drawable.card_opal_v2),
    SOLAR("Solar", R.drawable.card_solar_v2),
    LAGOON("Lagoon", R.drawable.card_lagoon_v2, dark = true),
    BLOOM("Bloom", R.drawable.card_bloom_v2),
    CHROME("Chrome", R.drawable.card_chrome_v2),
    AMETHYST("Amethyst", R.drawable.card_amethyst_v2, dark = true),
    NEON("Neon", R.drawable.card_neon_v2, dark = true),
    COMET("Comet", R.drawable.card_comet_v2, dark = true),
    CITRUS("Citrus", R.drawable.card_citrus_v2),
    ICE("Ice", R.drawable.card_ice_v2),
    EMBER("Ember", R.drawable.card_ember_v2, dark = true),
    MOSAIC("Mosaic", R.drawable.card_mosaic_v2, dark = true),
    PRISM("Prism", R.drawable.card_prism_v2, dark = true)
}

fun cardArtStyle(item: VaultItem): CardArtStyle =
    CardArtStyle.entries.firstOrNull { it.name.equals(item.metadata["cardArt"], true) }
        ?: listOf(CardArtStyle.AURORA, CardArtStyle.OPAL, CardArtStyle.MIDNIGHT)
            .let { it[(item.id.hashCode() and Int.MAX_VALUE) % 3] }

@Composable
fun CardArtBackground(art: CardArtStyle, modifier: Modifier = Modifier) {
    Image(painterResource(art.image), null, contentScale = ContentScale.Crop, modifier = modifier)
}

val selectableCardArt = CardArtStyle.entries.toList()

@Composable
fun CardArtPicker(selected: CardArtStyle, customSelected: Boolean = false, onImport: (() -> Unit)? = null, onSelect: (CardArtStyle) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text("Card art · swipe to choose", style = MaterialTheme.typography.titleSmall, color = LockrColors.TextPrimary, modifier = Modifier.weight(1f))
        if (onImport != null) IconButton(onClick = onImport) { Icon(Icons.Outlined.AddPhotoAlternate, "Custom card art", tint = LockrColors.Accent) }
        }
        LazyRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(selectableCardArt) { art ->
                val chosen = !customSelected && selected == art
                Column(Modifier.width(216.dp).clickable { onSelect(art) },
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val shape = RoundedCornerShape(16.dp)
                    Box(Modifier.fillMaxWidth().aspectRatio(1.586f).clip(shape)) {
                        CardArtBackground(art, Modifier.fillMaxSize())
                        Box(Modifier.fillMaxSize().border(if (chosen) 3.dp else 1.dp,
                            if (chosen) LockrColors.Accent else LockrColors.Line, shape))
                    }
                    Text(art.label, style = MaterialTheme.typography.labelMedium,
                        color = if (chosen) LockrColors.Accent else LockrColors.TextSecondary)
                }
            }
        }
    }
}

package com.lockr.app

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

enum class CardArtStyle(
    val label: String,
    @DrawableRes val image: Int,
    val dark: Boolean = false,
    val animated: Boolean = false
) {
    AURORA("Aurora", R.drawable.card_aurora_art),
    OPAL("Opal", R.drawable.card_opal_art),
    MIDNIGHT("Midnight", R.drawable.card_midnight_art, dark = true, animated = true)
}

fun cardArtStyle(item: VaultItem): CardArtStyle =
    CardArtStyle.entries.firstOrNull { it.name.equals(item.metadata["cardArt"], ignoreCase = true) }
        ?: CardArtStyle.entries[(item.id.hashCode() and Int.MAX_VALUE) % CardArtStyle.entries.size]

@Composable
fun CardArtPicker(selected: CardArtStyle, onSelect: (CardArtStyle) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Card art", style = MaterialTheme.typography.titleSmall, color = LockrColors.TextPrimary)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CardArtStyle.entries.forEach { art ->
                Column(Modifier.width(98.dp).clickable { onSelect(art) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val shape = RoundedCornerShape(13.dp)
                    Box(Modifier.width(96.dp).height(60.dp).clip(shape)) {
                        Image(painterResource(art.image), contentDescription = null,
                            contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
                        Box(Modifier.matchParentSize().border(if (selected == art) 3.dp else 1.dp,
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

package com.lockr.app

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/** Launcher click opens a brief fan-out scene in the app, after its lock gate. */
@Composable
fun WidgetOpening(documents: Boolean) {
    val progress = remember { Animatable(0f) }
    val context = LocalContext.current
    val dark = LocalDarkTheme.current
    val enabled = LocalMotionEnabled.current
    LaunchedEffect(Unit) { if (enabled) progress.animateTo(1f, tween(800, easing = FastOutSlowInEasing)) else progress.snapTo(1f) }
    val art = remember(documents, dark) { widgetArtwork(context, documents, dark, "Opening your Stash") }
    Box(Modifier.fillMaxSize().background(LockrColors.Pearl), contentAlignment = Alignment.Center) {
        Box(Modifier.width(320.dp).height(340.dp), contentAlignment = Alignment.Center) {
            repeat(3) { i ->
                Box(Modifier.size(175.dp, 112.dp).graphicsLayer {
                    val p = progress.value
                    translationY = (-55f - i * 38f) * p * density
                    translationX = (i - 1) * 38f * p * density
                    rotationZ = (i - 1) * 12f * p
                    alpha = p
                }.clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(
                    listOf(listOf(Color(0xFFE5DCFF), Color(0xFFA1E5EE), Color(0xFFF8C8AE))[i], Color(0xFF899CCB))))) {
                    Text(if (documents) "Page ${i+1}" else "••••  ••••  ••••", color = Color(0xFF24334C), modifier = Modifier.padding(20.dp), style = MaterialTheme.typography.labelLarge)
                }
            }
            Image(art.asImageBitmap(), if (documents) "Opening documents" else "Opening wallet", modifier = Modifier.width(320.dp).graphicsLayer { translationY = 45f * density * progress.value })
        }
    }
}

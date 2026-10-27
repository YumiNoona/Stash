package com.lockr.app

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.*

/** Decorative layers never move or blur card text. All positions are deterministic. */
@Composable
fun CardMotionOverlay(motion: CardMotion, phase: Float, modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val t = (phase + 1f) / 2f
        when (motion) {
            CardMotion.RIPPLE -> repeat(3) { i ->
                val progress = (t + i / 3f) % 1f
                drawCircle(Color.White.copy(alpha = (1f - progress) * 0.2f), w * progress,
                    Offset(w * 0.8f, h * 0.2f), style = Stroke(1.5f))
            }
            CardMotion.AURORA -> repeat(3) { i ->
                val path = Path().apply {
                    moveTo(-w * 0.1f, h * (0.2f + i * 0.2f))
                    cubicTo(w * 0.3f, h * (t - 0.4f), w * 0.6f, h * (1.3f - t), w * 1.1f, h * (0.5f + i * 0.2f))
                }
                drawPath(path, Brush.linearGradient(listOf(Color(0x0096FFF3), Color(0x558DDBFF), Color(0x55F5B0E9), Color.Transparent)),
                    style = Stroke(w * 0.06f))
            }
            CardMotion.SPARKLE -> repeat(18) { i ->
                val x = ((i * 73 + 17) % 100) / 100f * w
                val y = ((i * 47 + 23) % 100) / 100f * h
                val alpha = ((sin(t * PI * 2 + i * 1.7) + 1f) / 2f).toFloat() * 0.55f
                val r = 2f + alpha * 5f
                drawLine(Color.White.copy(alpha = alpha), Offset(x - r, y), Offset(x + r, y), 1f)
                drawLine(Color.White.copy(alpha = alpha), Offset(x, y - r), Offset(x, y + r), 1f)
            }
            CardMotion.ORBIT -> {
                val center = Offset(w * (0.5f + cos(t * PI * 2).toFloat() * 0.4f), h * (0.5f + sin(t * PI * 2).toFloat() * 0.35f))
                drawCircle(Brush.radialGradient(listOf(Color(0x55C6F9FF), Color(0x22CAACFF), Color.Transparent), center, w * 0.32f), w * 0.32f, center)
            }
            else -> Unit
        }
    }
}

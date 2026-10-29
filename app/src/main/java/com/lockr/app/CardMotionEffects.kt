package com.lockr.app

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.*

/** Seamless decorative light only. Card data stays still and readable. */
@Composable
fun CardMotionOverlay(motion: CardMotion, phase: () -> Float, modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width; val h = size.height
        val t = (phase() + 1f) / 2f
        val angle = t * PI * 2
        when (motion) {
            CardMotion.SHEEN, CardMotion.TILT -> {
                // A broad soft reflection followed by a narrow metallic glint; rest between passes.
                val pass = (t / 0.7f).coerceIn(0f, 1f)
                val x = -w * 0.55f + pass * w * 2.1f
                val strength = sin(pass * PI).toFloat().coerceAtLeast(0f)
                drawRect(Brush.linearGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.04f * strength),
                    Color.White.copy(alpha = 0.24f * strength), Color.White.copy(alpha = 0.04f * strength), Color.Transparent),
                    Offset(x - w * 0.18f, 0f), Offset(x + w * 0.18f, h)))
                if (motion == CardMotion.TILT) drawRoundRect(Color(0xFFCAF5FF).copy(alpha = strength * 0.18f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(22f), style = Stroke(2f))
            }
            CardMotion.RIPPLE -> {
                val alpha = (sin(angle) * 0.5 + 0.5).toFloat() * 0.1f
                val center = Offset(w * 0.8f, h * 0.1f)
                drawCircle(Brush.radialGradient(listOf(Color.Transparent, Color(0xFFDBDCFF).copy(alpha = alpha), Color.Transparent), center, w * 0.65f), w * 0.65f, center)
            }
            CardMotion.AURORA -> {
                repeat(2) { i ->
                    val center = Offset(w * (0.5f + sin(angle + i * PI).toFloat() * 0.5f), h * (0.5f + cos(angle + i).toFloat() * 0.35f))
                    drawCircle(Brush.radialGradient(listOf((if (i == 0) Color(0xFF83EDE0) else Color(0xFFD6B4FF)).copy(alpha = 0.14f), Color.Transparent), center, w * 0.65f), w * 0.65f, center)
                }
            }
            CardMotion.SPARKLE -> repeat(7) { i ->
                val x = ((i * 73 + 17) % 100) / 100f * w
                val y = ((i * 47 + 23) % 100) / 100f * h
                val pulse = max(0.0, sin(angle + i * 1.7)).pow(8).toFloat()
                val r = w * 0.006f * pulse
                drawLine(Color.White.copy(alpha = pulse * 0.65f), Offset(x - r * 2, y), Offset(x + r * 2, y), 1.2f)
                drawLine(Color.White.copy(alpha = pulse * 0.65f), Offset(x, y - r * 2), Offset(x, y + r * 2), 1.2f)
            }
            CardMotion.ORBIT -> {
                val center = Offset(w * (0.5f + cos(angle).toFloat() * 0.55f), h * (0.5f + sin(angle).toFloat() * 0.55f))
                drawCircle(Brush.radialGradient(listOf(Color(0x25C6F9FF), Color(0x10CAACFF), Color.Transparent), center, w * 0.42f), w * 0.42f, center)
            }
            else -> Unit
        }
    }
}

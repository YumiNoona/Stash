package com.lockr.app

import kotlin.math.hypot
import kotlin.math.roundToInt

internal data class CropCorner(val x: Float, val y: Float)

/** Corners must stay clockwise, convex and within the image. Distances use actual pixels. */
internal fun correctedDocumentSize(points: List<CropCorner>, width: Int, height: Int): Pair<Int, Int> {
    require(width > 0 && height > 0 && points.size == 4)
    require(points.all { it.x.isFinite() && it.y.isFinite() && it.x in 0f..1f && it.y in 0f..1f })
    for (i in points.indices) {
        val a = points[i]; val b = points[(i + 1) % 4]; val c = points[(i + 2) % 4]
        require((b.x - a.x) * (c.y - b.y) - (b.y - a.y) * (c.x - b.x) > 0.0001f) {
            "Keep the corners in order without crossing the edges."
        }
    }
    fun distance(a: Int, b: Int): Double = hypot(
        (points[a].x - points[b].x).toDouble() * width,
        (points[a].y - points[b].y).toDouble() * height)
    val top = distance(0, 1); val bottom = distance(3, 2)
    val left = distance(0, 3); val right = distance(1, 2)
    require(listOf(top, bottom, left, right).all { it > 80 }) { "Move the corners farther apart." }
    return maxOf(top, bottom).roundToInt().coerceIn(100, 2400) to maxOf(left, right).roundToInt().coerceIn(100, 3200)
}

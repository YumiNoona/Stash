package com.lockr.app

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Matrix
import android.graphics.Paint
import android.util.Base64
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentEdgeCorrectionSheet(attachment: VaultAttachment, onDismiss: () -> Unit,
                                onApply: (VaultAttachment) -> Unit) {
    var bitmap by remember(attachment.base64Data) { mutableStateOf<Bitmap?>(null) }
    var points by remember(attachment.base64Data) { mutableStateOf(listOf(
        Offset(.06f,.06f), Offset(.94f,.06f), Offset(.94f,.94f), Offset(.06f,.94f))) }
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    var activeCorner by remember { mutableIntStateOf(-1) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    LaunchedEffect(attachment.base64Data) {
        bitmap = withContext(Dispatchers.IO) { decodeAttachmentPreview(attachment.base64Data, 2400, 3200) }
    }
    ModalBottomSheet(onDismissRequest = { if (!busy) onDismiss() }, containerColor = LockrColors.Surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Correct page edges", style = MaterialTheme.typography.titleLarge)
            Text("Drag the four corners to the page edges. The exported scan will be straightened.",
                style = MaterialTheme.typography.bodySmall, color = LockrColors.TextSecondary)
            val source = bitmap
            if (source == null) LinearProgressIndicator(Modifier.fillMaxWidth())
            else Box(Modifier.fillMaxWidth().height(400.dp).onSizeChanged { viewport = it }
                .pointerInput(source, viewport, points) {
                    detectDragGestures(onDragStart = { touch ->
                        val rect = fittedImageRect(viewport, source)
                        activeCorner = points.indices.minByOrNull { i ->
                            val p = points[i]; (Offset(rect.first + p.x * rect.third, rect.second + p.y * rect.fourth) - touch).getDistance()
                        } ?: -1
                    }, onDragEnd = { activeCorner = -1 }) { change, drag ->
                        change.consume()
                        val rect = fittedImageRect(viewport, source)
                        if (activeCorner in points.indices && rect.third > 0f && rect.fourth > 0f) {
                            val old = points[activeCorner]
                            points = points.toMutableList().apply { this[activeCorner] = Offset(
                                (old.x + drag.x / rect.third).coerceIn(0f, 1f),
                                (old.y + drag.y / rect.fourth).coerceIn(0f, 1f)) }
                        }
                    }
                }, contentAlignment = Alignment.Center) {
                Image(source.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                Canvas(Modifier.fillMaxSize()) {
                    val rect = fittedImageRect(viewport, source)
                    val mapped = points.map { Offset(rect.first + it.x * rect.third, rect.second + it.y * rect.fourth) }
                    val outline = Path().apply {
                        moveTo(mapped[0].x, mapped[0].y)
                        mapped.drop(1).forEach { lineTo(it.x, it.y) }
                        close()
                    }
                    drawPath(outline, Color(0xFF8FF6EA), style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round))
                    mapped.forEach { p ->
                        drawCircle(Color.White, 13.dp.toPx(), p)
                        drawCircle(Color(0xFF31B5AD), 9.dp.toPx(), p)
                    }
                }
            }
            if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            Button(onClick = {
                val source = bitmap ?: return@Button
                busy = true; error = ""
                scope.launch {
                    try {
                        val corrected = withContext(Dispatchers.Default) { correctDocumentEdges(attachment, source, points) }
                        onApply(corrected)
                    } catch (_: Exception) { error = "Could not correct this page. Try moving the corners farther apart." }
                    finally { busy = false }
                }
            }, enabled = bitmap != null && !busy, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text(if (busy) "Correcting…" else "Use corrected page")
            }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) { Text("Keep original") }
        }
    }
}

private data class ImageRect(val first: Float, val second: Float, val third: Float, val fourth: Float)
private fun fittedImageRect(viewport: IntSize, bitmap: Bitmap): ImageRect {
    if (viewport.width == 0 || viewport.height == 0) return ImageRect(0f, 0f, 0f, 0f)
    val scale = minOf(viewport.width.toFloat() / bitmap.width, viewport.height.toFloat() / bitmap.height)
    val width = bitmap.width * scale; val height = bitmap.height * scale
    return ImageRect((viewport.width - width) / 2f, (viewport.height - height) / 2f, width, height)
}

internal fun correctDocumentEdges(attachment: VaultAttachment, bitmap: Bitmap, points: List<Offset>): VaultAttachment {
    require(points.size == 4)
    val top = (points[1] - points[0]).getDistance() * bitmap.width
    val bottom = (points[2] - points[3]).getDistance() * bitmap.width
    val left = (points[3] - points[0]).getDistance() * bitmap.height
    val right = (points[2] - points[1]).getDistance() * bitmap.height
    val width = maxOf(top, bottom).toInt().coerceIn(100, 2400)
    val height = maxOf(left, right).toInt().coerceIn(100, 3200)
    require(top > 80 && bottom > 80 && left > 80 && right > 80)
    val src = floatArrayOf(points[0].x * bitmap.width, points[0].y * bitmap.height,
        points[1].x * bitmap.width, points[1].y * bitmap.height,
        points[2].x * bitmap.width, points[2].y * bitmap.height,
        points[3].x * bitmap.width, points[3].y * bitmap.height)
    val dst = floatArrayOf(0f,0f,width.toFloat(),0f,width.toFloat(),height.toFloat(),0f,height.toFloat())
    val transform = Matrix()
    require(transform.setPolyToPoly(src, 0, dst, 0, 4))
    val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(output)
    canvas.drawColor(android.graphics.Color.WHITE)
    canvas.drawBitmap(bitmap, transform, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
    val stream = ByteArrayOutputStream()
    output.compress(Bitmap.CompressFormat.JPEG, 92, stream)
    output.recycle()
    val bytes = stream.toByteArray()
    require(bytes.size <= MAX_VAULT_ATTACHMENT_BYTES)
    return attachment.copy(name = attachment.name.substringBeforeLast('.') + "-corrected.jpg",
        mimeType = "image/jpeg", byteCount = bytes.size.toLong(), base64Data = Base64.encodeToString(bytes, Base64.NO_WRAP))
}

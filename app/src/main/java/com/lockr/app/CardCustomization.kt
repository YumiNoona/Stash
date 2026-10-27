package com.lockr.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Movie
import android.graphics.SurfaceTexture
import android.media.MediaDataSource
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.os.SystemClock
import android.util.Base64
import android.view.Surface
import android.view.TextureView
import android.view.View
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

enum class CardMotion(val label: String) {
    NONE("Still"), SHEEN("Light sweep"), FLOAT("Float"), PULSE("Glow"), TILT("Tilt"),
    DRIFT("Drift"), ZOOM("Breathe"), RIPPLE("Ripple"), AURORA("Aurora flow"), SPARKLE("Stardust"), ORBIT("Orbit")
}

fun cardMotion(item: VaultItem): CardMotion =
    CardMotion.entries.firstOrNull { it.name == item.metadata["cardMotion"] } ?: CardMotion.NONE

fun customCardArt(item: VaultItem): VaultAttachment? = item.metadata["cardArtAttachment"]?.let { name ->
    item.attachments.firstOrNull { it.name == name }
}

/** Keep original scans and artwork separate; replacing artwork never removes a scan. */
fun withCardArt(attachments: List<VaultAttachment>, previousName: String?, art: VaultAttachment?): List<VaultAttachment> =
    attachments.filterNot { previousName != null && it.name == previousName } + listOfNotNull(art)

@Composable
fun CardCustomization(
    art: CardArtStyle, onArt: (CardArtStyle) -> Unit,
    motion: CardMotion, onMotion: (CardMotion) -> Unit,
    media: VaultAttachment?, onMedia: (VaultAttachment?) -> Unit,
    lightText: Boolean, onLightText: (Boolean) -> Unit,
    showLabels: Boolean = true
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            busy = true
            message = ""
            try {
                val candidate = readVaultAttachment(context, uri, 8L * 1024 * 1024)
                withContext(Dispatchers.IO) { validateCardArtMedia(candidate) }
                onMedia(candidate.copy(name = "card-art-${java.util.UUID.randomUUID()}-${candidate.name}"))
            } catch (_: Exception) {
                message = "Choose a supported image, GIF, or video under 8 MB. Videos can be up to 30 seconds."
            } finally { busy = false }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CardArtPicker(art, customSelected = media != null, showLabels = showLabels,
            onImport = if (busy) null else ({ picker.launch(arrayOf("image/*", "video/*")) })) { onArt(it); onMedia(null) }
        if (busy) Text("Opening artwork…", style = MaterialTheme.typography.bodySmall)
        if (media != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(lightText, { onLightText(true) }, label = { Text("Light lettering") })
                FilterChip(!lightText, { onLightText(false) }, label = { Text("Dark lettering") })
            }
        }
        if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall)
        if (showLabels) Text("Motion", style = MaterialTheme.typography.titleSmall)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CardMotion.entries.forEach { option ->
                FilterChip(motion == option, { onMotion(option) }, label = { Text(option.label) })
            }
        }
    }
}

private class ArtDataSource(private val bytes: ByteArray) : MediaDataSource() {
    override fun getSize(): Long = bytes.size.toLong()
    override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
        if (position < 0 || position >= bytes.size) return -1
        val count = minOf(size, bytes.size - position.toInt())
        bytes.copyInto(buffer, offset, position.toInt(), position.toInt() + count)
        return count
    }
    override fun close() = Unit
}

private fun validateCardArtMedia(attachment: VaultAttachment) {
    val bytes = Base64.decode(attachment.base64Data, Base64.DEFAULT)
    if (attachment.mimeType.startsWith("video/")) {
        val reader = MediaMetadataRetriever()
        try {
            reader.setDataSource(ArtDataSource(bytes))
            val duration = reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0
            require(duration in 1..30_000)
            require(reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()?.let { it > 0 } == true)
        } finally { reader.release() }
    } else {
        require(attachment.mimeType.startsWith("image/"))
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0 && bounds.outWidth.toLong() * bounds.outHeight <= 16_000_000)
    }
}

private data class DecodedArt(val bytes: ByteArray, val bitmap: Bitmap?, val movie: Movie?)

@Composable
fun CustomCardArtBackground(attachment: VaultAttachment, modifier: Modifier = Modifier, animate: Boolean = true) {
    var decoded by remember(attachment) { mutableStateOf<DecodedArt?>(null) }
    LaunchedEffect(attachment) {
        decoded = withContext(Dispatchers.IO) {
            val bytes = Base64.decode(attachment.base64Data, Base64.DEFAULT)
            if (attachment.mimeType.startsWith("video/")) DecodedArt(bytes, null, null)
            else {
                val movie = if (attachment.mimeType == "image/gif") Movie.decodeByteArray(bytes, 0, bytes.size) else null
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, this)
                    inSampleSize = max(1, max(outWidth, outHeight) / 1024)
                    inJustDecodeBounds = false
                }
                DecodedArt(bytes, if (movie == null) BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) else null, movie)
            }
        }
    }
    var running by remember { mutableStateOf(true) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        running = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        val observer = LifecycleEventObserver { _, _ -> running = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    decoded?.let { data ->
        key(attachment.name) {
            if (attachment.mimeType.startsWith("video/")) {
                AndroidView(factory = { ArtVideoView(it, data.bytes) }, modifier = modifier,
                    onRelease = { it.release() }, update = { it.playing = animate && running })
            } else {
                AndroidView(factory = { ArtImageView(it, data) }, modifier = modifier,
                    update = { it.playing = animate && running; it.invalidate() })
            }
        }
    }
}

private class ArtImageView(context: Context, private val art: DecodedArt) : View(context) {
    var playing = true
    private val start = SystemClock.uptimeMillis()
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = art.movie?.width() ?: art.bitmap?.width ?: return
        val h = art.movie?.height() ?: art.bitmap?.height ?: return
        if (w <= 0 || h <= 0) return
        val scale = max(width.toFloat() / w, height.toFloat() / h)
        canvas.save()
        canvas.translate((width - w * scale) / 2, (height - h * scale) / 2)
        canvas.scale(scale, scale)
        art.movie?.let {
            it.setTime(if (playing) ((SystemClock.uptimeMillis() - start) % max(1, it.duration())).toInt() else 0)
            it.draw(canvas, 0f, 0f)
            if (playing) postInvalidateOnAnimation()
        } ?: art.bitmap?.let { canvas.drawBitmap(it, 0f, 0f, android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG)) }
        canvas.restore()
    }
}

private class ArtVideoView(context: Context, private val bytes: ByteArray) : TextureView(context), TextureView.SurfaceTextureListener {
    private var player: MediaPlayer? = null
    private var surface: Surface? = null
    private var ready = false
    var playing = true
        set(value) { field = value; if (ready) { if (value) player?.start() else player?.pause() } }
    init { surfaceTextureListener = this; isOpaque = false }
    override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
        surface = Surface(texture)
        player = MediaPlayer().apply {
            setSurface(surface)
            setVolume(0f, 0f)
            isLooping = true
            setOnPreparedListener {
                ready = true
                crop()
                if (playing) start() else seekTo(0)
            }
            setOnErrorListener { _, _, _ -> ready = false; true }
            try { setDataSource(ArtDataSource(bytes)); prepareAsync() } catch (_: Exception) { release() }
        }
    }
    private fun crop() {
        val p = player ?: return
        if (p.videoWidth <= 0 || p.videoHeight <= 0 || width == 0 || height == 0) return
        val scale = max(width.toFloat() / p.videoWidth, height.toFloat() / p.videoHeight)
        setTransform(android.graphics.Matrix().apply {
            setScale(p.videoWidth * scale / width, p.videoHeight * scale / height, width / 2f, height / 2f)
        })
    }
    override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) = crop()
    override fun onSurfaceTextureUpdated(texture: SurfaceTexture) = Unit
    override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean { release(); return true }
    fun release() { ready = false; player?.release(); player = null; surface?.release(); surface = null }
}

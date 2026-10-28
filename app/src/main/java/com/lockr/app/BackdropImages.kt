package com.lockr.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.AtomicFile
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID

internal const val MAX_BACKDROP_BYTES = 32L * 1024 * 1024

internal fun backdropSampleSize(width: Int, height: Int): Int {
    require(width > 0 && height > 0 && width <= 32_768 && height <= 32_768 && width.toLong() * height <= 200_000_000) {
        "Choose an image smaller than 200 megapixels."
    }
    var sample = 1
    while ((width + sample - 1) / sample > 1600 || (height + sample - 1) / sample > 1600) sample *= 2
    return sample
}

internal fun backdropTargetDark(target: String, currentDark: Boolean): Boolean = when (target) {
    "Dark" -> true
    "Light" -> false
    else -> currentDark
}

internal fun customBackdropFile(context: Context, dark: Boolean): File {
    val prefs = context.getSharedPreferences("lockr_preferences", 0)
    val name = prefs.getString(if (dark) "backdrop_custom_dark_file" else "backdrop_custom_light_file", null)
    return if (name != null && name.matches(Regex("custom-backdrop-[a-f0-9-]+\\.jpg"))) File(context.filesDir, name)
        else File(context.filesDir, if (dark) "custom-backdrop-dark.jpg" else "custom-backdrop-light.jpg")
}

/** Stage a bounded, oriented JPEG before atomically switching either mode's preference. */
internal suspend fun importBackdrop(context: Context, uri: Uri, target: String) = withContext(Dispatchers.IO) {
    require(target in listOf("Both", "Light", "Dark"))
    val source = File.createTempFile("backdrop-source-", ".img", context.cacheDir)
    val destination = File(context.filesDir, "custom-backdrop-${UUID.randomUUID()}.jpg")
    var decoded: Bitmap? = null
    var oriented: Bitmap? = null
    var committed = false
    try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            source.outputStream().use { output ->
                val buffer = ByteArray(16 * 1024)
                var total = 0L
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    if (total > MAX_BACKDROP_BYTES) throw IOException("Choose an image under 32 MB.")
                    output.write(buffer, 0, count)
                }
            }
        } ?: throw IOException("This image cannot be opened.")
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(source.absolutePath, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "This image format is not supported." }
        val sample = backdropSampleSize(bounds.outWidth, bounds.outHeight)
        decoded = BitmapFactory.decodeFile(source.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: throw IOException("This image format is not supported.")
        val orientation = runCatching { ExifInterface(source).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }
            .getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        val transform = Matrix().apply {
            when (orientation) {
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
                ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> setScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> { setRotate(90f); postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
                ExifInterface.ORIENTATION_TRANSVERSE -> { setRotate(-90f); postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(-90f)
            }
        }
        val image = decoded!!
        oriented = if (transform.isIdentity) image else Bitmap.createBitmap(image, 0, 0, image.width, image.height, transform, true)
        val atomic = AtomicFile(destination)
        val stream = atomic.startWrite()
        try {
            if (!oriented!!.compress(Bitmap.CompressFormat.JPEG, 88, stream)) throw IOException("Could not prepare the photo.")
            atomic.finishWrite(stream)
        } catch (error: Throwable) { atomic.failWrite(stream); throw error }
        currentCoroutineContext().ensureActive()
        val prefs = context.getSharedPreferences("lockr_preferences", 0)
        val editor = prefs.edit()
        if (target != "Dark") editor.putString("backdrop_light", "custom").putString("backdrop_custom_light_file", destination.name)
        if (target != "Light") editor.putString("backdrop_dark", "custom").putString("backdrop_custom_dark_file", destination.name)
        if (!editor.commit()) throw IOException("Could not save the background preference.")
        committed = true
        // Keep old files for a grace period: Compose may still be decoding a previous choice.
        val referenced = listOf(customBackdropFile(context, false).name, customBackdropFile(context, true).name)
        context.filesDir.listFiles()?.filter { it.name.startsWith("custom-backdrop-") && it.name !in referenced &&
            System.currentTimeMillis() - it.lastModified() > 86_400_000L }?.forEach(File::delete)
    } catch (_: OutOfMemoryError) {
        throw IOException("This photo needs too much memory. Choose a smaller image.")
    } finally {
        if (oriented !== decoded) oriented?.recycle()
        decoded?.recycle()
        source.delete()
        if (!committed) destination.delete()
    }
}

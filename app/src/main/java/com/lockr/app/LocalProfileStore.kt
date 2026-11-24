package com.lockr.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.AtomicFile
import java.io.File

/** Profile details remain in app-private storage and are excluded from Android backup. */
class LocalProfileStore(private val context: Context) {
    private val preferences = context.getSharedPreferences("lockr_preferences", Context.MODE_PRIVATE)
    private val photoFile = AtomicFile(File(context.filesDir, "stash-profile.jpg"))

    fun name(): String = preferences.getString("profile_name", "Stash").orEmpty().ifBlank { "Stash" }

    fun saveName(value: String) {
        preferences.edit().putString("profile_name", value.trim().take(40).ifBlank { "Stash" }).apply()
    }

    fun photo(): Bitmap? = try {
        photoFile.openRead().use { BitmapFactory.decodeStream(it) }
    } catch (_: Exception) {
        null
    }

    fun savePhoto(uri: Uri): Bitmap {
        val bytes = context.contentResolver.openInputStream(uri)?.use { readBoundedBytes(it, 32 * 1024 * 1024) }
            ?: throw IllegalArgumentException("Could not read this photo")
        val bitmap = try { decodeImagePreview(bytes, 1024, 1024, Bitmap.Config.ARGB_8888) }
            finally { bytes.fill(0) }
        requireNotNull(bitmap) { "This photo cannot be opened" }
        try {
            val output = photoFile.startWrite()
            try {
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 86, output)) throw IllegalStateException("Could not save this photo")
                photoFile.finishWrite(output)
            } catch (error: Throwable) {
                photoFile.failWrite(output)
                throw error
            }
            return bitmap
        } catch (error: Throwable) {
            bitmap.recycle()
            throw error
        }
    }

    fun removePhoto() {
        photoFile.delete()
    }
}

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
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val boundsInput = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Could not read this photo")
        boundsInput.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IllegalArgumentException("This photo cannot be opened")
        var sample = 1
        while (bounds.outWidth / sample > 1024 || bounds.outHeight / sample > 1024) sample *= 2
        val bitmap = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: throw IllegalArgumentException("Could not read this photo")
        val output = photoFile.startWrite()
        try {
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 86, output)) throw IllegalStateException("Could not save this photo")
            photoFile.finishWrite(output)
        } catch (error: Exception) {
            photoFile.failWrite(output)
            throw error
        }
        return bitmap
    }

    fun removePhoto() {
        photoFile.delete()
    }
}

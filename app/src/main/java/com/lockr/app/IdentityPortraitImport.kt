package com.lockr.app

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.UUID

/** The selected image is bounded, oriented and copied into the encrypted attachment set on Save. */
internal suspend fun importIdentityPortrait(context: Context, uri: Uri): VaultAttachment = withContext(Dispatchers.IO) {
    val source=readVaultAttachment(context,uri,8L*1024*1024)
    val bitmap=decodeAttachmentPreview(source.base64Data,900,1200)
        ?: throw java.io.IOException("This photo cannot be opened. Choose a JPG or PNG image.")
    try {
        val output=ByteArrayOutputStream()
        check(bitmap.compress(Bitmap.CompressFormat.JPEG,92,output)) { "Could not prepare the photo." }
        val bytes=output.toByteArray()
        require(bytes.size<=2*1024*1024) { "Choose a smaller portrait photo." }
        try { VaultAttachment("stash-id-photo-${UUID.randomUUID()}.jpg","image/jpeg",bytes.size.toLong(),Base64.encodeToString(bytes,Base64.NO_WRAP)) }
        finally { bytes.fill(0) }
    } finally { bitmap.recycle() }
}

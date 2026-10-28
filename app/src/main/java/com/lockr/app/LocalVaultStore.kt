package com.lockr.app

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

const val MAX_VAULT_FILE_BYTES = 64 * 1024 * 1024

/** Stores the whole vault as authenticated ciphertext in app-private storage. */
class LocalVaultStore(context: Context) {
    private val vaultFile = File(context.filesDir, "lockr-vault.bin")
    private val atomicFile = AtomicFile(vaultFile)

    @Synchronized
    fun loadOrNull(): List<VaultItem>? {
        val encoded = try {
            atomicFile.openRead().use { it.readBytes() }
        } catch (_: FileNotFoundException) {
            return null
        }
        val loaded = decodeEncrypted(encoded)
        val safeItems = loaded.map { item ->
            val safeMetadata = item.metadata.filterKeys { key ->
                !key.equals("securityCode", true) && !key.equals("cvv", true) && !key.equals("cvc", true)
            }
            if (safeMetadata.size == item.metadata.size) item else item.copy(metadata = safeMetadata)
        }
        if (safeItems != loaded) save(safeItems)
        return safeItems
    }

    /** Returns the encrypted vault bytes for a user-selected local backup destination. */
    @Synchronized
    fun exportEncryptedBackup(): ByteArray = atomicFile.openRead().use { it.readBytes() }

    /** Authenticates and decodes a candidate backup without modifying the current vault. */
    @Synchronized
    fun validateEncryptedBackup(encoded: ByteArray): List<VaultItem> = decodeEncrypted(encoded)

    /** Validates first, then atomically replaces the current encrypted vault file. */
    @Synchronized
    fun restoreEncryptedBackup(encoded: ByteArray): List<VaultItem> {
        if (encoded.size > MAX_BACKUP_BYTES) throw IOException("Backup file is too large")
        val decodedItems = decodeEncrypted(encoded)
        // Re-encode restored records so backups created by older builds cannot reintroduce
        // card verification codes that the current vault format intentionally discards.
        val restoredItems = decodedItems.map { item ->
            val safeMetadata = item.metadata.filterKeys { key ->
                !key.equals("securityCode", true) && !key.equals("cvv", true) && !key.equals("cvc", true)
            }
            if (safeMetadata.size == item.metadata.size) item else item.copy(metadata = safeMetadata)
        }
        save(restoredItems)
        return restoredItems
    }

    private fun decodeEncrypted(encoded: ByteArray): List<VaultItem> {
        if (encoded.size < 1 + IV_LENGTH + TAG_LENGTH_BYTES) {
            throw IOException("Encrypted vault file is incomplete")
        }
        if (encoded.size > MAX_BACKUP_BYTES) throw IOException("Encrypted vault file is too large")
        val version = encoded[0]
        if (version != FORMAT_VERSION) throw IOException("Unsupported vault file version")

        val iv = encoded.copyOfRange(1, 1 + IV_LENGTH)
        val ciphertext = encoded.copyOfRange(1 + IV_LENGTH, encoded.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(TAG_LENGTH_BITS, iv))
        cipher.updateAAD(AUTHENTICATED_HEADER)
        val plaintext = cipher.doFinal(ciphertext)
        return decodeItems(plaintext)
    }

    @Synchronized
    fun save(items: List<VaultItem>) {
        val plaintext = encodeItems(items)
        if (plaintext.size + 1 + IV_LENGTH + TAG_LENGTH_BYTES > MAX_BACKUP_BYTES) {
            plaintext.fill(0)
            throw IOException("The encrypted vault exceeds the 64 MB local storage limit")
        }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        cipher.updateAAD(AUTHENTICATED_HEADER)
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext)
        val stream = atomicFile.startWrite()
        try {
            stream.write(FORMAT_VERSION.toInt())
            stream.write(iv)
            stream.write(ciphertext)
            atomicFile.finishWrite(stream)
        } catch (error: Throwable) {
            atomicFile.failWrite(stream)
            throw error
        } finally {
            plaintext.fill(0)
        }
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private fun encodeItems(items: List<VaultItem>): ByteArray {
        val json = JSONArray()
        items.forEach { item ->
            val attachments = JSONArray().apply {
                item.attachments.forEach { attachment ->
                    put(
                        JSONObject()
                            .put("name", attachment.name)
                            .put("mimeType", attachment.mimeType)
                            .put("byteCount", attachment.byteCount)
                            .put("base64Data", attachment.base64Data)
                    )
                }
            }
            json.put(
                JSONObject()
                    .put("id", item.id)
                    .put("createdAt", item.createdAt)
                    .put("title", item.title)
                    .put("category", item.category.name)
                    .put("detail", item.detail)
                    .put("time", item.time)
                    .put("lastFour", item.lastFour)
                    .put("metadata", JSONObject(item.metadata))
                    .put("attachments", attachments)
            )
        }
        return json.toString().toByteArray(Charsets.UTF_8)
    }

    private fun decodeItems(plaintext: ByteArray): List<VaultItem> {
        try {
            val array = JSONArray(String(plaintext, Charsets.UTF_8))
            return List(array.length()) { index ->
                val json = array.getJSONObject(index)
                VaultItem(
                    title = json.getString("title"),
                    createdAt = json.optLong("createdAt", 0L),
                    category = VaultCategory.valueOf(json.getString("category")),
                    detail = json.optString("detail"),
                    time = json.optString("time"),
                    lastFour = json.optString("lastFour"),
                    metadata = json.optJSONObject("metadata")?.let { fields ->
                        fields.keys().asSequence().associateWith { key -> fields.optString(key) }
                    } ?: emptyMap(),
                    attachments = json.optJSONArray("attachments")?.let { attachments ->
                        List(attachments.length()) { attachmentIndex ->
                            val attachment = attachments.getJSONObject(attachmentIndex)
                            VaultAttachment(
                                name = attachment.optString("name"),
                                mimeType = attachment.optString("mimeType", "application/octet-stream"),
                                byteCount = attachment.optLong("byteCount"),
                                base64Data = attachment.optString("base64Data")
                            )
                        }
                    } ?: emptyList(),
                    id = json.optString("id").takeIf { it.isNotBlank() } ?: java.util.UUID.randomUUID().toString()
                )
            }
        } finally {
            plaintext.fill(0)
        }
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "lockr_vault_aes_gcm_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val FORMAT_VERSION: Byte = 1
        const val IV_LENGTH = 12
        const val TAG_LENGTH_BITS = 128
        const val TAG_LENGTH_BYTES = TAG_LENGTH_BITS / 8
        const val MAX_BACKUP_BYTES = MAX_VAULT_FILE_BYTES
        val AUTHENTICATED_HEADER = "Lockr local vault format 1".toByteArray(Charsets.UTF_8)
    }
}

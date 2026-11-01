package com.lockr.app

import java.io.IOException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** Versioned, authenticated export that can be restored after an uninstall or on another device. */
internal object PortableVaultBackup {
    private val magic = "STASHPT1".toByteArray(Charsets.US_ASCII)
    private const val saltSize = 16
    private const val ivSize = 12
    private const val tagSize = 16
    private const val iterations = 310_000
    private const val headerSize = 8 + saltSize + ivSize
    const val maximumBytes = MAX_VAULT_FILE_BYTES + headerSize + tagSize

    fun isPortable(bytes: ByteArray): Boolean = bytes.size >= headerSize + tagSize &&
        bytes.copyOfRange(0, magic.size).contentEquals(magic)

    fun create(store: LocalVaultStore, items: List<VaultItem>, passphrase: CharArray): ByteArray {
        require(passphrase.size >= 12) { "Use a passphrase of at least 12 characters." }
        val plaintext = store.serializePortable(items)
        try {
            if (plaintext.size > MAX_VAULT_FILE_BYTES) throw IOException("The vault is too large for one backup.")
            return encryptPayload(plaintext, passphrase)
        } finally { plaintext.fill(0) }
    }

    fun read(store: LocalVaultStore, bytes: ByteArray, passphrase: CharArray): List<VaultItem> {
        return store.parsePortable(decryptPayload(bytes, passphrase))
    }

    internal fun encryptPayload(plaintext: ByteArray, passphrase: CharArray): ByteArray {
        require(passphrase.size >= 12) { "Use a passphrase of at least 12 characters." }
        val salt = ByteArray(saltSize).also(SecureRandom()::nextBytes)
        val iv = ByteArray(ivSize).also(SecureRandom()::nextBytes)
        val header = magic + salt + iv
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, deriveKey(passphrase, salt), GCMParameterSpec(128, iv))
        cipher.updateAAD(header)
        return header + cipher.doFinal(plaintext)
    }

    internal fun decryptPayload(bytes: ByteArray, passphrase: CharArray): ByteArray {
        if (!isPortable(bytes) || bytes.size > maximumBytes) throw IOException("This is not a supported portable backup.")
        val salt = bytes.copyOfRange(magic.size, magic.size + saltSize)
        val iv = bytes.copyOfRange(magic.size + saltSize, headerSize)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, deriveKey(passphrase, salt), GCMParameterSpec(128, iv))
        cipher.updateAAD(bytes, 0, headerSize)
        return cipher.doFinal(bytes, headerSize, bytes.size - headerSize)
    }

    private fun deriveKey(passphrase: CharArray, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(passphrase, salt, iterations, 256)
        return try {
            val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            try { SecretKeySpec(bytes, "AES") } finally { bytes.fill(0) }
        } finally { spec.clearPassword() }
    }
}

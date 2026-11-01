package com.lockr.app

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PortableVaultBackupTest {
    private val passphrase = "a-long-passphrase-for-testing".toCharArray()

    @Test fun roundTripUsesFreshSaltAndIv() {
        val payload = "sample vault payload".toByteArray()
        val first = PortableVaultBackup.encryptPayload(payload, passphrase)
        val second = PortableVaultBackup.encryptPayload(payload, passphrase)
        assertTrue(PortableVaultBackup.isPortable(first))
        assertFalse(first.contentEquals(second))
        assertArrayEquals(payload, PortableVaultBackup.decryptPayload(first, passphrase))
    }

    @Test(expected = javax.crypto.AEADBadTagException::class)
    fun wrongPassphraseFailsAuthentication() {
        val encoded = PortableVaultBackup.encryptPayload("private".toByteArray(), passphrase)
        PortableVaultBackup.decryptPayload(encoded, "a-different-long-passphrase".toCharArray())
    }

    @Test(expected = javax.crypto.AEADBadTagException::class)
    fun modifiedCiphertextFailsAuthentication() {
        val encoded = PortableVaultBackup.encryptPayload("private".toByteArray(), passphrase)
        encoded[encoded.lastIndex] = (encoded.last().toInt() xor 1).toByte()
        PortableVaultBackup.decryptPayload(encoded, passphrase)
    }
}

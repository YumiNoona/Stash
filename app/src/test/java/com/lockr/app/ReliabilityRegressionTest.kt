package com.lockr.app

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream

class ReliabilityRegressionTest {
    @Test fun exactSizeAndEmptyInputsRemainReadable() {
        assertArrayEquals(byteArrayOf(1, 2, 3), readBoundedBytes(ByteArrayInputStream(byteArrayOf(1, 2, 3)), 3))
        assertArrayEquals(byteArrayOf(), readBoundedBytes(ByteArrayInputStream(byteArrayOf()), 3))
    }
    @Test fun oversizedStreamsStopBeforeBeingReadIntoMemory() {
        var read = 0
        val input = object : InputStream() {
            override fun read(): Int { read++; return 1 }
            override fun read(buffer: ByteArray, offset: Int, length: Int): Int { read += length; return length }
        }
        try { readBoundedBytes(input, 3); fail("Reject oversized input") }
        catch (_: IOException) { }
        assertTrue(read <= 16 * 1024)
    }
    @Test fun zeroByteBulkReadsStillMakeProgress() {
        val input = object : ByteArrayInputStream(byteArrayOf(4, 5, 6)) {
            var first = true
            override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                if (first) { first = false; return 0 }
                return super.read(buffer, offset, length)
            }
        }
        assertArrayEquals(byteArrayOf(4, 5, 6), readBoundedBytes(input, 3))
    }
    @Test fun temporaryBufferIsClearedEvenWhenReadFails() {
        var temporary: ByteArray? = null
        val input = object : InputStream() {
            override fun read(): Int = throw IOException("Read failed")
            override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                temporary = buffer; buffer.fill(7); throw IOException("Read failed")
            }
        }
        try { readBoundedBytes(input, 10); fail("Propagate read failure") } catch (_: IOException) { }
        assertTrue(temporary!!.all { it == 0.toByte() })
    }
    @Test(expected = IllegalArgumentException::class) fun invalidReadLimitIsRejected() {
        readBoundedBytes(ByteArrayInputStream(byteArrayOf()), 0)
    }
    @Test fun rotatedImagesFitTheOrientedPreview() {
        assertEquals(4, imageSampleSize(4000, 2000, 1000, 500))
        assertEquals(8, imageSampleSize(4000, 2000, 1000, 500, 6))
        for (orientation in 1..8) {
            val sample = imageSampleSize(6000, 4000, 900, 1200, orientation)
            val rotated = orientation in 5..8
            assertTrue((if (rotated) 4000 else 6000) / sample <= 900)
            assertTrue((if (rotated) 6000 else 4000) / sample <= 1200)
        }
    }
    @Test fun smallImagesKeepTheirResolution() {
        assertEquals(1, imageSampleSize(640, 480, 1024, 1024))
    }
    @Test(expected = IllegalArgumentException::class) fun hugePixelCountsAreRejectedBeforeDecoding() {
        imageSampleSize(16000, 16000, 1024, 1024)
    }
    @Test(expected = IllegalArgumentException::class) fun invalidPreviewBoundsAreRejected() {
        imageSampleSize(100, 100, 0, 1024)
    }
    @Test fun copyingTheSameTextAgainGetsItsOwnExpiry() {
        var currentToken: String? = "first-copy"
        var cleared = 0
        val timers = mutableListOf<() -> Unit>()
        fun copied(token: String) {
            currentToken = token
            scheduleClipboardClear(token, { currentToken }, { currentToken = null; cleared++ }, { delay, action ->
                assertEquals(30_000L, delay); timers += action
            })
        }
        copied("first-copy"); copied("second-copy")
        timers[0](); assertEquals("second-copy", currentToken); assertEquals(0, cleared)
        timers[1](); assertNull(currentToken); assertEquals(1, cleared)
    }
    @Test fun aLaterCopyWithoutAutoClearAndExternalCopiesArePreserved() {
        var currentToken: String? = "auto-clear-copy"
        var cleared = false
        var timer: (() -> Unit)? = null
        scheduleClipboardClear("auto-clear-copy", { currentToken }, { cleared = true }, { _, action -> timer = action })
        currentToken = "new-copy-without-expiry"; timer!!(); assertFalse(cleared)
        currentToken = null; timer!!(); assertFalse(cleared)
    }
}

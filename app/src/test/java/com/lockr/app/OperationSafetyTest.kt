package com.lockr.app

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream

class OperationSafetyTest {
    private val card = VaultItem("QA card", VaultCategory.CARD, "Credit", "", lastFour = "1111", metadata = mapOf(
        "cardNumber" to "4111111111111111", "cardCvc" to "987", "issuer" to "QA bank", "cardholder" to "TEST HOLDER"))
    @Test fun cardCopiesDefaultToMaskedAndNeverIncludeVerificationCodes() {
        val public = cardExportText(card, false)
        assertFalse(public.contains("4111111111111111")); assertTrue(public.contains("1111"))
        assertTrue(cardExportText(card, true).contains("4111111111111111"))
        assertFalse(public.contains("987")); assertFalse(cardExportText(card, true).contains("987"))
    }
    @Test fun privateExportFilteringIsCaseInsensitive() {
        val fields = card.copy(metadata = mapOf("CARDNUMBER" to "secret", "Password" to "secret", "CARDcvc" to "secret"))
        assertTrue(visibleMetadata(fields, false).isEmpty())
        assertFalse(visibleMetadata(fields, true).containsKey("CARDcvc"))
    }
    @Test fun failedDestinationWritesCloseTheStreamAndPropagateTheError() {
        var closed = false
        val stream = object : OutputStream() {
            override fun write(value: Int) { throw IOException("Storage full") }
            override fun close() { closed = true }
        }
        try { writeExportDestination({ stream }) { it.write(1) }; fail("Expected storage error") }
        catch (failure: IOException) { assertEquals("Storage full", failure.message) }
        assertTrue(closed)
    }
    @Test(expected = IOException::class) fun nullDestinationIsAnError() { writeExportDestination({ null }) { fail("Do not write") } }
    @Test fun successfulDestinationWritesTheCompleteCopy() {
        val stream = ByteArrayOutputStream()
        writeExportDestination({ stream }) { it.write("Saved".toByteArray()) }
        assertEquals("Saved", stream.toString("UTF-8"))
    }
    @Test fun staleImportsCannotApplyAfterDiscardOrANewerImport() {
        val gate = DraftImportGate(); val first = gate.begin()
        gate.invalidate(); assertFalse(gate.current(first))
        val second = gate.begin(); val third = gate.begin()
        assertFalse(gate.current(second)); assertTrue(gate.current(third))
    }
    @Test fun cameraAndFileImportsShareTheRemainingSlotLimit() {
        assertEquals(1, remainingDocumentSlots(19)); assertEquals(0, remainingDocumentSlots(20)); assertEquals(0, remainingDocumentSlots(37))
        val page = VaultAttachment("page.png", "image/png", 1, "")
        try { validateDocumentPages(List(21) { page }); fail("Reject a 21st file") } catch (_: IllegalArgumentException) { }
        try { validateDocumentPages(listOf(page.copy(byteCount = MAX_VAULT_TOTAL_ATTACHMENT_BYTES), page)); fail("Reject combined oversized document") } catch (_: IllegalArgumentException) { }
    }
    @Test fun allFolderCreationFlowsUseTheSameRules() {
        assertNotNull(folderNameError("ALL FOLDERS", emptyList()))
        assertNotNull(folderNameError("unfiled", emptyList()))
        assertNotNull(folderNameError(" work ", listOf("Work")))
        assertNull(folderNameError("work", listOf("Work"), "Work"))
        assertEquals("School work", normalizedFolderName("  School   work "))
        assertNotNull(folderNameError("bad\u0001name", emptyList()))
    }
    @Test fun hugeBackgroundsAreSampledBeforePixelsAreAllocated() {
        val sample = backdropSampleSize(15_000, 10_000)
        assertTrue(15_000 / sample <= 1600); assertTrue(10_000 / sample <= 1600)
        assertEquals(1, backdropSampleSize(800, 1200))
    }
    @Test(expected = IllegalArgumentException::class) fun rejectUnboundedBackgroundDimensions() { backdropSampleSize(40_000, 40_000) }
    @Test fun themePreviewsUseTheChosenTarget() {
        assertTrue(backdropTargetDark("Dark", false)); assertFalse(backdropTargetDark("Light", true))
        assertTrue(backdropTargetDark("Both", true)); assertFalse(backdropTargetDark("Both", false))
    }
    @Test fun slantedDocumentEdgesUsePixelDistances() {
        val points = listOf(CropCorner(.1f,.1f), CropCorner(.8f,.3f), CropCorner(.9f,.9f), CropCorner(.2f,.7f))
        assertEquals(806 to 1204, correctedDocumentSize(points, 1000, 2000))
    }
    @Test(expected = IllegalArgumentException::class) fun rejectCrossedCropEdges() {
        correctedDocumentSize(listOf(CropCorner(.1f,.1f), CropCorner(.9f,.9f), CropCorner(.9f,.1f), CropCorner(.1f,.9f)), 1000, 2000)
    }
    @Test(expected = IllegalArgumentException::class) fun rejectDegenerateCropEdges() {
        correctedDocumentSize(List(4) { CropCorner(.5f,.5f) }, 1000, 2000)
    }
    @Test fun redactionRetainsEveryImageAndPdfPageInOrder() {
        assertEquals(listOf(RedactionPage(0,0), RedactionPage(1,0), RedactionPage(2,0)), documentRedactionPlan(listOf(1,1,1)))
        assertEquals(listOf(RedactionPage(0,0), RedactionPage(0,1), RedactionPage(1,0)), documentRedactionPlan(listOf(2,1)))
    }
    @Test(expected = IllegalArgumentException::class) fun unreadableRedactionPagesAreNeverSilentlyDropped() { documentRedactionPlan(listOf(1,0,1)) }
}

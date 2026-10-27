package com.lockr.app

import org.junit.Assert.*
import org.junit.Test

class CardCustomizationTest {
    private val scan = VaultAttachment("scan.jpg", "image/jpeg", 100, "scan")
    private val oldArt = VaultAttachment("art.gif", "image/gif", 200, "old")
    private val newArt = VaultAttachment("art.mp4", "video/mp4", 300, "new")

    @Test fun replacingArtRetainsOriginalScan() {
        assertEquals(listOf(scan, newArt), withCardArt(listOf(scan, oldArt), oldArt.name, newArt))
    }

    @Test fun choosingBuiltInArtRemovesOnlyCustomArt() {
        assertEquals(listOf(scan), withCardArt(listOf(scan, oldArt), oldArt.name, null))
    }

    @Test fun newArtDoesNotTreatAnOriginalScanAsArtwork() {
        assertEquals(listOf(scan, newArt), withCardArt(listOf(scan), null, newArt))
    }

    @Test fun artworkLookupUsesSavedReferenceRatherThanFirstAttachment() {
        val item = VaultItem("Card", VaultCategory.CARD, "", "", attachments = listOf(scan, oldArt),
            metadata = mapOf("cardArtAttachment" to oldArt.name))
        assertEquals(oldArt, customCardArt(item))
        assertNull(customCardArt(item.copy(metadata = emptyMap())))
    }

    @Test fun olderCardsHaveNoMotionUnlessSelected() {
        val item = VaultItem("Card", VaultCategory.CARD, "", "", metadata = mapOf("cardArt" to "MIDNIGHT"))
        assertEquals(CardMotion.NONE, cardMotion(item))
        assertEquals(CardMotion.FLOAT, cardMotion(item.copy(metadata = item.metadata + ("cardMotion" to "FLOAT"))))
        assertEquals(CardMotion.NONE, cardMotion(item.copy(metadata = item.metadata + ("cardMotion" to "unknown"))))
    }
}

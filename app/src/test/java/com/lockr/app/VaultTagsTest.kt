package com.lockr.app

import org.junit.Assert.*
import org.junit.Test

class VaultTagsTest {
    @Test fun tagEncodingNormalizesAndDeduplicates() {
        val encoded = encodeTags(setOf("  Travel   cards ", "Travel cards", "Work", ""))
        val item = VaultItem("Card", VaultCategory.CARD, "", "", metadata = mapOf("tags" to encoded))
        assertEquals(setOf("Travel cards", "Work"), cardTags(item))
    }

    @Test fun removingCardTagPreservesOtherTagsAndPrivateFields() {
        val item = VaultItem("Card", VaultCategory.CARD, "", "", metadata = mapOf(
            "tags" to encodeTags(setOf("Travel", "Work")), "cardNumber" to "4111111111111111", "cardArt" to "MIDNIGHT"))
        val updated = withoutTag(item, VaultCategory.CARD, "travel")
        assertEquals(setOf("Work"), cardTags(updated))
        assertEquals(item.metadata["cardNumber"], updated.metadata["cardNumber"])
        assertEquals(item.id, updated.id)
        assertEquals(item.createdAt, updated.createdAt)
    }

    @Test fun deletingDocumentTagReassignsGroupWithoutDeletingDocument() {
        val document = VaultItem("Receipt", VaultCategory.DOCUMENT, "", "", metadata = mapOf("documentCategory" to "Travel"))
        assertEquals("Other", withoutTag(document, VaultCategory.DOCUMENT, "Travel").metadata["documentCategory"])
        assertEquals(document, withoutTag(document, VaultCategory.CARD, "Travel"))
    }

    @Test fun cardTitlesWorkWithoutNicknameField() {
        assertEquals("Amazon Pay · 1111", cardDisplayTitle("ICICI", "Amazon Pay", "Visa", "Credit", "4111111111111111"))
        assertEquals("ICICI", cardDisplayTitle("ICICI", "", "Visa", "Credit", ""))
        assertEquals("Card", cardDisplayTitle("", "", "", "", ""))
    }

    @Test fun eachFinishHasItsOwnAssetAndTenEffectsAreAvailable() {
        assertEquals(15, selectableCardArt.size)
        assertEquals(15, selectableCardArt.map { it.image }.distinct().size)
        assertEquals(10, CardMotion.entries.count { it != CardMotion.NONE })
    }
}

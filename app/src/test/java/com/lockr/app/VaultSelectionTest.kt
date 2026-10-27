package com.lockr.app

import org.junit.Assert.*
import org.junit.Test

class VaultSelectionTest {
    @Test fun enteringStartsEmptyAndSingleTapOnlyTogglesOneRecord() {
        val selection = VaultSelection().enter()
        assertTrue(selection.active); assertTrue(selection.ids.isEmpty())
        assertEquals(setOf("first"), selection.toggle("first").ids)
        val deselected = selection.toggle("first").toggle("first")
        assertTrue(deselected.active); assertTrue(deselected.ids.isEmpty())
    }
    @Test fun selectAllIsExplicitAndTracksOnlyTheVisibleCollection() {
        val visible = setOf("first", "second")
        val selected = VaultSelection().toggle("first").all(visible)
        assertEquals(visible, selected.ids)
        assertTrue(selected.all(visible).ids.isEmpty())
        assertEquals(setOf("first"), selected.reconcile(setOf("first")).ids)
    }
}

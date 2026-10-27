package com.lockr.app

import org.junit.Assert.*
import org.junit.Test

class DocumentOrganizationTest {
    @Test fun duplicateNamesPreserveExtensionsAndSkipOccupiedSuffixes() {
        assertEquals("Homework (3).pdf", uniqueVaultTitle("Homework.pdf", listOf("HOMEWORK.PDF", "Homework (1).pdf", "Homework (2).pdf")))
        assertEquals("report (2).png", uniqueVaultTitle("report (1).png", listOf("report.png", "report (1).png")))
        assertEquals("Notes (1)", uniqueVaultTitle("Notes", listOf("Notes")))
    }
    @Test fun movingDocumentsDoesNotOverwriteAnotherFolderOrRenameItself() {
        val source = VaultItem("notes.pdf", VaultCategory.DOCUMENT, "", "", metadata = mapOf("folder" to "School"))
        val target = source.copy(id = "another", metadata = mapOf("folder" to "Personal"))
        assertEquals("notes.pdf", uniqueItem(source, listOf(source, target)).title)
        val moved = uniqueItem(source.copy(metadata = mapOf("folder" to "Personal")), listOf(source, target))
        assertEquals("notes (1).pdf", moved.title)
        assertEquals(source.id, moved.id)
        assertEquals(source.createdAt, moved.createdAt)
    }
    @Test fun multipageImagesExportAsOnePdfAndMixedDocumentsAsZip() {
        val image = VaultAttachment("page.png", "image/png", 1, "")
        val item = VaultItem("School work", VaultCategory.DOCUMENT, "", "", attachments = listOf(image, image.copy(name="page2.png")))
        assertEquals("School work.pdf", documentDownloadName(listOf(item)))
        assertEquals("page.png", documentDownloadName(listOf(item.copy(attachments = listOf(image)))))
        assertEquals("Stash-documents.zip", documentDownloadName(listOf(item, item.copy(id="another"))))
    }
}

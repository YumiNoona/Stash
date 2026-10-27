package com.lockr.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentFieldExtractionTest {
    @Test fun aadhaarNumberAndMultilineAddressAreSeparateFields() {
        val text = """
            Government of India
            Aadhaar
            Name: Asha Rao
            DOB: 04/05/1997
            2345 6789 1234
            Address: D/O Meera Rao, Flat 7
            Lake Road, Bengaluru
            Karnataka 560001
            VID: 1234 5678 9012 3456
        """.trimIndent()

        val fields = suggestDocumentFields(text).associateBy { it.key }
        assertEquals("2345 6789 1234", fields["idNumber"]?.value)
        assertEquals("D/O Meera Rao, Flat 7\nLake Road, Bengaluru\nKarnataka 560001", fields["address"]?.value)
        assertEquals("Asha Rao", fields["holderName"]?.value)
        assertEquals("04/05/1997", fields["dateOfBirth"]?.value)
        assertFalse(fields["address"]!!.value.contains("VID"))
    }

    @Test fun doesNotTreatUnrelatedLongNumberAsAadhaar() {
        val fields = suggestDocumentFields("Invoice number 2345 6789 1234\nTotal due 500")
        assertFalse(fields.any { it.key == "idNumber" })
    }

    @Test fun recognizesIdentityHeaderWhenAadhaarLabelIsMissedByOcr() {
        val fields = suggestDocumentFields("Government of India\n2345 6789 1234")
        assertEquals("2345 6789 1234", fields.firstOrNull { it.key == "idNumber" }?.value)
    }

    @Test fun aadhaarNumberIsRemovedFromSearchableText() {
        val redacted = redactAadhaarNumbers("UIDAI\n2345 6789 1234\nAddress: Hill Road")
        assertFalse(redacted.contains("2345"))
        assertTrue(redacted.contains("Address: Hill Road"))
        assertEquals("Invoice 2345 6789 1234", redactAadhaarNumbers("Invoice 2345 6789 1234"))
    }
}

package com.lockr.app

import org.junit.Assert.*
import org.junit.Test

class IdentityCardsTest {
    @Test fun changingIdentityTypeKeepsNamesButDoesNotReuseAnotherIssuersNumber() {
        val fields=newIdentityTypeFields(IdentityType.LICENCE,mapOf("cardholder" to "Test","idNumber" to "ABCDE1234F","issuer" to "Income Tax Department","vehicleClasses" to "Old"))
        assertEquals("Test",fields["cardholder"]);assertEquals("Transport Department",fields["issuer"])
        assertNull(fields["idNumber"]);assertNull(fields["vehicleClasses"])
    }
    @Test fun panRecognitionKeepsAlphanumericNumberAndLabelledNames() {
        val value=recognizeIdentity("INCOME TAX DEPARTMENT\nABCDE1234F\nName: TEST PERSON\nFather's Name: TEST PARENT\nDOB: 12/08/1994")!!
        assertEquals(IdentityType.PAN,value.type)
        assertEquals("ABCDE1234F",value.fields["idNumber"])
        assertEquals("TEST PERSON",value.fields["cardholder"])
        assertEquals("TEST PARENT",value.fields["relativeName"])
        assertEquals("12/08/1994",value.fields["dateOfBirth"])
    }
    @Test fun aadhaarIsNotMistakenForPaymentCard() {
        val value=recognizeIdentity("UIDAI AADHAAR\n2345 6789 0123\nName: TEST PERSON\nDOB: 1994\nFEMALE")!!
        assertEquals(IdentityType.AADHAAR,value.type)
        assertEquals("234567890123",value.fields["idNumber"])
        assertEquals("Female",value.fields["gender"])
        assertEquals("1994",value.fields["dateOfBirth"])
    }
    @Test fun electionAndLicenceHaveSeparateSchemas() {
        assertEquals("ABC1234567",recognizeIdentity("ELECTION COMMISSION OF INDIA\nABC1234567")!!.fields["idNumber"])
        assertEquals("MH1220241234567",recognizeIdentity("DRIVING LICENCE\nDL No: MH1220241234567")!!.fields["idNumber"])
        assertTrue(identityFields(IdentityType.LICENCE).any {it.key=="transportValidTill"})
        assertFalse(identityFields(IdentityType.PAN).any {it.key=="validTill"})
    }
    @Test fun genericPaymentTextDoesNotSuggestIdentity() {
        assertNull(recognizeIdentity("VISA\n4111 1111 1111 1111\n12/30"))
    }
    @Test fun recognitionDoesNotInventUnlabelledNames() {
        assertNull(recognizeIdentity("AADHAAR\nTEST PERSON\n234567890123")!!.fields["cardholder"])
    }
    @Test fun validateEachNumberWithoutTreatingItAsPaymentNumber() {
        assertNull(identityNumberError(IdentityType.PAN,"abcde1234f"))
        assertNotNull(identityNumberError(IdentityType.PAN,"ABCDE12345"))
        assertNotNull(identityNumberError(IdentityType.AADHAAR,"123456789012"))
        assertNull(identityNumberError(IdentityType.AADHAAR,"2345 6789 0123"))
        assertNull(identityNumberError(IdentityType.VOTER,"ABC1234567"))
        assertNull(identityNumberError(IdentityType.LICENCE,"MH12-20241234567"))
    }
    @Test fun fullDatesAndYearOnlyAreAllowedButImpossibleDatesAreRejected() {
        assertNull(identityDateError("29/02/2024"));assertNull(identityDateError("1994"));assertNull(identityDateError(""))
        assertNotNull(identityDateError("31/02/2024"));assertNotNull(identityDateError("12/30"))
    }
    @Test fun switchingTypeDropsIrrelevantAndLegacyPaymentFields() {
        val meta=identityMetadata(IdentityType.PAN,mapOf("cardholder" to "Test","idNumber" to "abcde1234f","cardCvc" to "123","address" to "Old address","validTill" to "12/12/2030"),emptyMap(),emptySet())
        assertEquals("ABCDE1234F",meta["cardNumber"]);assertEquals("234F",meta["lastFour"])
        assertFalse(meta.containsKey("cardCvc"));assertEquals("Old address",meta["address"]);assertFalse(meta.containsKey("validTill"))
        assertEquals("ID Card",meta["cardType"])
    }
    @Test fun identityAssetsHaveExplicitRolesAndOriginalsStaySeparate() {
        val front=VaultAttachment("front.png","image/png",1,"front")
        val photo=VaultAttachment("photo.png","image/png",1,"photo")
        val meta=identityMetadata(IdentityType.PAN,mapOf("cardholder" to "Test","idNumber" to "ABCDE1234F"),mapOf(IdentityAsset.PHOTO to photo),emptySet())
        val item=VaultItem("Test",VaultCategory.CARD,"ID Card","",metadata=meta,attachments=listOf(front,photo))
        assertEquals(photo,identityAsset(item,IdentityAsset.PHOTO));assertNull(identityAsset(item,IdentityAsset.QR))
        assertEquals(listOf(front),identityOriginals(item))
    }
    @Test fun identityExportKeepsPrivateFieldsOutByDefault() {
        val meta=identityMetadata(IdentityType.AADHAAR,mapOf("cardholder" to "Test","idNumber" to "234567890123","address" to "Private address","dateOfBirth" to "12/08/1994"),emptyMap(),emptySet())
        val item=VaultItem("Aadhaar",VaultCategory.CARD,"ID Card","",metadata=meta)
        val visible=visibleMetadata(item,false)
        assertFalse(visible.containsKey("cardNumber"));assertFalse(visible.containsKey("idNumber"));assertFalse(visible.containsKey("dateOfBirth"));assertFalse(visible.containsKey("address"))
        val public=cardExportText(item,false)
        assertFalse(public.contains("234567890123"));assertFalse(public.contains("Private address"))
        assertTrue(public.contains("0123"));assertTrue(cardExportText(item,true).contains("2345 6789 0123"))
    }
    @Test fun templateNumberMaskKeepsOnlyLastFour() {
        assertEquals("••••  234F",identityNumberText(IdentityType.PAN,"ABCDE1234F",false))
        assertEquals("2345 6789 0123",identityNumberText(IdentityType.AADHAAR,"234567890123",true))
    }
    @Test fun cropGeometryUsesSourcePixelsAndRejectsTinyOrCrossedAreas() {
        assertEquals(AssetCrop(100,400,700,1200),identityCropBounds(.1f,.2f,.8f,.8f,1000,2000))
        assertThrows(IllegalArgumentException::class.java) {identityCropBounds(.8f,.2f,.1f,.8f,1000,2000)}
        assertThrows(IllegalArgumentException::class.java) {identityCropBounds(.1f,.2f,.11f,.21f,1000,2000)}
    }
}

package com.lockr.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordGeneratorTest {
    @Test
    fun respectsLengthAndGuaranteesSelectedTypesWhenMixed() {
        repeat(50) {
            val password = generateRandomPassword(PasswordGeneratorOptions(length = 32))
            assertEquals(32, password.length)
            assertTrue(password.any(Char::isLowerCase))
            assertTrue(password.any(Char::isUpperCase))
            assertTrue(password.any(Char::isDigit))
            assertTrue(password.any { !it.isLetterOrDigit() })
        }
    }

    @Test
    fun respectsSingleTypeAndLookalikePreference() {
        val password = generateRandomPassword(PasswordGeneratorOptions(
            length = 64, lowercase = false, uppercase = false, numbers = true,
            symbols = false, avoidLookalikes = true))
        assertEquals(64, password.length)
        assertTrue(password.all { it in '2'..'9' })
        assertFalse(password.any { it == '0' || it == '1' })
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNoCharacterTypes() {
        generateRandomPassword(PasswordGeneratorOptions(
            lowercase = false, uppercase = false, numbers = false, symbols = false))
    }
}

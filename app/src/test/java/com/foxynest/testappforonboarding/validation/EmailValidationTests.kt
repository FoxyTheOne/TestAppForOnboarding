package com.foxynest.testappforonboarding.validation

import com.foxynest.testappforonboarding.other.utils.EmailUtils
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test

class EmailValidationTests {
    @Test
    fun `email validation returns true for valid email`() {
        assertTrue(
            "Валидный email должен проходить проверку",
            EmailUtils.emailMatchesForTest("test@example.com")
        )
    }

    @Test
    fun `email validation returns false for invalid email`() {
        assertFalse(
            "Email без домена должен не проходить проверку",
            EmailUtils.emailMatchesForTest("invalid-email")
        )
    }

    @Test
    fun `email without at symbol should return false`() {
        assertFalse(
            "Email без @ должен не проходить проверку",
            EmailUtils.emailMatchesForTest("test.example.com")
        )
    }

    @Test
    fun `email validation returns false for empty string`() {
        assertFalse(
            "Пустая строка должна не проходить проверку",
            EmailUtils.emailMatchesForTest("")
        )
    }
}
package com.foxynest.testappforonboarding.validation

import com.foxynest.testappforonboarding.other.utils.EmailUtils
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test

/** Проверка формата почты (EmailUtils – тот же код, что работает в приложении). */
class EmailValidationTests {
    @Test
    fun `email validation returns true for valid email`() {
        assertTrue(
            "Валидный email должен проходить проверку",
            EmailUtils.emailMatches("test@example.com")
        )
    }

    @Test
    fun `email validation returns false for invalid email`() {
        assertFalse(
            "Email без домена должен не проходить проверку",
            EmailUtils.emailMatches("invalid-email")
        )
    }

    @Test
    fun `email without at symbol should return false`() {
        assertFalse(
            "Email без @ должен не проходить проверку",
            EmailUtils.emailMatches("test.example.com")
        )
    }

    @Test
    fun `email validation returns false for empty string`() {
        assertFalse(
            "Пустая строка должна не проходить проверку",
            EmailUtils.emailMatches("")
        )
    }
}
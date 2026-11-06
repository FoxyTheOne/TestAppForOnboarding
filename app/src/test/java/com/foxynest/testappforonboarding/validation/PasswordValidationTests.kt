package com.foxynest.testappforonboarding.validation

import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.register.RegisterValidatePasswordUC
import junit.framework.TestCase.assertTrue
import org.junit.Test

/** Пример правильного пароля для регистрации. */
class PasswordValidationTests {
    @Test
    fun `password validation succeeds for valid password`() {
        val useCase = RegisterValidatePasswordUC()
        val result = useCase.execute("Valid123")

        assertTrue(result.successful)
    }
}
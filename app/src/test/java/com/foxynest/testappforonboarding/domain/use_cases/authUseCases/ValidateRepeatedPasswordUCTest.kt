package com.foxynest.testappforonboarding.domain.use_cases.authUseCases

import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.register.RegisterValidateRepeatedPasswordUC
import junit.framework.TestCase.assertEquals
import org.junit.Before
import org.junit.Test

class ValidateRepeatedPasswordUCTest {
    private lateinit var validateRepeatedPasswordUseCase: RegisterValidateRepeatedPasswordUC

    @Before
    fun setUp() {
        validateRepeatedPasswordUseCase = RegisterValidateRepeatedPasswordUC()
    }

    @Test
    fun `Passwords are not equal, returns error`(){
        val result = validateRepeatedPasswordUseCase.execute("qwertyui", "123")

        assertEquals(false, result.successful)
    }

    @Test
    fun `Passwords are equal, returns success`(){
        val result = validateRepeatedPasswordUseCase.execute("12345678q", "12345678q")

        assertEquals(true, result.successful)
    }
}
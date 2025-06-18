package com.foxynest.testappforonboarding.domain.use_cases.authUseCases

import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.register.ValidatePasswordUC
import com.foxynest.testappforonboarding.other.constants.AuthConstants
import junit.framework.TestCase.assertEquals
import org.junit.Before
import org.junit.Test
import kotlin.math.pow
import kotlin.random.Random

class ValidatePasswordUCTest {
    private lateinit var validatePasswordUseCase: ValidatePasswordUC

    @Before
    fun setUp() {
        validatePasswordUseCase = ValidatePasswordUC()
    }

    @Test
    fun `Password is small, returns error`() {
        val numberOfDigits = AuthConstants.MIN_PASSWORD_LENGTH - 2
        val randomNumber = generateRandomNumber(numberOfDigits)
        println("Случайное число из $numberOfDigits цифр: $randomNumber")

        val result = validatePasswordUseCase.execute("q$randomNumber")

        assertEquals(false, result.successful)
    }

    @Test
    fun `Password is letter only, returns error`() {
        val result = validatePasswordUseCase.execute("qwertyuqqqqqqqqi")

        assertEquals(false, result.successful)
    }

    @Test
    fun `Password is number only, returns error`() {
        val result = validatePasswordUseCase.execute("12345678910")

        assertEquals(false, result.successful)
    }

    private fun generateRandomNumber(digits: Int): Int {
        require(digits > 0) { "Количество цифр должно быть больше 0" }
        val min = 10.0.pow(digits - 1).toInt()
        val max = 10.0.pow(digits).toInt() - 1
        return Random.nextInt(min, max + 1)
    }
}
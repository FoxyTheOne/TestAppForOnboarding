package com.foxynest.testappforonboarding.domain.use_cases.authUseCases

import com.foxynest.testappforonboarding.domain.entities.ErrorType
import com.foxynest.testappforonboarding.domain.entities.user.UserPresentation
import com.foxynest.testappforonboarding.domain.iRepository.IAuthRepository
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.register.RegisterUC
import com.foxynest.testappforonboarding.other.Resource
import com.foxynest.testappforonboarding.other.Status
import com.foxynest.testappforonboarding.other.utils.EmailUtils
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RegisterUCTests {
    // Раньше registerUC был @Inject lateinit без Hilt – тест падал с UninitializedPropertyAccessException
    private val authRepository = mockk<IAuthRepository>()
    private val registerUC = RegisterUC(authRepository)

    @Test
    fun `register with valid data returns success`() = runTest {
        val user = UserPresentation(uid = "123", authEmail = "test@test.com")
        coEvery { authRepository.register(any(), any(), any()) } returns Resource.success(null)

        val result = registerUC.execute("test@test.com", "password123", user)

        assertEquals(Status.SUCCESS, result.status)
    }

    @Test
    fun `register with empty password does not call repository`() = runTest {
        val result = registerUC.execute("test@test.com", "", UserPresentation())

        assertEquals(ErrorType.FIELD_EMPTY_ERROR, result.data)
        coVerify(exactly = 0) { authRepository.register(any(), any(), any()) }
    }

    @Test
    fun `email validation`() {
        assertTrue(EmailUtils.emailMatches("test@example.com"))
        assertFalse(EmailUtils.emailMatches("invalid-email"))
    }
}
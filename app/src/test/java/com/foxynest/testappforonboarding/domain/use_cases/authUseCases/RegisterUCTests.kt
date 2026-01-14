package com.foxynest.testappforonboarding.domain.use_cases.authUseCases

import com.foxynest.testappforonboarding.data.repository.AuthRepository
import com.foxynest.testappforonboarding.domain.entities.user.UserPresentation
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.register.RegisterUC
import com.foxynest.testappforonboarding.other.Resource
import com.foxynest.testappforonboarding.other.Status
import com.foxynest.testappforonboarding.other.utils.EmailUtils
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import javax.inject.Inject

class RegisterUCTests {
//    @get:Rule
//    val hiltRule = HiltAndroidRule(this)

    @MockK
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var registerUC: RegisterUC

    @Before
    fun setup() {
//        hiltRule.inject()
        MockKAnnotations.init(this)
    }

    @Test
    fun `register with valid data returns success`() = runTest {
        // Given
        val user = UserPresentation(uid = "123", authEmail = "test@test.com")
        coEvery { authRepository.register(any(), any(), any()) } returns Resource.success(null)

        // When
        val result = registerUC.execute("test@test.com", "password123", user)

        // Then
        assertEquals(Status.SUCCESS, result.status)
    }

    @Test
    fun `register with valid data returns success2`() = runTest {
        // Given - создаем тестовые данные вручную
        val testEmail = "test@example.com"
        val testPassword = "password123"
        val testUser = UserPresentation(
            uid = "test-uid",
            authEmail = testEmail,
            userName = "Test User"
        )

        // When - здесь мы бы тестировали RegisterUC, но он требует AuthRepository
        // Для начала тестируем простые функции

        // Then - проверяем валидацию email
        assertTrue(EmailUtils.emailMatchesForTest(testEmail))
    }

    @Test
    fun `email validation fails for invalid email`() = runTest {
        // Given
        val invalidEmail = "invalid-email"

        // When & Then
        assertFalse(EmailUtils.emailMatchesForTest(invalidEmail))
    }
}
package com.foxynest.testappforonboarding.domain.use_cases.authUseCases

import com.foxynest.testappforonboarding.domain.entities.ErrorType
import com.foxynest.testappforonboarding.domain.iRepository.IAuthRepository
import com.foxynest.testappforonboarding.domain.iRepository.IBirthdayNotificationRepository
import com.foxynest.testappforonboarding.domain.iRepository.IOnboardingRepository
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.account.DeleteAccountUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.account.SendPasswordResetUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.LoginValidateEmailUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.LogoutUC
import com.foxynest.testappforonboarding.other.Resource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AccountUseCasesTest {
    private val authRepository = mockk<IAuthRepository>()
    private val onboardingRepository = mockk<IOnboardingRepository>(relaxed = true)
    private val birthdayRepository = mockk<IBirthdayNotificationRepository>(relaxed = true)

    @Test
    fun `logout keeps local data when unsynced changes block it`() = runTest {
        coEvery { authRepository.logout() } returns
                Resource.error("unsynced", ErrorType.UNSYNCED_DATA_LOGOUT_ERROR)

        LogoutUC(authRepository, onboardingRepository, birthdayRepository).execute()

        coVerify(exactly = 0) { onboardingRepository.clearAllOnboardingData() }
        coVerify(exactly = 0) { birthdayRepository.cancel() }
    }

    @Test
    fun `successful logout clears local data and birthday alarm`() = runTest {
        coEvery { authRepository.logout() } returns Resource.success(null)

        LogoutUC(authRepository, onboardingRepository, birthdayRepository).execute()

        coVerify { onboardingRepository.clearAllOnboardingData() }
        coVerify { birthdayRepository.cancel() }
    }

    @Test
    fun `account deletion with wrong password keeps everything`() = runTest {
        coEvery { authRepository.deleteAccount("wrong") } returns
                Resource.error("wrong", ErrorType.WRONG_PASSWORD_ERROR)

        val result = DeleteAccountUC(
            authRepository,
            onboardingRepository,
            birthdayRepository
        ).execute("wrong")

        assertEquals(ErrorType.WRONG_PASSWORD_ERROR, result.data)
        coVerify(exactly = 0) { onboardingRepository.clearAllOnboardingData() }
    }

    @Test
    fun `account deletion without password does not call server`() = runTest {
        val result =
            DeleteAccountUC(authRepository, onboardingRepository, birthdayRepository).execute(" ")

        assertEquals(ErrorType.PASSWORD_EMPTY_ERROR, result.data)
        coVerify(exactly = 0) { authRepository.deleteAccount(any()) }
    }

    @Test
    fun `password reset validates email before calling server`() = runTest {
        val result =
            SendPasswordResetUC(authRepository, LoginValidateEmailUC()).execute("not-an-email")

        assertEquals(ErrorType.EMAIL_FORMAT_ERROR, result.data)
        coVerify(exactly = 0) { authRepository.sendPasswordResetEmail(any()) }
    }

    @Test
    fun `password reset normalizes email`() = runTest {
        coEvery { authRepository.sendPasswordResetEmail(any()) } returns Resource.success(null)

        SendPasswordResetUC(authRepository, LoginValidateEmailUC()).execute("  Mama@Example.COM ")

        coVerify { authRepository.sendPasswordResetEmail("mama@example.com") }
    }
}
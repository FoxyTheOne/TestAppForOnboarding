package com.foxynest.testappforonboarding

import com.foxynest.testappforonboarding.domain.entities.language.LanguageEnum
import com.foxynest.testappforonboarding.domain.use_cases.appLanguage.IGetAppLanguageUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.email.ICheckEmailVerifiedUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.email.ISendEmailVerificationUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.ILogoutUC
import com.foxynest.testappforonboarding.other.Resource
import com.foxynest.testappforonboarding.other.Status
import com.foxynest.testappforonboarding.presentation.profile.ProfileViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ProfileViewModelTests {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val sendEmailUC = mockk<ISendEmailVerificationUC>()
    private val checkEmailUC = mockk<ICheckEmailVerifiedUC>()
    private val logoutUC = mockk<ILogoutUC>()
    private val getAppLanguageUC = mockk<IGetAppLanguageUC> {
        coEvery { this@mockk.invoke() } returns LanguageEnum.SYSTEM
    }

    private fun createViewModel() = ProfileViewModel(
        sendEmailUC,
        checkEmailUC,
        logoutUC,
        getAppLanguageUC,
        mockk(),
        mockk()
    )

    @Test
    fun `sendVerificationEmail disables button and sends email`() =
        runTest(mainDispatcherRule.testDispatcher) {
            coEvery { sendEmailUC.execute() } returns Resource.success(null)
            coEvery { checkEmailUC.execute() } returns false
            val viewModel = createViewModel()

            viewModel.sendVerificationEmail()
            runCurrent()

            assertFalse(viewModel.isVerificationButtonEnabled.value)
            assertTrue(viewModel.isEmailVerificationSent.value)
        }

    @Test
    fun `verification polling stops after intensive period`() =
        runTest(mainDispatcherRule.testDispatcher) {
            coEvery { sendEmailUC.execute() } returns Resource.success(null)
            coEvery { checkEmailUC.execute() } returns false
            val viewModel = createViewModel()

            viewModel.sendVerificationEmail()
            advanceTimeBy(10 * 60 * 1000L)
            runCurrent()

            // Раньше опрос был бесконечным; теперь не больше ~40 проверок за 2 минуты
            coVerify(atMost = 45) { checkEmailUC.execute() }
        }

    @Test
    fun `logout calls use case and updates state`() = runTest(mainDispatcherRule.testDispatcher) {
        coEvery { logoutUC.execute() } returns Resource.success(null)
        val viewModel = createViewModel()

        viewModel.logout()
        advanceUntilIdle()

        coVerify { logoutUC.execute() }
        assertEquals(Status.SUCCESS, viewModel.logoutStateFlow.value.status)
    }
}
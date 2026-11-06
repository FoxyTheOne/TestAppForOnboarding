package com.foxynest.testappforonboarding

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.foxynest.testappforonboarding.domain.entities.language.LanguageEnum
import com.foxynest.testappforonboarding.domain.use_cases.appLanguage.IGetAppLanguageUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.email.ICheckEmailVerifiedUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.email.ISendEmailVerificationUC
import com.foxynest.testappforonboarding.other.Resource
import com.foxynest.testappforonboarding.presentation.profile.ProfileViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

/**
 * Подтверждение почты в профиле: кнопка блокируется после отправки, частая проверка статуса не бесконечна.
 * Благодаря виртуальному времени (MainDispatcherRule) «10 минут ожидания» проходят мгновенно.
 */
class ProfileViewModelTests {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val sendEmailUC = mockk<ISendEmailVerificationUC>()
    private val checkEmailUC = mockk<ICheckEmailVerifiedUC>()
    private val getAppLanguageUC = mockk<IGetAppLanguageUC> {
        coEvery { this@mockk.invoke() } returns LanguageEnum.SYSTEM
    }

    private fun createViewModel() =
        ProfileViewModel(sendEmailUC, checkEmailUC, getAppLanguageUC, mockk())

    @Test
    fun `sendVerificationEmail disables button and reports sent once`() =
        runTest(mainDispatcherRule.testDispatcher) {
            coEvery { sendEmailUC.execute() } returns Resource.success(null)
            coEvery { checkEmailUC.execute() } returns false
            val viewModel = createViewModel()

            viewModel.sendVerificationEmail()
            runCurrent()

            assertFalse(viewModel.isVerificationButtonEnabled.value)
            val event = viewModel.verificationEmailSentEvent.value!!
            assertEquals(Unit, event.getContentIfNotHandled())
            assertNull(event.getContentIfNotHandled())
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
    fun `verification status is unknown until checked`() =
        runTest(mainDispatcherRule.testDispatcher) {
            coEvery { checkEmailUC.execute() } returns false
            val viewModel = createViewModel()

            assertNull(viewModel.isEmailVerified.value)
            viewModel.checkEmailVerificationStatus()
            runCurrent()
            assertEquals(false, viewModel.isEmailVerified.value)
        }
}
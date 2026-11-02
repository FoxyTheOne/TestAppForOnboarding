package com.foxynest.testappforonboarding

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.foxynest.testappforonboarding.domain.entities.ErrorType
import com.foxynest.testappforonboarding.domain.entities.onboarding.OnboardingAnswers
import com.foxynest.testappforonboarding.domain.entities.user.UserPresentation
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.account.IDeleteAccountUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.IIsUserLoggedInUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.ILogoutUC
import com.foxynest.testappforonboarding.domain.use_cases.notification.IScheduleBirthdayNotificationUC
import com.foxynest.testappforonboarding.domain.use_cases.onboardingUseCases.IOnboardingUC
import com.foxynest.testappforonboarding.domain.use_cases.updatingDataUseCases.IUpdateFirebaseProfileUC
import com.foxynest.testappforonboarding.other.Resource
import com.foxynest.testappforonboarding.presentation.MainViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MainViewModelTests {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val onboardingUC = mockk<IOnboardingUC>(relaxed = true)
    private val updateProfileUC = mockk<IUpdateFirebaseProfileUC>()
    private val scheduleUC = mockk<IScheduleBirthdayNotificationUC>(relaxed = true)
    private val logoutUC = mockk<ILogoutUC>()
    private val deleteAccountUC = mockk<IDeleteAccountUC>()
    private val isLoggedInUC = mockk<IIsUserLoggedInUC> { every { execute() } returns false }
    private val authState = MutableStateFlow<UserPresentation?>(null)

    private val completedAnswers = OnboardingAnswers(
        userName = "Мама",
        childGender = "BOY",
        isItABoy = "YES",
        childName = "Ваня",
        childBirthDate = "2020-01-01",
        childHobbies = "chess"
    )

    private lateinit var viewModel: MainViewModel

    @Before
    fun setup() {
        every { onboardingUC.getOnboardingAnswersFlow() } returns flowOf(OnboardingAnswers())
        every { onboardingUC.getOnboardingDraftFlow() } returns flowOf(OnboardingAnswers())
        coEvery { scheduleUC.shouldAskNotificationPermission() } returns false

        viewModel = MainViewModel(
            onboardingUC = onboardingUC,
            updateFirebaseProfileUC = updateProfileUC,
            observeCurrentUserUC = mockk { every { execute() } returns flowOf(Resource.success(null)) },
            getAuthStateFlowUC = mockk { every { execute() } returns authState },
            isUserLoggedInUC = isLoggedInUC,
            loginValidateEmailUC = mockk(),
            updateEmailUC = mockk(),
            logoutUC = logoutUC,
            deleteAccountUC = deleteAccountUC,
            scheduleBirthdayNotificationUC = scheduleUC
        )
    }

    @Test
    fun `finish onboarding waits for the last answer to be saved`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val calls = mutableListOf<String>()
            coEvery { onboardingUC.updateOnboardingDraft(any()) } coAnswers {
                delay(500) // медленная запись на диск
                calls += "save hobbies"
            }
            coEvery { onboardingUC.commitOnboardingDraft() } coAnswers {
                calls += "commit"
                completedAnswers
            }

            viewModel.updateChildHobbies("chess")
            viewModel.finishOnboarding()
            advanceUntilIdle()

            assertEquals(listOf("save hobbies", "commit"), calls)
            coVerify { scheduleUC.update("Ваня", "2020-01-01", userIsInApp = true) }
        }

    @Test
    fun `repeated finish without draft does nothing`() =
        runTest(mainDispatcherRule.testDispatcher) {
            coEvery { onboardingUC.commitOnboardingDraft() } returns null

            viewModel.finishOnboarding()
            advanceUntilIdle()

            coVerify(exactly = 0) { scheduleUC.update(any(), any(), any()) }
        }

    @Test
    fun `cancel onboarding only drops the draft`() = runTest(mainDispatcherRule.testDispatcher) {
        viewModel.cancelOnboarding()
        advanceUntilIdle()

        coVerify { onboardingUC.cancelOnboardingDraft() }
        coVerify(exactly = 0) { onboardingUC.saveOnboardingAnswers(any()) }
    }

    @Test
    fun `forced sign out shows session ended message`() =
        runTest(mainDispatcherRule.testDispatcher) {
            authState.value = UserPresentation(uid = "u1")
            advanceUntilIdle()

            authState.value = null // Firebase отозвал сессию, например после смены почты
            advanceUntilIdle()

            assertTrue(viewModel.sessionEndedEvent.value?.getContentIfNotHandled() != null)
        }

    @Test
    fun `user logout does not show session ended message`() =
        runTest(mainDispatcherRule.testDispatcher) {
            coEvery { logoutUC.execute() } coAnswers {
                authState.value = null
                Resource.success(null)
            }
            authState.value = UserPresentation(uid = "u1")
            advanceUntilIdle()

            viewModel.logout()
            advanceUntilIdle()

            assertNull(viewModel.sessionEndedEvent.value)
        }

    @Test
    fun `failed account deletion keeps error state for the dialog`() =
        runTest(mainDispatcherRule.testDispatcher) {
            coEvery { deleteAccountUC.execute("wrong") } returns
                    Resource.error("Wrong password", ErrorType.WRONG_PASSWORD_ERROR)

            viewModel.deleteAccount("wrong")
            advanceUntilIdle()

            assertEquals(ErrorType.WRONG_PASSWORD_ERROR, viewModel.deleteAccountState.value?.data)
        }
}
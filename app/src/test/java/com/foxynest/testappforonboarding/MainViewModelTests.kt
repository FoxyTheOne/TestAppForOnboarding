package com.foxynest.testappforonboarding

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.foxynest.testappforonboarding.domain.entities.onboarding.OnboardingAnswers
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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

        viewModel = MainViewModel(
            onboardingUC = onboardingUC,
            updateFirebaseProfileUC = updateProfileUC,
            getCurrentFirebaseUserUC = mockk { coEvery { execute() } returns Resource.success(null) },
            getCurrentUserPresentationUC = mockk(),
            getAuthStateFlowUC = mockk { every { execute() } returns flowOf(null) },
            loginValidateEmailUC = mockk(),
            updateEmailUC = mockk(),
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
}
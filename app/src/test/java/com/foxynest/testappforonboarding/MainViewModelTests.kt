package com.foxynest.testappforonboarding

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.foxynest.testappforonboarding.domain.use_cases.updatingDataUseCases.IUpdateFirebaseProfileUC
import com.foxynest.testappforonboarding.domain.use_cases.onboardingUseCases.IOnboardingUC
import com.foxynest.testappforonboarding.presentation.MainViewModel
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Before
import org.junit.Rule

class MainViewModelTests {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    @MockK
    private lateinit var onboardingUC: IOnboardingUC

    @MockK
    private lateinit var updateProfileUC: IUpdateFirebaseProfileUC

    private lateinit var viewModel: MainViewModel

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        viewModel = MainViewModel(
            onboardingUC = onboardingUC,
            updateFirebaseProfileUC = updateProfileUC,
            getCurrentFirebaseUserUC = mockk(),
            getAuthStateFlowUC = mockk(),
            loginValidateEmailUC = mockk(),
            updateEmailUC = mockk(),
            scheduleBirthdayNotificationUC = mockk(),
            dispatcher = testDispatcher // Нужно добавить в конструктор
        )
    }

//    @Test
//    fun `save user name answer updates live data`() = runTest {
//        // Given
//        val userName = "Test User"
//        coEvery { onboardingUC.saveUserName(userName) } returns Unit
//
//        // When
//        viewModel.saveUserNameAnswer(userName)
//        testDispatcher.scheduler.advanceUntilIdle()
//
//        // Then
//        assertEquals(userName, viewModel.answers["USER_NAME_ANSWER"])
//    }
}
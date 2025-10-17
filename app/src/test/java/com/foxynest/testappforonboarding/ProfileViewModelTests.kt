package com.foxynest.testappforonboarding

import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.email.ICheckEmailVerifiedUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.email.ISendEmailVerificationUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.ILogoutUC
import com.foxynest.testappforonboarding.other.Resource
import com.foxynest.testappforonboarding.other.Status
import com.foxynest.testappforonboarding.presentation.profile.ProfileViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ProfileViewModelTests {
    private val testDispatcher = StandardTestDispatcher()

    @Test
    fun `sendVerificationEmail disables button and sends email`() = runTest {
        // Given
        val mockSendEmailUC = mockk<ISendEmailVerificationUC>()
        val mockCheckEmailUC = mockk<ICheckEmailVerifiedUC>()
        val mockLogoutUC = mockk<ILogoutUC>()

        coEvery { mockSendEmailUC.execute() } returns Resource.success(null)

        val viewModel = ProfileViewModel(mockSendEmailUC, mockCheckEmailUC, mockLogoutUC)

        // When
        viewModel.sendVerificationEmail()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertFalse(viewModel.isVerificationButtonEnabled.value)
        assertTrue(viewModel.isEmailVerificationSent.value)
    }

    @Test
    fun `logout calls use case and updates state`() = runTest {
        // Given
        val mockSendEmailUC = mockk<ISendEmailVerificationUC>()
        val mockCheckEmailUC = mockk<ICheckEmailVerifiedUC>()
        val mockLogoutUC = mockk<ILogoutUC>()

        coEvery { mockLogoutUC.execute() } returns Resource.success(null)

        val viewModel = ProfileViewModel(mockSendEmailUC, mockCheckEmailUC, mockLogoutUC)

        // When
        viewModel.logout()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { mockLogoutUC.execute() }
        assertEquals(Status.SUCCESS, viewModel.logoutStateFlow.value.status)
    }
}
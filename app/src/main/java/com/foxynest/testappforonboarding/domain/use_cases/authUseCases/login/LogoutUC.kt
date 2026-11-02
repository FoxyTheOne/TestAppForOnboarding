package com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login

import com.foxynest.testappforonboarding.domain.entities.ErrorType
import com.foxynest.testappforonboarding.domain.iRepository.IAuthRepository
import com.foxynest.testappforonboarding.domain.iRepository.IBirthdayNotificationRepository
import com.foxynest.testappforonboarding.domain.iRepository.IOnboardingRepository
import com.foxynest.testappforonboarding.other.Resource
import com.foxynest.testappforonboarding.other.Status
import javax.inject.Inject

/**
 * Выход из аккаунта целиком: Firebase + локальные данные + уведомление о дне рождения.
 * Локальные данные удаляются только после успешного выхода.
 */
class LogoutUC @Inject constructor(
    private val authRepository: IAuthRepository,
    private val onboardingRepository: IOnboardingRepository,
    private val birthdayNotificationRepository: IBirthdayNotificationRepository
) : ILogoutUC {
    override suspend fun execute(): Resource<ErrorType> {
        val result = authRepository.logout()
        if (result.status == Status.SUCCESS) {
            onboardingRepository.clearAllOnboardingData()
            birthdayNotificationRepository.cancel()
        }
        return result
    }
}
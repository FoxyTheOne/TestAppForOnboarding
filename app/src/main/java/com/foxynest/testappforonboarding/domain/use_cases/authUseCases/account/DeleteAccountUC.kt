package com.foxynest.testappforonboarding.domain.use_cases.authUseCases.account

import com.foxynest.testappforonboarding.domain.entities.ErrorType
import com.foxynest.testappforonboarding.domain.iRepository.IAuthRepository
import com.foxynest.testappforonboarding.domain.iRepository.IBirthdayNotificationRepository
import com.foxynest.testappforonboarding.domain.iRepository.IOnboardingRepository
import com.foxynest.testappforonboarding.other.Resource
import com.foxynest.testappforonboarding.other.Status
import javax.inject.Inject

/** Удаление аккаунта и всех данных пользователя: на сервере, на устройстве и запланированного уведомления. */
class DeleteAccountUC @Inject constructor(
    private val authRepository: IAuthRepository,
    private val onboardingRepository: IOnboardingRepository,
    private val birthdayNotificationRepository: IBirthdayNotificationRepository
) : IDeleteAccountUC {
    override suspend fun execute(password: String): Resource<ErrorType> {
        if (password.isBlank()) {
            return Resource.error("Password is empty", ErrorType.PASSWORD_EMPTY_ERROR)
        }
        val result = authRepository.deleteAccount(password)
        if (result.status == Status.SUCCESS) {
            onboardingRepository.clearAllOnboardingData()
            birthdayNotificationRepository.cancel()
        }
        return result
    }
}
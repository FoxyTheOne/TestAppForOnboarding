package com.foxynest.testappforonboarding.domain.use_cases.authUseCases.account

import com.foxynest.testappforonboarding.domain.entities.ErrorType
import com.foxynest.testappforonboarding.domain.iRepository.IAuthRepository
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.ILoginValidateEmailUC
import com.foxynest.testappforonboarding.other.Resource
import javax.inject.Inject

class SendPasswordResetUC @Inject constructor(
    private val authRepository: IAuthRepository,
    private val validateEmailUC: ILoginValidateEmailUC
) : ISendPasswordResetUC {
    override suspend fun execute(email: String): Resource<ErrorType> {
        val trimmedEmail = email.lowercase().trim()
        val validation = validateEmailUC.execute(trimmedEmail)
        if (!validation.successful) {
            return Resource.error(
                "Invalid email",
                validation.errorMessage ?: ErrorType.EMAIL_FORMAT_ERROR
            )
        }
        return authRepository.sendPasswordResetEmail(trimmedEmail)
    }
}
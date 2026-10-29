package com.foxynest.testappforonboarding.domain.use_cases.authUseCases.email

import com.foxynest.testappforonboarding.domain.iRepository.IAuthRepository
import javax.inject.Inject

class SendEmailVerificationUC @Inject constructor(
    private val authRepository: IAuthRepository
) : ISendEmailVerificationUC {
    override suspend fun execute() = authRepository.sendEmailVerification()
}
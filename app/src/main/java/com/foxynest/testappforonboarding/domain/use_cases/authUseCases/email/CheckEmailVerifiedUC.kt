package com.foxynest.testappforonboarding.domain.use_cases.authUseCases.email

import com.foxynest.testappforonboarding.data.repository.AuthRepository
import javax.inject.Inject

class CheckEmailVerifiedUC @Inject constructor(
    private val repository: AuthRepository
) : ICheckEmailVerifiedUC{
    override suspend fun execute() = repository.checkEmailVerified()
}
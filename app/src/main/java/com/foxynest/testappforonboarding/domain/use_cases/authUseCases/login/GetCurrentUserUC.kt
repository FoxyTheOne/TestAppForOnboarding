package com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login

import com.foxynest.testappforonboarding.data.repository.AuthRepository
import com.foxynest.testappforonboarding.domain.entities.UserPresentation
import com.foxynest.testappforonboarding.other.Resource
import javax.inject.Inject

class GetCurrentUserUC @Inject constructor(
    private val authRepository: AuthRepository
) : IGetCurrentUserUC {
    override suspend fun execute(): Resource<UserPresentation?> = authRepository.getCurrentUser()
}
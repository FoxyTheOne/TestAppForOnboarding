package com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login

import com.foxynest.testappforonboarding.domain.entities.user.UserPresentation
import com.foxynest.testappforonboarding.domain.iRepository.IAuthRepository
import com.foxynest.testappforonboarding.other.Resource
import javax.inject.Inject

class GetCurrentFirebaseUserUC @Inject constructor(
    private val authRepository: IAuthRepository
) : IGetCurrentFirebaseUserUC {
    override suspend fun execute(): Resource<UserPresentation?> =
        authRepository.getCurrentFirebaseUser()
}
package com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login

import com.foxynest.testappforonboarding.domain.entities.user.UserPresentation
import com.foxynest.testappforonboarding.domain.iRepository.IAuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAuthStateFlowUC @Inject constructor(
    private val authRepository: IAuthRepository
) : IGetAuthStateFlowUC {
    override fun execute(): Flow<UserPresentation?> = authRepository.observeAuthUser()
}
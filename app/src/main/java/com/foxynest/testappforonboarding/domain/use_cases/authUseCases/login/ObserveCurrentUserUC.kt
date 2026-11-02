package com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login

import com.foxynest.testappforonboarding.domain.entities.user.UserPresentation
import com.foxynest.testappforonboarding.domain.iRepository.IAuthRepository
import com.foxynest.testappforonboarding.other.Resource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveCurrentUserUC @Inject constructor(
    private val authRepository: IAuthRepository
) : IObserveCurrentUserUC {
    override fun execute(): Flow<Resource<UserPresentation?>> = authRepository.observeCurrentUser()
}
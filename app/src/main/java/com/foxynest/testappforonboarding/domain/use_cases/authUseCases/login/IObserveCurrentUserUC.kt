package com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login

import com.foxynest.testappforonboarding.domain.entities.user.UserPresentation
import com.foxynest.testappforonboarding.other.Resource
import kotlinx.coroutines.flow.Flow

interface IObserveCurrentUserUC {
    fun execute(): Flow<Resource<UserPresentation?>>
}
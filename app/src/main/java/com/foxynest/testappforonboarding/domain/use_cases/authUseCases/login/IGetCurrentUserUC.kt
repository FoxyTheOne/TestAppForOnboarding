package com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login

import com.foxynest.testappforonboarding.domain.entities.UserPresentation
import com.foxynest.testappforonboarding.other.Resource

interface IGetCurrentUserUC {
    suspend fun execute(): Resource<UserPresentation?>
}
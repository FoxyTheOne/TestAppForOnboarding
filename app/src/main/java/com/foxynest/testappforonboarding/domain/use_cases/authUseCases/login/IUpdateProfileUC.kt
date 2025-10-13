package com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login

import com.foxynest.testappforonboarding.domain.entities.ErrorType
import com.foxynest.testappforonboarding.domain.entities.UserPresentation
import com.foxynest.testappforonboarding.other.Resource

interface IUpdateProfileUC {
    suspend fun execute(user: UserPresentation): Resource<ErrorType>
}
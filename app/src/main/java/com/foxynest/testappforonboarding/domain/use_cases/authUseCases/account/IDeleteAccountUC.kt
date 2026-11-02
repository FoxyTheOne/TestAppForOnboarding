package com.foxynest.testappforonboarding.domain.use_cases.authUseCases.account

import com.foxynest.testappforonboarding.domain.entities.ErrorType
import com.foxynest.testappforonboarding.other.Resource

interface IDeleteAccountUC {
    suspend fun execute(password: String): Resource<ErrorType>
}
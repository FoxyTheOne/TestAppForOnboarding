package com.foxynest.testappforonboarding.domain.use_cases.authUseCases.account

import com.foxynest.testappforonboarding.domain.entities.ErrorType
import com.foxynest.testappforonboarding.other.Resource

interface ISendPasswordResetUC {
    suspend fun execute(email: String): Resource<ErrorType>
}
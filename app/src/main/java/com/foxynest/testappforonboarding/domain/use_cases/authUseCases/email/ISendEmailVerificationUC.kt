package com.foxynest.testappforonboarding.domain.use_cases.authUseCases.email

import com.foxynest.testappforonboarding.domain.entities.ErrorType
import com.foxynest.testappforonboarding.other.Resource

interface ISendEmailVerificationUC {
    suspend fun execute(): Resource<ErrorType>
}
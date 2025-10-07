package com.foxynest.testappforonboarding.domain.use_cases.authUseCases.email

import com.foxynest.testappforonboarding.other.Resource

interface ICheckEmailVerifiedUC {
    suspend fun execute(): Boolean
}
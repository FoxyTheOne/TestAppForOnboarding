package com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login

import com.foxynest.testappforonboarding.other.Resource

interface ILogoutUC {
    suspend fun execute(): Resource<Unit>
}
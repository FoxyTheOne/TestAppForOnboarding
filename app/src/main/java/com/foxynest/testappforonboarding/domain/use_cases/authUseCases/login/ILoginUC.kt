package com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login

import com.foxynest.testappforonboarding.other.Resource

interface ILoginUC {
    suspend fun execute(email: String, password: String): Resource<Unit>
}
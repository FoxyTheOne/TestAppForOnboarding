package com.foxynest.testappforonboarding.domain.use_cases.authUseCases.register

import com.foxynest.testappforonboarding.domain.entities.UserPresentation
import com.foxynest.testappforonboarding.other.Resource

interface IRegisterUC {
    suspend fun execute(email: String, password: String, user: UserPresentation): Resource<Unit>
}
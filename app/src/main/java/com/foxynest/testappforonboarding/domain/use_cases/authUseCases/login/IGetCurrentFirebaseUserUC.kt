package com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login

import com.foxynest.testappforonboarding.domain.entities.user.UserPresentation
import com.foxynest.testappforonboarding.other.Resource

interface IGetCurrentFirebaseUserUC {
    suspend fun execute(): Resource<UserPresentation?>
}
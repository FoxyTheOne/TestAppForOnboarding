package com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login

import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.ValidationResult

interface ILoginValidateEmailUC {
    fun execute(email: String): ValidationResult
}
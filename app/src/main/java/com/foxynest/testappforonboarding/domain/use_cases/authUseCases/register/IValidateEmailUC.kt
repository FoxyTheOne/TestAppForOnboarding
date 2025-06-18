package com.foxynest.testappforonboarding.domain.use_cases.authUseCases.register

import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.ValidationResult

interface IValidateEmailUC {
    fun execute(email: String): ValidationResult
}
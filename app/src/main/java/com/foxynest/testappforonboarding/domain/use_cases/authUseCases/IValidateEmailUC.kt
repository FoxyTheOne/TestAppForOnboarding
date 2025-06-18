package com.foxynest.testappforonboarding.domain.use_cases.authUseCases

interface IValidateEmailUC {
    fun execute(email: String): ValidationResult
}
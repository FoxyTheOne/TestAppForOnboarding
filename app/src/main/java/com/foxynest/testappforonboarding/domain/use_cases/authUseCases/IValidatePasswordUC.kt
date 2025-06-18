package com.foxynest.testappforonboarding.domain.use_cases.authUseCases

interface IValidatePasswordUC {
    fun execute(password: String): ValidationResult
}
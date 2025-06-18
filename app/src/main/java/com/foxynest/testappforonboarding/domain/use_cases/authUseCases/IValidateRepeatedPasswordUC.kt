package com.foxynest.testappforonboarding.domain.use_cases.authUseCases

interface IValidateRepeatedPasswordUC {
    fun execute(password: String, repeatedPassword: String): ValidationResult
}
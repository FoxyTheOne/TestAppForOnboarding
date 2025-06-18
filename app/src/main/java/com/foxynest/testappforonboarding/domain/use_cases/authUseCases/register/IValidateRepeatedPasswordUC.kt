package com.foxynest.testappforonboarding.domain.use_cases.authUseCases.register

import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.ValidationResult

interface IValidateRepeatedPasswordUC {
    fun execute(password: String, repeatedPassword: String): ValidationResult
}
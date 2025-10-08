package com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login

import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.ValidationResult

interface ILoginValidatePasswordUC {
    fun execute(password: String): ValidationResult
}
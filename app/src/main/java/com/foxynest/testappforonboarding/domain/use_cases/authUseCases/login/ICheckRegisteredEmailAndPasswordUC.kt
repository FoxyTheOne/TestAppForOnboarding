package com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login

import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.ValidationResult

interface ICheckRegisteredEmailAndPasswordUC {
    fun execute(email: String, password: String): ValidationResult
}

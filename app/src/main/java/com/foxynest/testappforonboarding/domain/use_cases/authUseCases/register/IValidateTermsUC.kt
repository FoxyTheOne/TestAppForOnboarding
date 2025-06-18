package com.foxynest.testappforonboarding.domain.use_cases.authUseCases.register

import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.ValidationResult

interface IValidateTermsUC {
    fun execute(acceptedTerms: Boolean): ValidationResult
}
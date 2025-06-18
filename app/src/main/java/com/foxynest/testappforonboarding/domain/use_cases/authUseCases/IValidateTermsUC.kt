package com.foxynest.testappforonboarding.domain.use_cases.authUseCases

interface IValidateTermsUC {
    fun execute(acceptedTerms: Boolean): ValidationResult
}
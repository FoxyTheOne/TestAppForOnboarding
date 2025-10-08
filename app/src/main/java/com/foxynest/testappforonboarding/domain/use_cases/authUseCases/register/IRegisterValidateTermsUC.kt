package com.foxynest.testappforonboarding.domain.use_cases.authUseCases.register

import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.ValidationResult

interface IRegisterValidateTermsUC {
    fun execute(acceptedTerms: Boolean): ValidationResult
}
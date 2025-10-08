package com.foxynest.testappforonboarding.domain.use_cases.authUseCases

import com.foxynest.testappforonboarding.domain.entities.ErrorType

data class ValidationResult(
    val successful: Boolean,
//    val errorMessage: String? = null
    val errorMessage: ErrorType? = null
)

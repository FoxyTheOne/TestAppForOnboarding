package com.foxynest.testappforonboarding.domain.use_cases.authUseCases

data class ValidationResult(
    val successful: Boolean,
    val errorMessage: String? = null
)

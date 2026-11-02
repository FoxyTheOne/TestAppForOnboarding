package com.foxynest.testappforonboarding.presentation.welcome.auth.registration

import com.foxynest.testappforonboarding.domain.entities.ErrorType

// Тип ошибки, а не текст: текст подбирает фрагмент по своему (локализованному) контексту

data class RegistrationFormState(
    val email: String = "",
    val emailError: ErrorType? = null,
    val password: String = "",
    val passwordError: ErrorType? = null,
    val repeatedPassword: String = "",
    val repeatedPasswordError: ErrorType? = null
)
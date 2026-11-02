package com.foxynest.testappforonboarding.presentation.welcome.auth.login

import com.foxynest.testappforonboarding.domain.entities.ErrorType

// Тип ошибки, а не текст: текст подбирает фрагмент по своему (локализованному) контексту

data class LoginFormState(
    val email: String = "",
    val emailError: ErrorType? = null,
    val password: String = "",
    val passwordError: ErrorType? = null
)
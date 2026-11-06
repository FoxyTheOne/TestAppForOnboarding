package com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login

import com.foxynest.testappforonboarding.domain.entities.ErrorType
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.ValidationResult
import javax.inject.Inject

/**
 * Проверка пароля для ВХОДА: только «не пустой». Требования к сложности здесь не нужны – пароль уже
 * придуман при регистрации, а если правила когда-нибудь станут строже, старые пароли всё равно должны подходить.
 */
class LoginValidatePasswordUC @Inject constructor() : ILoginValidatePasswordUC {
    // Для ВХОДА - базовая валидация
    // Data слой здесь не нужен, мы проверяем только те данные, которые вводят в textfield
    override fun execute(password: String): ValidationResult {
        if (password.isBlank()) {
            return ValidationResult(false, ErrorType.PASSWORD_EMPTY_ERROR)
        }
        return ValidationResult(true)
    }
}
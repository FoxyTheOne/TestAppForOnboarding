package com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login

import com.foxynest.testappforonboarding.domain.entities.ErrorType
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.ValidationResult
import com.foxynest.testappforonboarding.other.utils.EmailUtils
import javax.inject.Inject

/**
 * Проверка почты для ВХОДА: не пустая и похожа на адрес. Сервер не спрашиваем – только то, что ввели в поле.
 * Используется и для сброса пароля, и для смены почты.
 */
class LoginValidateEmailUC @Inject constructor() : ILoginValidateEmailUC {
    // Для ВХОДА - базовая валидация
    override fun execute(email: String): ValidationResult {
        if (email.isBlank()) {
            return ValidationResult(
                false,
                ErrorType.EMAIL_EMPTY_ERROR
            )
        }

        if (!EmailUtils.emailMatches(email)) {
            return ValidationResult(
                false,
                ErrorType.EMAIL_FORMAT_ERROR
            )
        }

        return ValidationResult(true)
    }
}
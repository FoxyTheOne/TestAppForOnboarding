package com.foxynest.testappforonboarding.domain.use_cases.authUseCases.register

import com.foxynest.testappforonboarding.domain.entities.ErrorType
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.ValidationResult
import com.foxynest.testappforonboarding.other.constants.AuthConstants.MIN_PASSWORD_LENGTH
import javax.inject.Inject

/**
 * Проверка пароля при РЕГИСТРАЦИИ (строгая): не короче MIN_PASSWORD_LENGTH и содержит и буквы, и цифры.
 * У Firebase есть своё требование (не короче 6 символов), наше строже – значит, сервер его не отклонит.
 */
class RegisterValidatePasswordUC @Inject constructor() : IRegisterValidatePasswordUC {

    // Для РЕГИСТРАЦИИ - строгая валидация
    // Data слой здесь не нужен, мы проверяем только те данные, которые вводят в textfield
    override fun execute(password: String): ValidationResult {
        if (password.length < MIN_PASSWORD_LENGTH) {
            return ValidationResult(
                false,
                ErrorType.PASSWORD_LENGTH_ERROR_NEEDS_MIN_PASSWORD_LENGTH
            )
        }

        val containsLettersAndDigits =
            password.any { it.isDigit() } && password.any { it.isLetter() }
        if (!containsLettersAndDigits) {
            return ValidationResult(
                false,
                ErrorType.PASSWORD_LETTERS_ERROR
            )
        }

        return ValidationResult(true)
    }

}
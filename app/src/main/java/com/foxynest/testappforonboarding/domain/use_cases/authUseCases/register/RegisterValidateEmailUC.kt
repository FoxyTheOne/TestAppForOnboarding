package com.foxynest.testappforonboarding.domain.use_cases.authUseCases.register

import com.foxynest.testappforonboarding.domain.entities.ErrorType
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.ValidationResult
import com.foxynest.testappforonboarding.other.utils.EmailUtils
import javax.inject.Inject

/** Проверка почты при РЕГИСТРАЦИИ: не пустая и похожа на адрес. Занята ли она – скажет только сервер. */
class RegisterValidateEmailUC @Inject constructor() : IRegisterValidateEmailUC {

    // Data слой здесь не нужен, мы проверяем только те данные, которые вводят в textfield
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
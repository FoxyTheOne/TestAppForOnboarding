package com.foxynest.testappforonboarding.domain.use_cases.authUseCases.register

import com.foxynest.testappforonboarding.domain.entities.ErrorType
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.ValidationResult
import javax.inject.Inject

/** «Повторите пароль» совпадает с паролем – защита от опечатки, которую человек не видит за точками. */
class RegisterValidateRepeatedPasswordUC @Inject constructor() : IRegisterValidateRepeatedPasswordUC {

    // Data слой здесь не нужен, мы проверяем только те данные, которые вводят в textfield
    override fun execute(password: String, repeatedPassword: String): ValidationResult {
        if (password != repeatedPassword) {
            return ValidationResult(
                false,
                ErrorType.PASSWORD_MATCH_ERROR
            )
        }

        return ValidationResult(true)
    }

}
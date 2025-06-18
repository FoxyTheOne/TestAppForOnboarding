package com.foxynest.testappforonboarding.domain.use_cases.authUseCases

import com.foxynest.testappforonboarding.other.constants.AuthConstants.MIN_PASSWORD_LENGTH
import javax.inject.Inject

/**
 * Domain layer, UseCase. Бизнес-логика, Kotlin. Работает только с Repository.
 *
 * Interactor ответственен за обеспечение данными отдельные экраны (для каждого экрана - отдельный Interactor).
 * При работе с model, здесь происходит преобразование local -> presentation, т.е.
 * преобразование моделей в модели нижнего уровня перед тем, как нижний уровень сможет их использовать.
 *
 * Use case is a very integral part of clean architecture. It's a middle layer between view model and data layer.
 * Maybe some access to a repository that accesses a database or a directly access to some kind of data source.
 * We need use cases because they are an excellent way to follow the single responsibility principle. So we have a class for every single bit of logic we have in our project.
 * So typically each use case consists of just one function which executes that use case (which validates the email in our case here)
 */
class ValidatePasswordUC @Inject constructor() : IValidatePasswordUC {

    override fun execute(password: String): ValidationResult {
        if (password.length < MIN_PASSWORD_LENGTH) {
            // TODO Здесь можно использовать String Resources
            return ValidationResult(
                false,
                "В пароле должно быть минимум $MIN_PASSWORD_LENGTH символов"
            )
        }

        val containsLettersAndDigits =
            password.any { it.isDigit() } && password.any { it.isLetter() }
        if (!containsLettersAndDigits) {
            return ValidationResult(
                false,
                "Пароль должен содержать хотябы одну цифру и одну букву"
            )
        }

        return ValidationResult(
            true
        )
    }

    // We can make a data class right here (if this type of result class would really differ per use case)
    // data class Result (..) and we would have here all fields that are individual to the email validation
    // But since it's the same for all of our use cases let's create a separate class ValidationResult
}
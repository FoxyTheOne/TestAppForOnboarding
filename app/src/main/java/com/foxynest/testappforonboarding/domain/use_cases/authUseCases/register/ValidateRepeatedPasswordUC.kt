package com.foxynest.testappforonboarding.domain.use_cases.authUseCases.register

import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.ValidationResult
import javax.inject.Inject

/**
 * Domain layer, UseCase. Бизнес-логика, Kotlin.
 * It's a middle layer between view model and data layer.
 * Работает только с Repository (maybe some access to a repository that accesses a database or a directly access to some kind of data source).
 *
 * При работе с model, здесь происходит преобразование local -> presentation,
 * т.е. преобразование моделей в модели нижнего уровня перед тем, как нижний уровень сможет их использовать.
 *
 * We need use cases because they are an excellent way to follow the single responsibility principle.
 * So we have a class for every single bit of logic we have in our project.
 * Typically each use case consists of just one function which executes that use case (which validates the email for instance)
 */
class ValidateRepeatedPasswordUC @Inject constructor() : IValidateRepeatedPasswordUC {

    // Data слой здесь не нужен, мы проверяем только те данные, которые вводят в textfield
    override fun execute(password: String, repeatedPassword: String): ValidationResult {
        if (password != repeatedPassword) {
            // TODO Здесь можно использовать String Resources
            return ValidationResult(
                false,
                "Пароли не совпадают"
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
package com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login

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
class CheckRegisteredEmailAndPasswordUC @Inject constructor() : ICheckRegisteredEmailAndPasswordUC {
    override fun execute(email: String, password: String): ValidationResult {
        // TODO Сначала узнаём, зарегистрирована ли такая почта и соответствует ли пароль, а затем передаём результат в UI
        val isEmailRegistered = false
        val isPasswordCorrect = false

        // По правилам безопасности нельзя говорить, не верен логин или пароль, только всё вместе.
        if (!isEmailRegistered || !isPasswordCorrect) {
            // TODO Здесь можно использовать String Resources
            return ValidationResult(
                false,
                "Не верная почта или пароль"
            )
        }

        return ValidationResult(
            true
        )
    }
}
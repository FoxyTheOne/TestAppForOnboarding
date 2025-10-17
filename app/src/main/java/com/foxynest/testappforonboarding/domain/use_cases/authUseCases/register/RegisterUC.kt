package com.foxynest.testappforonboarding.domain.use_cases.authUseCases.register

import com.foxynest.testappforonboarding.data.repository.AuthRepository
import com.foxynest.testappforonboarding.domain.entities.ErrorType
import com.foxynest.testappforonboarding.domain.entities.user.UserPresentation
import com.foxynest.testappforonboarding.other.Resource
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
class RegisterUC @Inject constructor(
    private val authRepository: AuthRepository
) : IRegisterUC {
    override suspend fun execute(
        email: String,
        password: String,
        user: UserPresentation
    ): Resource<ErrorType> {
        if (email.isBlank() || password.isBlank()) {
            return Resource.error("Поле не может быть пустым", ErrorType.FIELD_EMPTY_ERROR)
        }
        return authRepository.register(email, password, user)
    }
}
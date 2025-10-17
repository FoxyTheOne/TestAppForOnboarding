package com.foxynest.testappforonboarding.domain.use_cases.updatingDataUseCases

import com.foxynest.testappforonboarding.data.repository.AuthRepository
import com.foxynest.testappforonboarding.domain.entities.ErrorType
import com.foxynest.testappforonboarding.domain.entities.user.UserPresentation
import com.foxynest.testappforonboarding.other.Resource
import com.foxynest.testappforonboarding.other.extensions.isUserDataValid
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
class UpdateProfileUC @Inject constructor(
    private val authRepository: AuthRepository
) : IUpdateProfileUC {
    override suspend fun execute(user: UserPresentation): Resource<ErrorType> {
        return if (user.isUserDataValid()) {
            authRepository.updateProfile(user)
        } else {
            Resource.error(
                "Данные не валидны для сохранения - есть пустые поля",
                ErrorType.NOT_VALID_USER_DATA_FOR_SAVING_TO_FIREBASE
            )
        }
    }
}
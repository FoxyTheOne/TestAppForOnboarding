package com.foxynest.testappforonboarding.domain.use_cases.updatingDataUseCases

import com.foxynest.testappforonboarding.domain.entities.ErrorType
import com.foxynest.testappforonboarding.domain.entities.user.UserPresentation
import com.foxynest.testappforonboarding.domain.entities.user.isValidData
import com.foxynest.testappforonboarding.domain.iRepository.IAuthRepository
import com.foxynest.testappforonboarding.other.Resource
import javax.inject.Inject

/**
 * Сохранение анкеты в Firebase с защитой: неполный профиль не отправляется.
 * Запись идёт через set + merge, и пустое поле перезаписало бы хорошее значение на сервере пустым.
 */
class UpdateFirebaseProfileUC @Inject constructor(
    private val authRepository: IAuthRepository
) : IUpdateFirebaseProfileUC {
    override suspend fun execute(user: UserPresentation): Resource<ErrorType> {
        return if (user.isValidData()) {
            authRepository.updateFirebaseProfile(user)
        } else {
            Resource.error(
                "Данные не валидны для сохранения - есть пустые поля",
                ErrorType.NOT_VALID_USER_DATA_FOR_SAVING_TO_FIREBASE
            )
        }
    }
}
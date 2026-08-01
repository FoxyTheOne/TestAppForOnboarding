package com.foxynest.testappforonboarding.domain.use_cases.authUseCases

import com.foxynest.testappforonboarding.domain.entities.user.UserPresentation
import com.foxynest.testappforonboarding.domain.iRepository.IAuthRepository
import com.foxynest.testappforonboarding.domain.iRepository.IOnboardingRepository
import com.foxynest.testappforonboarding.other.Resource
import com.foxynest.testappforonboarding.other.Status
import com.foxynest.testappforonboarding.other.extensions.createUserFromAnswersForNotRegisteredUser
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Domain layer, UseCase. Бизнес-логика, Kotlin. Работает только с Repository.
 *
 * Interactor ответственен за обеспечение данными отдельные экраны (для каждого экрана - отдельный Interactor).
 * При работе с model, здесь происходит преобразование local -> presentation, т.е.
 * преобразование моделей в модели нижнего уровня перед тем, как нижний уровень сможет их использовать.
 */
class GetCurrentUserPresentationUC @Inject constructor(
    private val authRepository: IAuthRepository,
    private val onboardingRepository: IOnboardingRepository
) : IGetCurrentUserPresentationUC {
    override suspend fun invoke(): Resource<UserPresentation?> {
        // 1. Проверяем залогиненного пользователя
        val currentFirebaseUser = authRepository.getCurrentFirebaseUser()

        if (currentFirebaseUser.status == Status.SUCCESS) {
            return currentFirebaseUser
        } else {
            // 2. Иначе берём из DataStore
            val answers = onboardingRepository.getOnboardingAnswersFlow().first()
            return if (
            // TODO можно сделать extention на проверку полей
                !answers.userName.isNullOrBlank() && answers.userName != "null" &&
                !answers.childName.isNullOrBlank() && answers.childName != "null" &&
                !answers.childBirthDate.isNullOrBlank() && answers.childBirthDate != "null"
            ) {
                Resource.success(answers.createUserFromAnswersForNotRegisteredUser())
            } else {
                Resource.error("Поля пользователя не заполнены", null)
            }
        }
    }
}

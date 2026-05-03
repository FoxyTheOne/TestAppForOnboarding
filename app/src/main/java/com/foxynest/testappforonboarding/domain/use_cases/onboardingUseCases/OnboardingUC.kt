package com.foxynest.testappforonboarding.domain.use_cases.onboardingUseCases

import com.foxynest.testappforonboarding.domain.entities.ErrorType
import com.foxynest.testappforonboarding.domain.entities.onboarding.OnboardingAnswers
import com.foxynest.testappforonboarding.domain.iRepository.IOnboardingRepository
import com.foxynest.testappforonboarding.other.Resource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Domain layer, UseCase. Бизнес-логика, Kotlin. Работает только с Repository.
 *
 * Interactor ответственен за обеспечение данными отдельные экраны (для каждого экрана - отдельный Interactor).
 * При работе с model, здесь происходит преобразование local -> presentation, т.е.
 * преобразование моделей в модели нижнего уровня перед тем, как нижний уровень сможет их использовать.
 */
class OnboardingUC @Inject constructor(
    private val onboardingRepository: IOnboardingRepository
) : IOnboardingUC {
    // Onboarding state
    override suspend fun saveOnboardingState(isCompleted: Boolean): Resource<ErrorType> =
        onboardingRepository.saveOnboardingState(isCompleted)

    override fun isOnboardingCompleted(): Flow<Boolean> =
        onboardingRepository.isOnboardingCompleted()

    override suspend fun clearAllOnboardingData(): Resource<ErrorType> {
        return onboardingRepository.clearAllOnboardingData()
    }

    // SAVE
    override suspend fun saveOnboardingAnswers(answers: OnboardingAnswers) =
        onboardingRepository.saveOnboardingAnswers(answers)

    // GET
    override fun getOnboardingAnswersFlow(): Flow<OnboardingAnswers> =
        onboardingRepository.getOnboardingAnswersFlow()
}
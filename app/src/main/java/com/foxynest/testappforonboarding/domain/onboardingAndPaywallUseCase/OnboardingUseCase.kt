package com.foxynest.testappforonboarding.domain.onboardingAndPaywallUseCase

import com.foxynest.testappforonboarding.domain.iRepository.IOnboardingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Domain layer, UseCase. Бизнес-логика, Kotlin. Работает только с Repository.
 *
 * Interactor ответственен за обеспечение данными отдельные экраны (для каждого экрана - отдельный Interactor).
 * При работе с model, здесь происходит преобразование local -> presentation, т.е.
 * преобразование моделей в модели нижнего уровня перед тем, как нижний уровень сможет их использовать.
 */
class OnboardingUseCase @Inject constructor(
    private val onboardingRepository: IOnboardingRepository
) : IOnboardingUseCase {
    override suspend fun saveOnboardingState(isCompleted: Boolean) =
        onboardingRepository.saveOnboardingState(isCompleted)

    override fun isOnboardingCompleted(): Flow<Boolean> =
        onboardingRepository.isOnboardingCompleted()

    override suspend fun saveGender(gender: String) = onboardingRepository.saveGender(gender)
    override suspend fun saveName(name: String) = onboardingRepository.saveName(name)
    override suspend fun saveIsItABoy(answer: String) = onboardingRepository.saveIsItABoy(answer)
    override suspend fun saveHobbyAnswer(hobby: String) = onboardingRepository.saveHobbyAnswer(hobby)

    override fun getGenderFlow(): Flow<String?> = onboardingRepository.getGenderFlow()
    override fun getNameFlow(): Flow<String?> = onboardingRepository.getNameFlow()
    override fun getIsItABoyFlow(): Flow<String?> = onboardingRepository.getIsItABoyFlow()
    override fun getHobbyFlow(): Flow<String?> = onboardingRepository.getHobbyFlow()
}
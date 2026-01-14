package com.foxynest.testappforonboarding.domain.use_cases.onboardingUseCases

import com.foxynest.testappforonboarding.domain.entities.ErrorType
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
    override suspend fun saveOnboardingState(isCompleted: Boolean) : Resource<ErrorType> =
        onboardingRepository.saveOnboardingState(isCompleted)

    override fun isOnboardingCompleted(): Flow<Boolean> =
        onboardingRepository.isOnboardingCompleted()

    // SAVE
    override suspend fun saveUserName(userName: String) =
        onboardingRepository.saveUserName(userName)

    override suspend fun saveGender(gender: String) = onboardingRepository.saveGender(gender)
    override suspend fun saveName(name: String) = onboardingRepository.saveName(name)
    override suspend fun saveIsItABoy(answer: String) = onboardingRepository.saveIsItABoy(answer)
    override suspend fun saveBirthDate(birthDate: String) =
        onboardingRepository.saveBirthDate(birthDate)

    override suspend fun saveChildAge(age: Int) = onboardingRepository.saveChildAge(age)
    override suspend fun saveHobbyAnswer(hobby: String) =
        onboardingRepository.saveHobbyAnswer(hobby)

    // GET
    override fun getUserNameFlow(): Flow<String?> =
        onboardingRepository.getUserNameFlow()

    override fun getGenderFlow(): Flow<String?> = onboardingRepository.getGenderFlow()
    override fun getNameFlow(): Flow<String?> = onboardingRepository.getNameFlow()
    override fun getIsItABoyFlow(): Flow<String?> = onboardingRepository.getIsItABoyFlow()
    override fun getBirthDateFlow(): Flow<String?> =
        onboardingRepository.getBirthDateFlow()

    override fun getChildAgeFlow(): Flow<Int?> =
        onboardingRepository.getChildAgeFlow()
    override fun getHobbyFlow(): Flow<String?> = onboardingRepository.getHobbyFlow()
}
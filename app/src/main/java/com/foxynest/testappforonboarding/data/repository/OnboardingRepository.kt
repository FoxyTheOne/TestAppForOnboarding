package com.foxynest.testappforonboarding.data.repository

import com.foxynest.testappforonboarding.data.dataSource.datastorePreferences.IAppDatastorePreferences
import com.foxynest.testappforonboarding.domain.entities.ErrorType
import com.foxynest.testappforonboarding.domain.iRepository.IOnboardingRepository
import com.foxynest.testappforonboarding.other.Resource
import kotlinx.coroutines.flow.Flow
import java.io.IOException
import javax.inject.Inject

/**
 * Data layer, Repository. Работает с Local и Remote data source.
 *
 * Repository - объект, предоставляющий доступ к данным с возможностью выбора источника данных в зависимости от условий.
 * Подписка на локальную базу данных Room. Раскладываем данные.
 * При работе с model, здесь происходит запрос в remote, преобразование remote -> local, сохранение результата в базу данных.
 */
class OnboardingRepository @Inject constructor(
    private val appDatastorePreferences: IAppDatastorePreferences
) : IOnboardingRepository {
    // Onboarding state
    override suspend fun saveOnboardingState(isCompleted: Boolean) : Resource<ErrorType>{
        return try {
            appDatastorePreferences.saveOnboardingState(isCompleted)
            Resource.success(null)
        } catch (e: IOException){
            Resource.error("Ошибка при сохранении статуса онбординга", ErrorType.FAILED_TO_SAVE_ONBOARDING_STATE_LOCAL)
        }
    }

    override fun isOnboardingCompleted(): Flow<Boolean> =
        appDatastorePreferences.isOnboardingCompleted()

    // SAVE
    override suspend fun saveUserName(userName: String) =
        appDatastorePreferences.saveUserName(userName)

    override suspend fun saveGender(gender: String) =
        appDatastorePreferences.saveGender(gender)

    override suspend fun saveName(name: String) = appDatastorePreferences.saveName(name)
    override suspend fun saveIsItABoy(answer: String) = appDatastorePreferences.saveIsItABoy(answer)
    override suspend fun saveBirthDate(birthDate: String) =
        appDatastorePreferences.saveBirthDate(birthDate)

    override suspend fun saveChildAge(age: Int) =
        appDatastorePreferences.saveChildAge(age)

    override suspend fun saveHobbyAnswer(hobby: String) =
        appDatastorePreferences.saveHobbyAnswer(hobby)

    // GET
    override fun getUserNameFlow(): Flow<String?> = appDatastorePreferences.getUserNameFlow()
    override fun getGenderFlow(): Flow<String?> = appDatastorePreferences.getGenderFlow()
    override fun getNameFlow(): Flow<String?> = appDatastorePreferences.getNameFlow()
    override fun getIsItABoyFlow(): Flow<String?> = appDatastorePreferences.getIsItABoyFlow()
    override fun getBirthDateFlow(): Flow<String?> = appDatastorePreferences.getBirthDateFlow()
    override fun getChildAgeFlow(): Flow<Int?> = appDatastorePreferences.getChildAgeFlow()
    override fun getHobbyFlow(): Flow<String?> = appDatastorePreferences.getHobbyFlow()
}
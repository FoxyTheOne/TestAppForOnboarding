package com.foxynest.testappforonboarding.data.repository

import com.foxynest.testappforonboarding.data.dataSource.datastorePreferences.IAppDatastorePreferences
import com.foxynest.testappforonboarding.domain.iRepository.IOnboardingRepository
import kotlinx.coroutines.flow.Flow
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
    override suspend fun saveOnboardingState(isCompleted: Boolean) =
        appDatastorePreferences.saveOnboardingState(isCompleted)

    override fun isOnboardingCompleted(): Flow<Boolean> =
        appDatastorePreferences.isOnboardingCompleted()

    override suspend fun saveGender(gender: String) =
        appDatastorePreferences.saveGender(gender)

    override suspend fun saveName(name: String) = appDatastorePreferences.saveName(name)

    override suspend fun saveIsItABoy(answer: String) = appDatastorePreferences.saveIsItABoy(answer)
    override suspend fun saveHobbyAnswer(hobby: String) = appDatastorePreferences.saveHobbyAnswer(hobby)

    override fun getGenderFlow(): Flow<String?> = appDatastorePreferences.getGenderFlow()

    override fun getNameFlow(): Flow<String?> = appDatastorePreferences.getNameFlow()

    override fun getIsItABoyFlow(): Flow<String?> = appDatastorePreferences.getIsItABoyFlow()
    override fun getHobbyFlow(): Flow<String?> = appDatastorePreferences.getHobbyFlow()
}
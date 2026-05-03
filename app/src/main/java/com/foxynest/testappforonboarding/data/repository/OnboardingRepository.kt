package com.foxynest.testappforonboarding.data.repository

import com.foxynest.testappforonboarding.data.dataSource.datastorePreferences.IAppDatastorePreferences
import com.foxynest.testappforonboarding.domain.entities.ErrorType
import com.foxynest.testappforonboarding.domain.entities.onboarding.OnboardingAnswers
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
    override suspend fun saveOnboardingState(isCompleted: Boolean): Resource<ErrorType> {
        return try {
            appDatastorePreferences.saveOnboardingState(isCompleted)
            Resource.success(null)
        } catch (e: IOException) {
            Resource.error(
                "Ошибка при сохранении статуса онбординга",
                ErrorType.FAILED_TO_SAVE_ONBOARDING_STATE_LOCAL
            )
        }
    }

    override fun isOnboardingCompleted(): Flow<Boolean> =
        appDatastorePreferences.isOnboardingCompleted()

    override suspend fun clearAllOnboardingData(): Resource<ErrorType> {
        return try {
            appDatastorePreferences.clearAllOnboardingData()
            Resource.success(null)
        } catch (e: IOException) {
            Resource.error(
                "Ошибка при очистке данных онбординга",
                ErrorType.FAILED_TO_CLEAR_DATA
            )
        }
    }

    // SAVE
    override suspend fun saveOnboardingAnswers(answers: OnboardingAnswers) =
        appDatastorePreferences.saveOnboardingAnswers(answers)

    // GET
    override fun getOnboardingAnswersFlow(): Flow<OnboardingAnswers> =
        appDatastorePreferences.getOnboardingAnswersFlow()
}
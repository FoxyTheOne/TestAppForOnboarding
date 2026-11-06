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
 * Data layer, Repository. Работает с Local data source (DataStore).
 *
 * Анкета на устройстве хранится в двух вариантах:
 * - **подтверждённые ответы** – то, что видно в профиле и уходит в Firebase;
 * - **черновик** – ответы текущего прохождения опроса. Подтверждённые ответы не меняются, пока опрос не завершён:
 *   отмена посреди опроса или закрытие приложения не портят уже сохранённую анкету.
 *   В конце черновик одной операцией переносится в подтверждённые ([commitOnboardingDraft]).
 */
class OnboardingRepository @Inject constructor(
    private val appDatastorePreferences: IAppDatastorePreferences
) : IOnboardingRepository {
    override fun isOnboardingCompleted(): Flow<Boolean> =
        appDatastorePreferences.isOnboardingCompleted()

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

    override suspend fun saveOnboardingAnswers(answers: OnboardingAnswers) =
        appDatastorePreferences.saveOnboardingAnswers(answers)

    override fun getOnboardingAnswersFlow(): Flow<OnboardingAnswers> =
        appDatastorePreferences.getOnboardingAnswersFlow()

    override suspend fun startOnboardingDraft() =
        appDatastorePreferences.startOnboardingDraft()

    override suspend fun updateOnboardingDraft(transform: (OnboardingAnswers) -> OnboardingAnswers) =
        appDatastorePreferences.updateOnboardingDraft(transform)

    override fun getOnboardingDraftFlow(): Flow<OnboardingAnswers> =
        appDatastorePreferences.getOnboardingDraftFlow()

    override suspend fun clearOnboardingDraft() =
        appDatastorePreferences.clearOnboardingDraft()

    override suspend fun commitOnboardingDraft(isValid: (OnboardingAnswers) -> Boolean): OnboardingAnswers? =
        appDatastorePreferences.commitOnboardingDraft(isValid)
}
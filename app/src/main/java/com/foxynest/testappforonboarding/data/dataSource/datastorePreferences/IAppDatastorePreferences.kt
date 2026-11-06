package com.foxynest.testappforonboarding.data.dataSource.datastorePreferences

import com.foxynest.testappforonboarding.domain.entities.BirthdayNotificationData
import com.foxynest.testappforonboarding.domain.entities.language.LanguageEnum
import com.foxynest.testappforonboarding.domain.entities.onboarding.OnboardingAnswers
import kotlinx.coroutines.flow.Flow

/**
 * Всё, что приложение хранит на устройстве (в DataStore). Реализация – [AppDatastorePreferences].
 *
 * Функции с `Flow` – «подписка»: значение приходит сразу и потом при каждом изменении.
 * `suspend`-функции – разовое чтение или запись (работа с диском, поэтому не на главном потоке).
 */
interface IAppDatastorePreferences {
    // Migration
    suspend fun getSchemaVersion(): Int
    suspend fun setSchemaVersion(version: Int)

    // Onboarding state
    suspend fun saveOnboardingState(isCompleted: Boolean)
    fun isOnboardingCompleted(): Flow<Boolean>
    suspend fun clearAllOnboardingData()

    // Подтверждённые ответы
    suspend fun saveOnboardingAnswers(answers: OnboardingAnswers)
    fun getOnboardingAnswersFlow(): Flow<OnboardingAnswers>

    // Черновик опроса
    suspend fun startOnboardingDraft()
    suspend fun updateOnboardingDraft(transform: (OnboardingAnswers) -> OnboardingAnswers)
    fun getOnboardingDraftFlow(): Flow<OnboardingAnswers>
    suspend fun clearOnboardingDraft()

    /** Атомарно переносит черновик в ответы и отмечает опрос пройденным. null – черновика нет или он невалиден. */
    suspend fun commitOnboardingDraft(isValid: (OnboardingAnswers) -> Boolean): OnboardingAnswers?

    // Уведомление на день рождения
    suspend fun saveBirthdayNotificationData(birthNotifData: BirthdayNotificationData)
    suspend fun getBirthdayNotificationData(): BirthdayNotificationData
    suspend fun clearBirthdayNotificationData()
    suspend fun getBirthdayLastNotifiedKey(): String?
    suspend fun saveBirthdayLastNotifiedKey(key: String)
    suspend fun wasNotificationPermissionAsked(): Boolean
    suspend fun markNotificationPermissionAsked()

    // Переключение языка в приложении
    suspend fun saveAppLanguage(languageCode: LanguageEnum) // "system", "ru", "en"
    fun getAppLanguageFlow(): Flow<String> // всегда возвращает валидный язык, по умолчанию "system"
    suspend fun isLanguageMigratedToAppCompat(): Boolean
    suspend fun markLanguageMigratedToAppCompat()
}
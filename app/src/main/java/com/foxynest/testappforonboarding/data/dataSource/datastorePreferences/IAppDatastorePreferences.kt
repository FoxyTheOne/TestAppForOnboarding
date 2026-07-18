package com.foxynest.testappforonboarding.data.dataSource.datastorePreferences

import com.foxynest.testappforonboarding.domain.entities.language.LanguageEnum
import com.foxynest.testappforonboarding.domain.entities.onboarding.OnboardingAnswers
import kotlinx.coroutines.flow.Flow

interface IAppDatastorePreferences {
    // Migration
    suspend fun getSchemaVersion(): Int
    suspend fun setSchemaVersion(version: Int)

    // Onboarding
    suspend fun saveOnboardingState(isCompleted: Boolean)
    fun isOnboardingCompleted(): Flow<Boolean>
    suspend fun clearAllOnboardingData()

    // Сохранение данных
    suspend fun saveOnboardingAnswers(answers: OnboardingAnswers)

    // Получение данных
    fun getOnboardingAnswersFlow(): Flow<OnboardingAnswers>

    // Переключение языка в приложении
    suspend fun saveAppLanguage(languageCode: LanguageEnum) // "system", "ru", "en"
    fun getAppLanguageFlow(): Flow<String> // всегда возвращает валидный язык, по умолчанию "system"
}
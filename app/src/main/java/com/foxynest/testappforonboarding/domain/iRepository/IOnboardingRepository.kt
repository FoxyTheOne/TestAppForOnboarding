package com.foxynest.testappforonboarding.domain.iRepository

import com.foxynest.testappforonboarding.domain.entities.ErrorType
import com.foxynest.testappforonboarding.domain.entities.onboarding.OnboardingAnswers
import com.foxynest.testappforonboarding.other.Resource
import kotlinx.coroutines.flow.Flow

/**
 * Анкета на устройстве: подтверждённые ответы, черновик текущего прохождения и флаг «опрос пройден».
 * Зачем два варианта ответов – см. `OnboardingRepository`.
 */
interface IOnboardingRepository {
    fun isOnboardingCompleted(): Flow<Boolean>
    suspend fun saveOnboardingState(isCompleted: Boolean): Resource<ErrorType>
    suspend fun clearAllOnboardingData(): Resource<ErrorType>

    // Подтверждённые ответы
    suspend fun saveOnboardingAnswers(answers: OnboardingAnswers)
    fun getOnboardingAnswersFlow(): Flow<OnboardingAnswers>

    // Черновик опроса
    suspend fun startOnboardingDraft()
    suspend fun updateOnboardingDraft(transform: (OnboardingAnswers) -> OnboardingAnswers)
    fun getOnboardingDraftFlow(): Flow<OnboardingAnswers>
    suspend fun clearOnboardingDraft()
    suspend fun commitOnboardingDraft(isValid: (OnboardingAnswers) -> Boolean): OnboardingAnswers?
}
package com.foxynest.testappforonboarding.domain.use_cases.onboardingUseCases

import com.foxynest.testappforonboarding.domain.entities.ErrorType
import com.foxynest.testappforonboarding.domain.entities.onboarding.OnboardingAnswers
import com.foxynest.testappforonboarding.other.Resource
import kotlinx.coroutines.flow.Flow

interface IOnboardingUC {
    fun isOnboardingCompleted(): Flow<Boolean>
    suspend fun saveOnboardingState(isCompleted: Boolean): Resource<ErrorType>
    suspend fun clearAllOnboardingData(): Resource<ErrorType>

    suspend fun saveOnboardingAnswers(answers: OnboardingAnswers)
    fun getOnboardingAnswersFlow(): Flow<OnboardingAnswers>

    suspend fun startOnboardingDraft()
    suspend fun updateOnboardingDraft(transform: (OnboardingAnswers) -> OnboardingAnswers)
    fun getOnboardingDraftFlow(): Flow<OnboardingAnswers>
    suspend fun cancelOnboardingDraft()

    /** Переносит полностью заполненный черновик в ответы. null – черновика нет (уже сохранён) или он неполный. */
    suspend fun commitOnboardingDraft(): OnboardingAnswers?
}
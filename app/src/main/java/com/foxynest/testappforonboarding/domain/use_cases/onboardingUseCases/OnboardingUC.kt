package com.foxynest.testappforonboarding.domain.use_cases.onboardingUseCases

import com.foxynest.testappforonboarding.domain.entities.ErrorType
import com.foxynest.testappforonboarding.domain.entities.onboarding.OnboardingAnswers
import com.foxynest.testappforonboarding.domain.entities.onboarding.isValidData
import com.foxynest.testappforonboarding.domain.iRepository.IOnboardingRepository
import com.foxynest.testappforonboarding.other.Resource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Всё про анкету на устройстве: черновик, подтверждённые ответы, флаг «опрос пройден».
 * Единственная «логика» здесь – [commitOnboardingDraft]: черновик переносится, только если анкета заполнена полностью.
 */
class OnboardingUC @Inject constructor(
    private val onboardingRepository: IOnboardingRepository
) : IOnboardingUC {
    override fun isOnboardingCompleted(): Flow<Boolean> =
        onboardingRepository.isOnboardingCompleted()

    override suspend fun saveOnboardingState(isCompleted: Boolean): Resource<ErrorType> =
        onboardingRepository.saveOnboardingState(isCompleted)

    override suspend fun clearAllOnboardingData(): Resource<ErrorType> =
        onboardingRepository.clearAllOnboardingData()

    override suspend fun saveOnboardingAnswers(answers: OnboardingAnswers) =
        onboardingRepository.saveOnboardingAnswers(answers)

    override fun getOnboardingAnswersFlow(): Flow<OnboardingAnswers> =
        onboardingRepository.getOnboardingAnswersFlow()

    override suspend fun startOnboardingDraft() =
        onboardingRepository.startOnboardingDraft()

    override suspend fun updateOnboardingDraft(transform: (OnboardingAnswers) -> OnboardingAnswers) =
        onboardingRepository.updateOnboardingDraft(transform)

    override fun getOnboardingDraftFlow(): Flow<OnboardingAnswers> =
        onboardingRepository.getOnboardingDraftFlow()

    override suspend fun cancelOnboardingDraft() =
        onboardingRepository.clearOnboardingDraft()

    override suspend fun commitOnboardingDraft(): OnboardingAnswers? =
        onboardingRepository.commitOnboardingDraft { it.isValidData() }
}
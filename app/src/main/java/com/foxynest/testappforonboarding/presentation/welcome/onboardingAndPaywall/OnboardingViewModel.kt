package com.foxynest.testappforonboarding.presentation.welcome.onboardingAndPaywall

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.foxynest.testappforonboarding.R
import com.foxynest.testappforonboarding.domain.entities.onboarding.AnswerNames
import com.foxynest.testappforonboarding.domain.use_cases.onboardingUseCases.IOnboardingUC
import com.foxynest.testappforonboarding.other.MyEvent
import com.foxynest.testappforonboarding.other.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val onboardingInteractor: IOnboardingUC
) : ViewModel() {
    companion object {
        private const val TAG = "OnboardingViewModel"
    }

    // Состояние прогресса
    private val _currentStep = MutableLiveData(0)
    val currentStep: LiveData<Int> = _currentStep

    private val _totalSteps = MutableLiveData(0)
    val totalSteps: LiveData<Int> = _totalSteps

    private val _currentTitle = MutableLiveData<String>()
    val currentTitle: LiveData<String> = _currentTitle

    private val _onboardingPath = MutableLiveData<List<Int>>(emptyList())
    val onboardingPath: LiveData<List<Int>> = _onboardingPath

    // If smth went wrong
    private val _errorMessageLiveData =
        MutableLiveData<MyEvent<Resource<Boolean>>>() // It must be private, so that other classes can't change it
    val errorMessageLiveData: LiveData<MyEvent<Resource<Boolean>>> =
        _errorMessageLiveData // And another LiveData, that equals to previous, so that classes can't change it

    // Карта зависимостей для очистки ответов
    private val dependentAnswers = mutableMapOf<Int, List<AnswerNames>>().apply {
        // Если возвращаемся на Question1, очищаем ответы Question2 и Question3
        put(
            R.id.navigation_question1, listOf(
                AnswerNames.IS_IT_A_BOY_ANSWER,
                AnswerNames.CHILD_BIRTH_DATE_ANSWER,
                AnswerNames.CHILD_HOBBY_ANSWER
            )
        )
        // Если возвращаемся на Question2, очищаем ответы Question3
        put(
            R.id.navigation_question2, listOf(
                AnswerNames.CHILD_BIRTH_DATE_ANSWER,
                AnswerNames.CHILD_HOBBY_ANSWER
            )
        )
    }

    fun updateProgress(destinationId: Int) {
        val path = _onboardingPath.value?.toMutableList() ?: mutableListOf()

        // Добавляем новый шаг если его еще нет в пути
        if (!path.contains(destinationId)) {
            path.add(destinationId)
            _onboardingPath.value = path
        } else {
            // Пользователь вернулся назад - удаляем последующие шаги
            val index = path.indexOf(destinationId) + 1
            if (index < path.size) {
                path.subList(index, path.size).clear()
                _onboardingPath.value = path
            }
        }

        // Обновляем прогресс
        _totalSteps.value = calculateTotalSteps(path)
        _currentStep.value = calculateCurrentStep(destinationId, path)
    }

    fun updateTitle(title: String) {
        _currentTitle.value = title
    }

    fun getDependentAnswersToClear(currentDestination: Int): List<AnswerNames> {
        return dependentAnswers[currentDestination] ?: emptyList()
    }

    private fun calculateTotalSteps(path: List<Int>): Int {
        // Учитываем только обязательные шаги
        val mandatorySteps = listOf(
            R.id.navigation_question1,
            R.id.navigation_question3,
            R.id.navigation_paywall
        )
        return mandatorySteps.size
    }

    private fun calculateCurrentStep(destinationId: Int, path: List<Int>): Int {
        return when (destinationId) {
            R.id.navigation_question1 -> 1
            R.id.navigation_question2 -> 2
            R.id.navigation_question3 -> 2 // Если Question2 был пропущен
            R.id.navigation_paywall -> 3
            else -> 0
        }
    }

    fun getProgressText(): String {
        val current = _currentStep.value ?: 0
        val total = _totalSteps.value ?: 0
        return "$current/$total"
    }
}
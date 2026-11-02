package com.foxynest.testappforonboarding.presentation.welcome.onboardingAndPaywall

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.foxynest.testappforonboarding.R

/**
 * Заголовок и прогресс-бар опроса.
 */
class OnboardingViewModel : ViewModel() {

    /**
     * При изменении, исправить так же calculateCurrentStep()
     */
    private val mandatorySteps = listOf(
        R.id.navigation_question1,
        R.id.navigation_question3,
        R.id.navigation_question4,
        R.id.navigation_paywall
    )

    // Состояние прогресса
    private val _currentStep = MutableLiveData(0)
    val currentStep: LiveData<Int> = _currentStep

    private val _totalSteps = MutableLiveData(0)
    val totalSteps: LiveData<Int> = _totalSteps

    private val _currentTitle = MutableLiveData<String>()
    val currentTitle: LiveData<String> = _currentTitle

    fun updateProgress(destinationId: Int) {
        _totalSteps.value = mandatorySteps.size
        _currentStep.value = when (destinationId) {
            R.id.navigation_question1 -> 1
            R.id.navigation_question2 -> 2
            R.id.navigation_question3 -> 2 // Question2 есть только у мальчиков
            R.id.navigation_question4 -> 3
            R.id.navigation_paywall -> 4
            else -> 0
        }
    }

    fun updateTitle(title: String) {
        _currentTitle.value = title
    }
}
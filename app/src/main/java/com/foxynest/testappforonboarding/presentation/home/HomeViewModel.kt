package com.foxynest.testappforonboarding.presentation.home

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.IGetCurrentUserUC
import com.foxynest.testappforonboarding.domain.use_cases.onboardingUseCases.IOnboardingUC
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getCurrentUserUC: IGetCurrentUserUC,
    private val onboardingUC: IOnboardingUC
) : ViewModel() {
    companion object {
        private const val TAG = "HomeViewModel"
    }

    private val _openWelcomeFragmentLiveData = MutableLiveData<Boolean>()
    val openWelcomeFragmentLiveData: LiveData<Boolean> = _openWelcomeFragmentLiveData

    fun showWelcomeFragmentIfOnboardingIsNotComplete() {
        viewModelScope.launch(Dispatchers.IO) {
            // Проверка авторизации при запуске. Регистрация возможна только после прохождения опроса, значит если человек залогинен - опрос пройден
            val currentUser = getCurrentUserUC.execute().data

            if (currentUser != null) {

                // Пользователь зарегистрирован и вошёл, значит остаёмся на домашнем фрагменте
                Log.d(TAG, "User is logged in, staying on home fragment")

            } else {

                // Если user = null, значит not logged in
                onboardingUC.isOnboardingCompleted()
                    .take(1)
                    .collect { isCompleted ->
                        Log.d(TAG, "Проверка, пройден ли онбординг = $isCompleted")
                        if (!isCompleted) {
                            _openWelcomeFragmentLiveData.postValue(true)
                        } else {
                            // Онбординг пройден, но пользователь не залогинен
                            // Остаемся на HomeFragment с локальными данными
                            Log.d(
                                TAG,
                                "Onboarding completed but user not logged in - showing local data"
                            )
                        }
                    }

            }
        }
    }
}
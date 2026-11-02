package com.foxynest.testappforonboarding.presentation.splash

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foxynest.testappforonboarding.R
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.IIsUserLoggedInUC
import com.foxynest.testappforonboarding.domain.use_cases.migrate.IMigrateUserDataUC
import com.foxynest.testappforonboarding.domain.use_cases.onboardingUseCases.IOnboardingUC
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val migrateUserDataUC: IMigrateUserDataUC,
    private val onboardingUC: IOnboardingUC,
    private val isUserLoggedInUC: IIsUserLoggedInUC
) : ViewModel() {
    companion object {
        private const val TAG = "SplashViewModel"
    }

    // Live Data
    private val _navigateToNextLiveData = MutableLiveData<Int>()
    val navigateToNextLiveData: LiveData<Int> = _navigateToNextLiveData

    // Methods
    fun performStartupChecksAndNavigate() {
        viewModelScope.launch {
            // 1. Миграция данных (если нужна)
            try {
                migrateUserDataUC.migrateIfNeeded()
            } catch (e: Exception) {
                // Логируем ошибку, но не блокируем запуск
                Log.e("Splash", "Migration failed", e)
            }

            // 2. Определяем, пройден ли онбординг
            val isOnboardingCompleted = onboardingUC.isOnboardingCompleted().first()

            // 3. Проверяем, авторизован ли пользователь
            // Только локальное состояние входа: раньше здесь был запрос профиля в Firestore при каждом запуске
            val isLoggedIn = isUserLoggedInUC.execute()

            val targetDestination = when {
                // Проверка авторизации – первой. Раньше вошедший пользователь на новом телефоне / после переустановки
                // (локальный флаг опроса = false) при каждом запуске попадал на Welcome
                isLoggedIn -> {
                    // Онбординг завершён, пользователь авторизован → на Home
                    Log.d("Splash", "User logged in, navigate to Home")
                    R.id.action_splash_to_home
                }

                !isOnboardingCompleted -> {
                    // Онбординг не завершён → отправляем на Welcome (или на первый экран онбординга)
                    Log.d("Splash", "Onboarding not completed, navigate to Welcome")
                    R.id.action_splash_to_welcome
                }

                else -> {
                    // Онбординг завершён, но пользователь не авторизован → на Home (локальные данные)
                    Log.d("Splash", "Onboarding completed but not logged in, navigate to Home")
                    R.id.action_splash_to_home
                }
            }

            _navigateToNextLiveData.postValue(targetDestination)
        }
    }
}
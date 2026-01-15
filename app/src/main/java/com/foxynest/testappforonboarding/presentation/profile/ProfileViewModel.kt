package com.foxynest.testappforonboarding.presentation.profile

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foxynest.testappforonboarding.domain.entities.ErrorType
import com.foxynest.testappforonboarding.domain.entities.onboarding.AnswerNames
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.email.ICheckEmailVerifiedUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.email.ISendEmailVerificationUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.IGetCurrentUserUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.ILoginValidateEmailUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.ILogoutUC
import com.foxynest.testappforonboarding.domain.use_cases.updatingDataUseCases.IUpdateEmailUC
import com.foxynest.testappforonboarding.other.Resource
import com.foxynest.testappforonboarding.other.Status
import com.foxynest.testappforonboarding.other.extensions.toErrorMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val sendEmailVerificationUC: ISendEmailVerificationUC,
    private val checkEmailVerifiedUC: ICheckEmailVerifiedUC,
    private val logoutUC: ILogoutUC
) : ViewModel() {
    companion object {
        private const val TAG = "ProfileViewModel"
    }

    private val _isEmailVerificationSent = MutableStateFlow(false)
    val isEmailVerificationSent: StateFlow<Boolean> = _isEmailVerificationSent

    private val _isEmailVerified = MutableStateFlow(false)
    val isEmailVerified: StateFlow<Boolean> = _isEmailVerified

    private val _isVerificationButtonEnabled = MutableStateFlow(true)
    val isVerificationButtonEnabled: StateFlow<Boolean> = _isVerificationButtonEnabled

    // LOG OUT
    // For a toast of logging out state
    private val _logoutStateFlow = MutableStateFlow<Resource<ErrorType>>(Resource.init(null))
    val logoutStateFlow: StateFlow<Resource<ErrorType>> = _logoutStateFlow

    init {
        checkEmailVerificationStatus()
    }

    fun checkEmailVerificationStatus() {
        viewModelScope.launch {
            _isEmailVerified.value = checkEmailVerifiedUC.execute()
        }
    }

    fun sendVerificationEmail() {
        viewModelScope.launch {
            _isVerificationButtonEnabled.value = false
            _isEmailVerificationSent.value = true

            val resourceResult = sendEmailVerificationUC.execute()

            when (resourceResult.status) {
                Status.SUCCESS -> {
                    Log.d(TAG, "Status.SUCCESS = ${resourceResult.status} ")
                    // Блокируем кнопку на 60 секунд
                    delay(60000)
                    _isVerificationButtonEnabled.value = true
                    _isEmailVerificationSent.value = false
                }

                Status.ERROR -> {
                    Log.d(TAG, "Status.ERROR = ${resourceResult.status} ")
                    _isVerificationButtonEnabled.value = true
                    _isEmailVerificationSent.value = false
                }

                else -> {
                    Log.d(TAG, "resource.status = ${resourceResult.status} ")
                }
            }
        }
    }

    fun formatBirthDateForDisplay(birthDateString: String, localization: String): String {
        if (birthDateString.isBlank()) return ""

        return try {
            // Парсим из формата хранения (yyyy-MM-dd)
            val inputFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date = inputFormatter.parse(birthDateString)

            if (date != null) {
                // Форматируем для отображения (19 мая 2020)
                val outputFormatter =
                    SimpleDateFormat("d MMMM yyyy", Locale(localization))
                outputFormatter.format(date)
            } else {
                Log.e(TAG, "Error formatting birth date: $birthDateString, date = null")
                birthDateString // Возвращаем оригинальную строку в случае ошибки
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error formatting birth date: $birthDateString", e)
            birthDateString // Возвращаем оригинальную строку в случае ошибки
        }
    }

    fun logout() {
        viewModelScope.launch {
            _logoutStateFlow.value = Resource.loading(null)

            // Выходим из Firebase
            val result = logoutUC.execute()
            _logoutStateFlow.value = result
        }
    }
}
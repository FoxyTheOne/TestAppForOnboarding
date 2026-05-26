package com.foxynest.testappforonboarding.presentation.profile

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foxynest.testappforonboarding.domain.entities.ErrorType
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.email.ICheckEmailVerifiedUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.email.ISendEmailVerificationUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.ILogoutUC
import com.foxynest.testappforonboarding.other.MyEvent
import com.foxynest.testappforonboarding.other.Resource
import com.foxynest.testappforonboarding.other.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
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
        private const val VERIFICATION_CHECK_INTERVAL = 5000L // 5 секунд TODO перенести в константы
        private const val INTENSIVE_CHECK_INTERVAL = 3000L
        private const val INTENSIVE_CHECK_DURATION = 120000L // 2 минуты
        private const val BUTTON_BLOCK_DURATION = 60000L // 60 секунд
    }

    private var verificationCheckJob: Job? = null
    private var isChecking = false
    private var intensiveCheckEndTime: Long = 0
    private var lastVerificationStatus: Boolean? = null

    private val _isEmailVerificationSent = MutableStateFlow(false)
    val isEmailVerificationSent: StateFlow<Boolean> = _isEmailVerificationSent

    private val _isEmailVerified = MutableStateFlow(false)
    val isEmailVerified: StateFlow<Boolean> = _isEmailVerified

    private val _isVerificationButtonEnabled = MutableStateFlow(true)
    val isVerificationButtonEnabled: StateFlow<Boolean> = _isVerificationButtonEnabled

    // Новое LiveData для одноразового события подтверждения
    private val _emailVerifiedEvent = MutableLiveData<MyEvent<Boolean>>()
    val emailVerifiedEvent: LiveData<MyEvent<Boolean>> = _emailVerifiedEvent

    // LOG OUT
    // For a toast of logging out state
    private val _logoutStateFlow = MutableStateFlow<Resource<ErrorType>>(Resource.init(null))
    val logoutStateFlow: StateFlow<Resource<ErrorType>> = _logoutStateFlow

    init {
        // Начинаем проверку при создании ViewModel
        startVerificationCheck()
//        checkEmailVerificationStatus()
    }

    fun checkEmailVerificationStatus(force: Boolean = false) {
        if (isChecking && !force) return

//        viewModelScope.launch {
////            _isEmailVerified.value = checkEmailVerifiedUC.execute()
//
//            val isVerified = checkEmailVerifiedUC.execute()
//            if (_isEmailVerified.value != isVerified) {
//                _isEmailVerified.value = isVerified
//                Log.d(TAG, "Email verification status changed: $isVerified")
//            }
//            if (!_isEmailVerified.value) {
//                startPeriodicVerificationCheck()
//            }
//        }

        viewModelScope.launch {
            isChecking = true
            try {
                val isVerified = checkEmailVerifiedUC.execute()
                val previousStatus = lastVerificationStatus
                lastVerificationStatus = isVerified

                if (_isEmailVerified.value != isVerified) {
                    _isEmailVerified.value = isVerified
                    Log.d(TAG, "Email verification status changed: $isVerified")

                    // Отправляем событие только если статус изменился на true
                    if (isVerified && previousStatus == false) {
                        _emailVerifiedEvent.postValue(MyEvent(true))
                    }

                    // Если email подтверждён, останавливаем проверку
                    if (isVerified) {
                        stopVerificationCheck()
                        // Разблокируем кнопку
                        _isVerificationButtonEnabled.value = true
                        _isEmailVerificationSent.value = false
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking email verification", e)
            } finally {
                isChecking = false
            }
        }
    }

    fun sendVerificationEmail() {
        viewModelScope.launch {
            _isVerificationButtonEnabled.value = false
            _isEmailVerificationSent.value = false

            val resourceResult = sendEmailVerificationUC.execute()

            when (resourceResult.status) {
                Status.SUCCESS -> {
                    _isEmailVerificationSent.value = true
                    Log.d(TAG, "Status.SUCCESS = ${resourceResult.status} ")

                    // Запускаем интенсивную проверку на 2 минуты
                    startIntensiveVerificationCheck()

//                    // Блокируем кнопку на 60 секунд
//                    delay(60000)
//
//                    _isVerificationButtonEnabled.value = true
//                    _isEmailVerificationSent.value = false

                    // Блокируем кнопку на 60 секунд
                    launch {
                        delay(BUTTON_BLOCK_DURATION)
                        // Проверяем, не подтверждён ли уже email
                        if (!_isEmailVerified.value) {
                            _isVerificationButtonEnabled.value = true
//                            _isEmailVerificationSent.value = false
                        }
                    }
                }

                Status.ERROR -> {
                    Log.d(TAG, "Status.ERROR = ${resourceResult.status} ")
                    _isVerificationButtonEnabled.value = true
                }

                else -> {
                    Log.d(TAG, "resource.status = ${resourceResult.status} ")
                }
            }
        }
    }

//    fun formatBirthDateForDisplay(birthDateString: String, localization: String): String {
//        if (birthDateString.isBlank()) return ""
//
//        return try {
//            // Парсим из формата хранения (yyyy-MM-dd)
//            val inputFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
//            val date = inputFormatter.parse(birthDateString)
//
//            if (date != null) {
//                // Форматируем для отображения (19 мая 2020)
//                val outputFormatter =
//                    SimpleDateFormat("d MMMM yyyy", Locale(localization))
//                outputFormatter.format(date)
//            } else {
//                Log.e(TAG, "Error formatting birth date: $birthDateString, date = null")
//                birthDateString // Возвращаем оригинальную строку в случае ошибки
//            }
//        } catch (e: Exception) {
//            Log.e(TAG, "Error formatting birth date: $birthDateString", e)
//            birthDateString // Возвращаем оригинальную строку в случае ошибки
//        }
//    }

    fun formatBirthDateForDisplay(dateString: String, context: Context): String {
        if (dateString.isBlank()) return ""

        return try {
            // Парсим из формата хранения (yyyy-MM-dd)
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date = inputFormat.parse(dateString)

            if (date != null) {
                // Форматируем для отображения (19 мая 2020)
                val outputFormat = android.text.format.DateFormat.getDateFormat(context)
                outputFormat.format(date)
            } else {
                Log.e(TAG, "Error formatting birth date: $dateString, date = null")
                dateString // Возвращаем оригинальную строку в случае ошибки
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error formatting birth date: $dateString", e)
            dateString // Возвращаем оригинальную строку в случае ошибки
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

//    private fun startPeriodicVerificationCheck() {
//        stopPeriodicVerificationCheck()
//
//        verificationCheckJob = viewModelScope.launch {
//            while (isActive) {
//                if (_isEmailVerified.value) return@launch
//                checkEmailVerificationStatus()
//                delay(VERIFICATION_CHECK_INTERVAL)
//            }
//        }
//    }
//
//    private fun stopPeriodicVerificationCheck() {
//        verificationCheckJob?.cancel()
//        verificationCheckJob = null
//    }

    private fun startVerificationCheck() {
        // Останавливаем предыдущую проверку, если есть
        stopVerificationCheck()

        verificationCheckJob = viewModelScope.launch {
            while (isActive && !_isEmailVerified.value) {
                checkEmailVerificationStatus()

                // Определяем интервал проверки
                val interval = if (System.currentTimeMillis() < intensiveCheckEndTime) {
                    INTENSIVE_CHECK_INTERVAL
                } else {
                    VERIFICATION_CHECK_INTERVAL
                }

                delay(interval)
            }
        }
    }


//    private fun startIntensiveVerificationCheck() {
//        viewModelScope.launch {
//            // Первые 2 минуты проверяем каждые 3 секунды
//            repeat(40) { // 120 секунд / 3 = 40 раз
//                delay(3000L)
//                checkEmailVerificationStatus()
//
//                // Если email подтверждён, останавливаем проверку
//                if (_isEmailVerified.value) {
//                    return@launch
//                }
//            }
//
//            // Возвращаемся к обычной проверке
//            _isVerificationButtonEnabled.value = true
//            _isEmailVerificationSent.value = false
//        }
//    }

    private fun startIntensiveVerificationCheck() {
        intensiveCheckEndTime = System.currentTimeMillis() + INTENSIVE_CHECK_DURATION
        // Перезапускаем проверку с новым интервалом
        startVerificationCheck()
    }

    private fun stopVerificationCheck() {
        verificationCheckJob?.cancel()
        verificationCheckJob = null
    }

    override fun onCleared() {
        stopVerificationCheck()
        super.onCleared()
    }
}
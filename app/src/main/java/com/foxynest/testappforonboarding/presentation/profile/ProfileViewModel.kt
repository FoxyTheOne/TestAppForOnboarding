package com.foxynest.testappforonboarding.presentation.profile

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foxynest.testappforonboarding.domain.entities.language.LanguageEnum
import com.foxynest.testappforonboarding.domain.use_cases.appLanguage.IGetAppLanguageUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.email.ICheckEmailVerifiedUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.email.ISendEmailVerificationUC
import com.foxynest.testappforonboarding.other.MyEvent
import com.foxynest.testappforonboarding.other.Status
import com.foxynest.testappforonboarding.other.extensions.parseDateToLocalDate
import com.foxynest.testappforonboarding.presentation.language.LanguageManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject

/**
 * ViewModel экрана профиля: язык интерфейса и подтверждение почты.
 *
 * Подтверждение почты «мягкое»: плашка с кнопкой, пользоваться приложением можно и без подтверждения.
 * Проверка статуса: разово при каждом открытии экрана (onResume) и часто – только 2 минуты после отправки письма
 * (человек как раз идёт в почту по ссылке). Бесконечная частая проверка тратила бы батарею и запросы к Firebase.
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val sendEmailVerificationUC: ISendEmailVerificationUC,
    private val checkEmailVerifiedUC: ICheckEmailVerifiedUC,
    private val getLanguageUC: IGetAppLanguageUC,
    private val languageManager: LanguageManager
) : ViewModel() {
    companion object {
        private const val TAG = "ProfileViewModel"
        private const val INTENSIVE_CHECK_INTERVAL = 3000L
        private const val INTENSIVE_CHECK_DURATION = 120000L // 2 минуты
        private const val BUTTON_BLOCK_DURATION = 60000L // 60 секунд
    }

    private var verificationCheckJob: Job? = null
    private var isChecking = false

    // null – ещё не проверяли: не показываем плашку «почта не подтверждена», пока не узнаем точно
    private val _isEmailVerified = MutableStateFlow<Boolean?>(null)
    val isEmailVerified: StateFlow<Boolean?> = _isEmailVerified

    private val _isVerificationButtonEnabled = MutableStateFlow(true)
    val isVerificationButtonEnabled: StateFlow<Boolean> = _isVerificationButtonEnabled

    // LiveData для одноразового события подтверждения
    private val _emailVerifiedEvent = MutableLiveData<MyEvent<Boolean>>()
    val emailVerifiedEvent: LiveData<MyEvent<Boolean>> = _emailVerifiedEvent

    // For a toast of logging out state
    // Событие, а не состояние: иначе тост «письмо отправлено» повторялся при каждом возврате на экран
    private val _verificationEmailSentEvent = MutableLiveData<MyEvent<Unit>>()
    val verificationEmailSentEvent: LiveData<MyEvent<Unit>> = _verificationEmailSentEvent

    private val _currentLanguageFlow = MutableStateFlow<LanguageEnum?>(null)
    val currentLanguageFlow: StateFlow<LanguageEnum?> = _currentLanguageFlow.asStateFlow()

    init {
        // Проверка почты – разово в onResume экрана и часто только 2 минуты после отправки письма
        viewModelScope.launch { _currentLanguageFlow.value = getLanguageUC() }
    }

    fun changeLanguage(language: LanguageEnum) {
        viewModelScope.launch {
            languageManager.setLanguage(language)
            _currentLanguageFlow.value = language
        }
    }

    fun checkEmailVerificationStatus() {
        if (isChecking) return
        isChecking = true

        viewModelScope.launch {
            try {
                val isVerified = checkEmailVerifiedUC.execute()
                val previousStatus = _isEmailVerified.value
                _isEmailVerified.value = isVerified

                if (isVerified && previousStatus == false) {
                    _emailVerifiedEvent.value = MyEvent(true)
                }
                if (isVerified) {
                    stopVerificationCheck()
                    _isVerificationButtonEnabled.value = true
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

            val result = sendEmailVerificationUC.execute()
            if (result.status == Status.SUCCESS) {
                _verificationEmailSentEvent.value = MyEvent(Unit)
                startIntensiveVerificationCheck()

                // Блокируем кнопку на 60 секунд, чтобы не упереться в лимит писем Firebase
                delay(BUTTON_BLOCK_DURATION)
                if (_isEmailVerified.value != true) {
                    _isVerificationButtonEnabled.value = true
                }
            } else {
                _isVerificationButtonEnabled.value = true
            }
        }
    }

    fun formatBirthDateForDisplay(dateString: String, context: Context): String {
        val date = dateString.parseDateToLocalDate() ?: return dateString
        val locale = context.resources.configuration.locales[0]
        return date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
    }

    private fun startIntensiveVerificationCheck() {
        stopVerificationCheck()

        verificationCheckJob = viewModelScope.launch {
            // Фиксированное число проверок, а не сравнение с часами: не зависит от смены системного времени
            repeat((INTENSIVE_CHECK_DURATION / INTENSIVE_CHECK_INTERVAL).toInt()) {
                delay(INTENSIVE_CHECK_INTERVAL)
                if (!isActive || _isEmailVerified.value == true) return@launch
                checkEmailVerificationStatus()
            }
        }
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
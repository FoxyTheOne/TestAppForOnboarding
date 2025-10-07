package com.foxynest.testappforonboarding.presentation.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.email.ICheckEmailVerifiedUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.email.ISendEmailVerificationUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.IGetCurrentUserUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.ILogoutUC
import com.foxynest.testappforonboarding.other.Resource
import com.foxynest.testappforonboarding.other.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getCurrentLogInStateUC: IGetCurrentUserUC,
//    private val updateProfileUC: IUpdateProfileUseCase,
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
    private val _logoutStateFlow = MutableStateFlow<Resource<Unit>>(Resource.init(null))
    val logoutStateFlow: StateFlow<Resource<Unit>> = _logoutStateFlow

    init {
//        loadUser()
        checkEmailVerificationStatus()
    }

    // TODO in profile fragment при инициализации вызываем метод из main view model для проверки этого статуса и затем подписываемся на liveData
//    private fun loadUser() {
//        viewModelScope.launch(Dispatchers.IO) {
//            _userState.value = getCurrentUserUseCase()
//        }
//    }

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

    fun logout() {
        viewModelScope.launch {
            _logoutStateFlow.value = Resource.loading(null)

            _logoutStateFlow.value = logoutUC.execute()
        }
    }
}
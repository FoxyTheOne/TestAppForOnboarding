package com.foxynest.testappforonboarding.presentation.welcome.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.ICheckRegisteredEmailAndPasswordUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.ILoginUC
import com.foxynest.testappforonboarding.other.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * A place for using usecases!
 * Usecases contains the buisness logic (like the validation logic: is the password longer than 8 caracters or not, is email validated and so on)
 * A view model uses those usecases and map the results to compose state.
 * So it will call the validated email use cse when it checks - was the validation actually successful?
 * If yes than it will change the state we have here correspondingly so that it is correctly displayed in the ui with the error message or that the error message is hidden if there is no error
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val checkRegisteredEmailAndPasswordUC: ICheckRegisteredEmailAndPasswordUC,
    private val loginUC: ILoginUC
) : ViewModel() {
    companion object {
        private const val TAG = "LoginViewModel"
    }

    // _state для сохранения изменений в нашей форме для входа
    private val _state = MutableStateFlow(LoginFormState())
    val state: StateFlow<LoginFormState> = _state.asStateFlow()

//    // To send events to the channel. UI can collect this changes by collecting from the flow
//    private val checkRegistrationEventChannel = Channel<CheckRegistrationEvent>()
//    val checkRegistrationEvents = checkRegistrationEventChannel.receiveAsFlow()

    // LOG IN
    // For a toast of logging in state
    private val _loginStateFlow = MutableStateFlow<Resource<Unit>>(Resource.init(null))
    val loginStateFlow: StateFlow<Resource<Unit>> = _loginStateFlow

    fun onEvent(event: LoginFormEvent) {
        when (event) {
            is LoginFormEvent.EmailChanged -> {
                // We're making a copy but changing an email
                _state.value = _state.value.copy(email = event.email)
            }

            is LoginFormEvent.PasswordChanged -> {
                _state.value = _state.value.copy(password = event.password)
            }

            is LoginFormEvent.Submit -> {
                submitData()
            }
        }
    }

    private fun submitData() {
        // Всё, что мы должны передать при подтверждении формы регистрации:
        val currentState = _state.value
        val emailAndPasswordResult = checkRegisteredEmailAndPasswordUC.execute(
            currentState.password, currentState.password
        )

        // Заменяем поля
        _state.value = currentState.copy(
            emailError = emailAndPasswordResult.errorMessage,
            passwordError = emailAndPasswordResult.errorMessage
        )

        // We need to know, was there an error or not
        val hasError = listOf(
            emailAndPasswordResult
        ).any { !it.successful }

        // Let's check
        if (hasError) {
            return
        }

//        // Now we need to send events from view model to our ui (not from ui to view model) so we use a channel
//        viewModelScope.launch {
//            checkRegistrationEventChannel.send(CheckRegistrationEvent.SuccessCheckRegistration)
//        }

        // Success validation, begin of logging in
        viewModelScope.launch {
            _loginStateFlow.value = Resource.loading(null)

            _loginStateFlow.value =
                loginUC.execute(currentState.email, currentState.password)
        }
    }

//    sealed class CheckRegistrationEvent {
//        // Here can be others events
//        object SuccessCheckRegistration : CheckRegistrationEvent()
//    }
}
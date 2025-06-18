package com.foxynest.testappforonboarding.presentation.welcome.auth.registration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.register.IValidateEmailUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.register.IValidatePasswordUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.register.IValidateRepeatedPasswordUC
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
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
class RegistrationViewModel @Inject constructor(
    private val validateEmailInteractor: IValidateEmailUC,
    private val validatePasswordInteractor: IValidatePasswordUC,
    private val validateRepeatedPasswordInteractor: IValidateRepeatedPasswordUC
) : ViewModel() {
    companion object {
        private const val TAG = "AuthViewModel"
    }

    // _state для сохранения изменений в нашей форме для регистрации
    private val _state = MutableStateFlow(RegistrationFormState())
    val state: StateFlow<RegistrationFormState> = _state.asStateFlow()

    // To send events to the channel. UI can collect this changes by collecting from the flow
    private val validationEventChannel = Channel<ValidationEvent>()
    val validationEvents = validationEventChannel.receiveAsFlow()

    fun onEvent(event: RegistrationFormEvent) {
        when (event) {
            is RegistrationFormEvent.EmailChanged -> {
                // We're making a copy but changing an email
                _state.value = _state.value.copy(email = event.email)
            }

            is RegistrationFormEvent.PasswordChanged -> {
                _state.value = _state.value.copy(password = event.password)
            }

            is RegistrationFormEvent.RepeatedPasswordChanged -> {
                _state.value = _state.value.copy(repeatedPassword = event.repeatedPassword)
            }

            is RegistrationFormEvent.Submit -> {
                submitData()
            }
        }
    }

    private fun submitData() {
        // Всё, что мы должны передать при подтверждении формы регистрации:
        val currentState = _state.value
        val emailResult = validateEmailInteractor.execute(currentState.email)
        val passwordResult = validatePasswordInteractor.execute(currentState.password)
        val repeatedPasswordResult = validateRepeatedPasswordInteractor.execute(
            currentState.password, currentState.repeatedPassword
        )

        // Заменяем поля
        _state.value = currentState.copy(
            emailError = emailResult.errorMessage,
            passwordError = passwordResult.errorMessage,
            repeatedPasswordError = repeatedPasswordResult.errorMessage
        )

        // We need to know, was there an error or not
        val hasError = listOf(
            emailResult,
            passwordResult,
            repeatedPasswordResult
        ).any { !it.successful }

        // Let's check
        if (hasError) {
            return
        }

        // Now we need to send events from view model to our ui (not from ui to view model) so we use a channel
        viewModelScope.launch {
            validationEventChannel.send(ValidationEvent.SuccessValidation)
        }
    }

    sealed class ValidationEvent {
        // Here can be others events
        object SuccessValidation : ValidationEvent()
    }
}
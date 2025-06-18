package com.foxynest.testappforonboarding.presentation.welcome.auth.registration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.ValidateEmailUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.ValidatePasswordUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.ValidateRepeatedPasswordUC
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
 * So it will call the validated email use cse when it checks  was the validation actually successful?
 * If yes than it will change the state we have here correspondingly so that it is correctly displayed in the ui with the error message or that the error message is hidden if there is no error
 */
@HiltViewModel
class RegistrationViewModel @Inject constructor(
    private val validateEmailInteractor: ValidateEmailUC,
    private val validatePasswordInteractor: ValidatePasswordUC,
    private val validateRepeatedPasswordInteractor: ValidateRepeatedPasswordUC
) : ViewModel() {
    companion object {
        private const val TAG = "AuthViewModel"
    }

    // Заменяем mutableStateOf на MutableStateFlow
    private val _state = MutableStateFlow(RegistrationFormState())
    val state: StateFlow<RegistrationFormState> = _state.asStateFlow()
//    var state by mutableStateOf(RegistrationFormState())

    // To send events to the channel. UI can collect this changes by collecting from the flow
    private val validationEventChannel = Channel<ValidationEvent>()
    val validationEvents = validationEventChannel.receiveAsFlow()

    fun onEvent(event: RegistrationFormEvent) {
        when (event) {
            is RegistrationFormEvent.EmailChanged -> {
                // We're making a copy but changing an email
                _state.value = _state.value.copy(email = event.email)
//                state = state.copy(email = event.email)
            }

            is RegistrationFormEvent.PasswordChanged -> {
                _state.value = _state.value.copy(password = event.password)
//                state = state.copy(password = event.password)
            }

            is RegistrationFormEvent.RepeatedPasswordChanged -> {
                _state.value = _state.value.copy(repeatedPassword = event.repeatedPassword)
//                state = state.copy(repeatedPassword = event.repeatedPassword)
            }
//            is RegistrationFormEvent.AcceptTerms -> {
//                state = state.copy(acceptedTerms = event.isAccepted)
//            }
            is RegistrationFormEvent.Submit -> {
                submitData()
            }
        }
    }

    private fun submitData() {
        // Всё, что мы должны передать при подтверждении формы регистрации:
//        val emailResult = validateEmailInteractor.execute(state.email)
//        val passwordResult = validatePasswordInteractor.execute(state.password)
//        val repeatedPasswordResult = validateRepeatedPasswordInteractor.execute(
//            state.password, state.repeatedPassword
//        )
////        val termsResult = validateTerms.execute(state.acceptedTerms)
        val currentState = _state.value
        val emailResult = validateEmailInteractor.execute(currentState.email)
        val passwordResult = validatePasswordInteractor.execute(currentState.password)
        val repeatedPasswordResult = validateRepeatedPasswordInteractor.execute(
            currentState.password, currentState.repeatedPassword
        )

        // We need to know, was there an error or not
        val hasError = listOf(
            emailResult,
            passwordResult,
            repeatedPasswordResult
//            termsResult
        ).any { !it.successful }

        // Let's check
        // Заменяем поля
//            state = state.copy(
//                emailError = emailResult.errorMessage,
//                passwordError = passwordResult.errorMessage,
//                repeatedPasswordError = repeatedPasswordResult.errorMessage
////                termsError = termsResult.errorMessage
//            )
        _state.value = currentState.copy(
            emailError = emailResult.errorMessage,
            passwordError = passwordResult.errorMessage,
            repeatedPasswordError = repeatedPasswordResult.errorMessage
        )
        if (hasError) {
            return
        }

        // Now we need to send events from view model to our ui (not from ui to view model) so we use a channel
        viewModelScope.launch {
            validationEventChannel.send(ValidationEvent.Success)
        }
    }

    sealed class ValidationEvent {
        // Here can be others events
        object Success : ValidationEvent()
    }
}
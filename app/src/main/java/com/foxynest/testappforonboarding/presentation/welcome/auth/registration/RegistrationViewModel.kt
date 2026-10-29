package com.foxynest.testappforonboarding.presentation.welcome.auth.registration

import android.content.Context
import android.content.res.Resources
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foxynest.testappforonboarding.domain.entities.ErrorType
import com.foxynest.testappforonboarding.domain.entities.user.UserPresentation
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.register.IRegisterUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.register.IRegisterValidateEmailUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.register.IRegisterValidatePasswordUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.register.IRegisterValidateRepeatedPasswordUC
import com.foxynest.testappforonboarding.other.Resource
import com.foxynest.testappforonboarding.other.Status
import com.foxynest.testappforonboarding.other.extensions.toErrorMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
class RegistrationViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val validateEmailInteractor: IRegisterValidateEmailUC,
    private val validatePasswordInteractor: IRegisterValidatePasswordUC,
    private val validateRepeatedPasswordInteractor: IRegisterValidateRepeatedPasswordUC,
    private val registerUC: IRegisterUC
) : ViewModel() {
    companion object {
        private const val TAG = "AuthViewModel"
    }

    private val _user = MutableStateFlow<UserPresentation?>(null)
    fun setUser(user: UserPresentation) {
        _user.value = user
    }

    // _state для сохранения изменений в нашей форме для регистрации
    private val _state = MutableStateFlow(RegistrationFormState())
    val state: StateFlow<RegistrationFormState> = _state.asStateFlow()

    // REGISTER
    // For a toast of registration state
    private val _registrationStateFlow = MutableStateFlow<Resource<ErrorType>>(Resource.init(null))
    val registrationStateFlow: StateFlow<Resource<ErrorType>> = _registrationStateFlow

    private val resources: Resources
        get() = context.resources

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
        val currentState = _state.value

        // Валидация. Всё, что мы должны передать при подтверждении формы регистрации:
        val emailResult = validateEmailInteractor.execute(currentState.email)
        val passwordResult = validatePasswordInteractor.execute(currentState.password)
        val repeatedPasswordResult = validateRepeatedPasswordInteractor.execute(
            currentState.password, currentState.repeatedPassword
        )

        _state.value = currentState.copy(
            emailError = emailResult.errorMessage.toErrorMessage(resources),
            passwordError = passwordResult.errorMessage.toErrorMessage(resources),
            repeatedPasswordError = repeatedPasswordResult.errorMessage.toErrorMessage(resources)
        )

        // Проверяем наличие ошибок валидации
        val hasError = listOf(
            emailResult,
            passwordResult,
            repeatedPasswordResult
        ).any { !it.successful }

        // Let's check
        if (hasError) {
            return
        }

        // Получаем переданный из профиля аргумент (выходим из метода, если он = null)
        val user = _user.value ?: run {
            _registrationStateFlow.value = Resource.error("User data not loaded", null)
            return
        }

        // Success validation, begin of registration
        viewModelScope.launch {
            _registrationStateFlow.value = Resource.loading(null)

            val result = registerUC.execute(currentState.email, currentState.password, user)
            _registrationStateFlow.value = result

            // ДОБАВЛЯЕМ: Обновляем состояние формы при ошибке
            if (result.status == Status.ERROR) {
                // Определяем какое поле нужно подсветить ошибкой
                _state.value = _state.value.copy(
                    emailError = result.data.toErrorMessage(resources),
                    passwordError = result.data.toErrorMessage(resources),
                    repeatedPasswordError = result.data.toErrorMessage(resources)
                )
            }
        }
    }
}
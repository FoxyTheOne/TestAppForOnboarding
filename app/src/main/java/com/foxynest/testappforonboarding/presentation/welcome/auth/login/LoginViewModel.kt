package com.foxynest.testappforonboarding.presentation.welcome.auth.login

import android.content.Context
import android.content.res.Resources
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foxynest.testappforonboarding.domain.entities.ErrorType
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.ILoginUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.ILoginValidateEmailUC
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login.ILoginValidatePasswordUC
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
class LoginViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val loginUC: ILoginUC,
    private val loginValidateEmailUC: ILoginValidateEmailUC,
    private val loginValidatePasswordUC: ILoginValidatePasswordUC
) : ViewModel() {
    companion object {
        private const val TAG = "LoginViewModel"
    }

    // _state для сохранения изменений в нашей форме для входа
    private val _state = MutableStateFlow(LoginFormState())
    val state: StateFlow<LoginFormState> = _state.asStateFlow()

    // LOG IN
    // For a toast of logging in state
    private val _loginStateFlow = MutableStateFlow<Resource<ErrorType>>(Resource.init(null))
    val loginStateFlow: StateFlow<Resource<ErrorType>> = _loginStateFlow

    // Computed property для resources
    private val resources: Resources
        get() = context.resources

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
        val currentState = _state.value

        // БАЗОВАЯ валидация - только проверка на пустоту и формат
        val emailResult = loginValidateEmailUC.execute(currentState.email)
        val passwordResult = loginValidatePasswordUC.execute(currentState.password)

        // Обновляем состояние с ошибками (заменяем поля)
        _state.value = currentState.copy(
            emailError = emailResult.errorMessage.toErrorMessage(resources),
            passwordError = passwordResult.errorMessage.toErrorMessage(resources)
        )

        // Проверяем наличие ошибок валидации
        val hasError = listOf(
            emailResult,
            passwordResult
        ).any { !it.successful }

        // Let's check
        if (hasError) {
            return
        }

        // Success validation, begin of logging in
        viewModelScope.launch {
            _loginStateFlow.value = Resource.loading(null)

            val result = loginUC.execute(currentState.email.lowercase().trim(), currentState.password)
            _loginStateFlow.value = result

            // ДОБАВЛЯЕМ: Обновляем состояние формы при ошибке
            if (result.status == Status.ERROR) {
                // Определяем какое поле нужно подсветить ошибкой
                _state.value = _state.value.copy(
                    emailError = result.data.toErrorMessage(resources),
                    passwordError = result.data.toErrorMessage(resources)
                )
            }
        }
    }
}
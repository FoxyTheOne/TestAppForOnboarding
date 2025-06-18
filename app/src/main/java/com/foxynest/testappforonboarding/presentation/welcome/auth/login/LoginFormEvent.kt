package com.foxynest.testappforonboarding.presentation.welcome.auth.login

sealed class LoginFormEvent {
    data class EmailChanged(val email: String) : LoginFormEvent()
    data class PasswordChanged(val password: String) : LoginFormEvent()
    object Submit : LoginFormEvent() // When we click on the button
}
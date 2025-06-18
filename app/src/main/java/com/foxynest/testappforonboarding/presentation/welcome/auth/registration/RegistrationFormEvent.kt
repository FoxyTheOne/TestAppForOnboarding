package com.foxynest.testappforonboarding.presentation.welcome.auth.registration

sealed class RegistrationFormEvent {
    data class EmailChanged(val email: String) : RegistrationFormEvent()
    data class PasswordChanged(val password: String) : RegistrationFormEvent()
    data class RepeatedPasswordChanged(val repeatedPassword: String) : RegistrationFormEvent()
    object Submit : RegistrationFormEvent() // When we click on the button
}
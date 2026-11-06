package com.foxynest.testappforonboarding.presentation.welcome.auth.login

/**
 * Что пользователь сделал в форме входа. Экран не меняет состояние сам, а сообщает ViewModel событие –
 * ViewModel решает, что с ним делать (паттерн «события вверх, состояние вниз»).
 * `sealed` – все варианты перечислены здесь, и `when` по ним компилятор проверит на полноту.
 */
sealed class LoginFormEvent {
    data class EmailChanged(val email: String) : LoginFormEvent()
    data class PasswordChanged(val password: String) : LoginFormEvent()
    object Submit : LoginFormEvent() // When we click on the button
}
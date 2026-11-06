package com.foxynest.testappforonboarding.domain.entities

/**
 * Тип ошибки domain-слоя. Текст для пользователя подбирается в presentation (`presentation/common/DomainDisplay.kt`),
 * чтобы domain не зависел от Android-ресурсов.
 *
 * Почему тип, а не готовый текст: текст зависит от языка, а язык знает только экран (его Context).
 * Если ViewModel сама возьмёт строку из ресурсов приложения, на Android 12 и ниже текст может прийти
 * на системном языке вместо выбранного в приложении.
 *
 * Новая ошибка: добавить сюда + строку в `values/strings.xml` и `values-ru/strings.xml` + ветку в DomainDisplay
 * (компилятор сам напомнит: `when` там без `else`).
 */
enum class ErrorType {
    EMAIL_EMPTY_ERROR,
    EMAIL_FORMAT_ERROR,
    EMAIL_ALREADY_REGISTERED_ERROR,
    PASSWORD_EMPTY_ERROR,
    PASSWORD_LETTERS_ERROR,
    PASSWORD_LENGTH_ERROR_NEEDS_MIN_PASSWORD_LENGTH,
    PASSWORD_MATCH_ERROR,
    PASSWORD_TOO_WEAK_ERROR,
    TERMS_ERROR,
    FIELD_EMPTY_ERROR,
    AUTH_DATA_ERROR,
    TOO_MANY_ATTEMPTS_ERROR,
    LOGIN_FAILED_ERROR,
    REGISTER_FAILED_ERROR,
    LOGOUT_FAILED_ERROR,
    NOT_AUTHENTICATED_ERROR,
    INTERNET_ERROR,
    DATA_NOT_SAVED_ERROR,
    SENDING_EMAIL_VERIFICATION_ERROR,
    UNEXPECTED_ERROR,
    FAILED_TO_SAVE_ONBOARDING_STATE_LOCAL,
    FAILED_TO_CLEAR_DATA,
    NOT_VALID_USER_DATA_FOR_SAVING_TO_FIREBASE,
    WRONG_PASSWORD_WHILE_CHANGING_EMAIL_ERROR,
    UNSYNCED_DATA_LOGOUT_ERROR,
    WRONG_PASSWORD_ERROR,
    ACCOUNT_DELETION_FAILED_ERROR,
    PASSWORD_RESET_FAILED_ERROR,
    NAME_INVALID_ERROR
}
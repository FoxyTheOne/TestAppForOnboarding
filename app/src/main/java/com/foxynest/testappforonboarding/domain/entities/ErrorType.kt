package com.foxynest.testappforonboarding.domain.entities

import com.foxynest.testappforonboarding.R

enum class ErrorType(val stringRes: Int) {
    EMAIL_EMPTY_ERROR(R.string.email_empty_error),
    EMAIL_FORMAT_ERROR(R.string.email_format_error),
    EMAIL_LENGTH_ERROR_NEEDS_MIN_PASSWORD_LENGTH(R.string.email_length_error),
    EMAIL_ALREADY_REGISTERED_ERROR(R.string.email_already_registered_error),
    PASSWORD_EMPTY_ERROR(R.string.password_empty_error),
    PASSWORD_LETTERS_ERROR(R.string.password_letters_error),
    PASSWORD_MATCH_ERROR(R.string.password_match_error),
    PASSWORD_TOO_WEAK_ERROR(R.string.password_too_weak_error),
    TERMS_ERROR(R.string.terms_error),
    FIELD_EMPTY_ERROR(R.string.field_empty_error),
    AUTH_DATA_ERROR(R.string.auth_data_error),
    TOO_MANY_ATTEMPTS_ERROR(R.string.too_many_attempts_error),
    LOGIN_FAILED_ERROR(R.string.login_failed_error),
    REGISTER_FAILED_ERROR(R.string.register_failed_error),
    LOGOUT_FAILED_ERROR(R.string.logout_failed_error),
    NOT_AUTHENTICATED_ERROR(R.string.not_authenticated_error),
    INTERNET_ERROR(R.string.internet_error),
    DATA_NOT_SAVED_ERROR(R.string.data_not_saved_error),
    SENDING_EMAIL_VERIFICATION_ERROR(R.string.sending_email_verification_error)
}
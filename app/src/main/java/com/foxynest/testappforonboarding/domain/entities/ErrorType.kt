package com.foxynest.testappforonboarding.domain.entities

import com.foxynest.testappforonboarding.R

enum class ErrorType(val stringRes: Int) {
    EMAIL_EMPTY_ERROR(R.string.email_empty_error),
    EMAIL_FORMAT_ERROR(R.string.email_format_error),
    EMAIL_LENGTH_ERROR_NEEDS_MIN_PASSWORD_LENGTH(R.string.email_length_error),
    PASSWORD_EMPTY_ERROR(R.string.password_empty_error),
    PASSWORD_LETTERS_ERROR(R.string.password_letters_error),
    PASSWORD_MATCH_ERROR(R.string.password_match_error),
    TERMS_ERROR(R.string.terms_error)
}
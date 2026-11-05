package com.foxynest.testappforonboarding.presentation.common

import android.content.Context
import android.content.res.Resources
import androidx.annotation.StringRes
import com.foxynest.testappforonboarding.R
import com.foxynest.testappforonboarding.domain.entities.ErrorType
import com.foxynest.testappforonboarding.domain.entities.Gender
import com.foxynest.testappforonboarding.domain.entities.onboarding.YesNoAnswer
import com.foxynest.testappforonboarding.other.constants.AuthConstants.MIN_PASSWORD_LENGTH

/**
 * Отображение domain-сущностей в UI. Всё, что зависит от Android-ресурсов, живёт здесь, а не в domain.
 */

@StringRes
private fun ErrorType.messageRes(): Int = when (this) {
    ErrorType.EMAIL_EMPTY_ERROR -> R.string.email_empty_error
    ErrorType.EMAIL_FORMAT_ERROR -> R.string.email_format_error
    ErrorType.EMAIL_ALREADY_REGISTERED_ERROR -> R.string.email_already_registered_error
    ErrorType.PASSWORD_EMPTY_ERROR -> R.string.password_empty_error
    ErrorType.PASSWORD_LETTERS_ERROR -> R.string.password_letters_error
    ErrorType.PASSWORD_LENGTH_ERROR_NEEDS_MIN_PASSWORD_LENGTH -> R.string.email_length_error
    ErrorType.PASSWORD_MATCH_ERROR -> R.string.password_match_error
    ErrorType.PASSWORD_TOO_WEAK_ERROR -> R.string.password_too_weak_error
    ErrorType.TERMS_ERROR -> R.string.terms_error
    ErrorType.FIELD_EMPTY_ERROR -> R.string.field_empty_error
    ErrorType.AUTH_DATA_ERROR -> R.string.auth_data_error
    ErrorType.TOO_MANY_ATTEMPTS_ERROR -> R.string.too_many_attempts_error
    ErrorType.LOGIN_FAILED_ERROR -> R.string.login_failed_error
    ErrorType.REGISTER_FAILED_ERROR -> R.string.register_failed_error
    ErrorType.LOGOUT_FAILED_ERROR -> R.string.logout_failed_error
    ErrorType.NOT_AUTHENTICATED_ERROR -> R.string.not_authenticated_error
    ErrorType.INTERNET_ERROR -> R.string.internet_error
    ErrorType.DATA_NOT_SAVED_ERROR -> R.string.data_not_saved_error
    ErrorType.SENDING_EMAIL_VERIFICATION_ERROR -> R.string.sending_email_verification_error
    ErrorType.UNEXPECTED_ERROR -> R.string.unexpected_error
    ErrorType.FAILED_TO_SAVE_ONBOARDING_STATE_LOCAL -> R.string.failed_to_save_onboarding_state_local
    ErrorType.FAILED_TO_CLEAR_DATA -> R.string.failed_to_clear_data
    ErrorType.NOT_VALID_USER_DATA_FOR_SAVING_TO_FIREBASE -> R.string.not_valid_user_data_for_saving_to_firebase
    ErrorType.WRONG_PASSWORD_WHILE_CHANGING_EMAIL_ERROR,
    ErrorType.WRONG_PASSWORD_ERROR -> R.string.wrong_password_while_changing_email

    ErrorType.UNSYNCED_DATA_LOGOUT_ERROR -> R.string.unsynced_data_logout_error
    ErrorType.ACCOUNT_DELETION_FAILED_ERROR -> R.string.account_deletion_failed_error
    ErrorType.PASSWORD_RESET_FAILED_ERROR -> R.string.password_reset_failed_error
    ErrorType.NAME_INVALID_ERROR -> R.string.error_name_invalid
}

fun ErrorType?.toErrorMessage(resources: Resources): String? = when (this) {
    null -> null
    ErrorType.PASSWORD_LENGTH_ERROR_NEEDS_MIN_PASSWORD_LENGTH ->
        resources.getString(messageRes(), MIN_PASSWORD_LENGTH)

    else -> resources.getString(messageRes())
}

fun Gender.displayName(context: Context): String = when (this) {
    Gender.BOY -> context.getString(R.string.text_boy)
    Gender.GIRL -> context.getString(R.string.text_girl)
}

/** Неизвестное значение (например, из старой версии) показываем как есть. */
fun yesNoDisplayName(context: Context, storedValue: String): String =
    when (YesNoAnswer.fromStoredValue(storedValue)) {
        YesNoAnswer.YES -> context.getString(R.string.yes)
        YesNoAnswer.NO -> context.getString(R.string.no)
        null -> storedValue
    }

object HobbyDisplay {
    private val hobbyKeyToResId = mapOf(
        "drawing" to R.string.hobby_drawing,
        "construction" to R.string.hobby_construction,
        "musical_instruments" to R.string.hobby_musical_instruments,
        "football" to R.string.hobby_football,
        "books" to R.string.hobby_books,
        "learning_languages" to R.string.hobby_learning_languages,
        "math" to R.string.hobby_math,
        "chess" to R.string.hobby_chess,
        "swimming" to R.string.hobby_swimming,
        "martial_arts" to R.string.hobby_martial_arts,
        "choreography" to R.string.hobby_choreography
    )

    fun displayName(context: Context, key: String): String =
        hobbyKeyToResId[key]?.let(context::getString) ?: key
}
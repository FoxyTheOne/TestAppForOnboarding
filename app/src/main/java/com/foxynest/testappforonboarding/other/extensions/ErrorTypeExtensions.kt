package com.foxynest.testappforonboarding.other.extensions

import android.content.res.Resources
import com.foxynest.testappforonboarding.domain.entities.ErrorType
import com.foxynest.testappforonboarding.other.constants.AuthConstants.MIN_PASSWORD_LENGTH

fun ErrorType?.toErrorMessage(resources: Resources): String? {
    return when (this) {
        ErrorType.PASSWORD_LENGTH_ERROR_NEEDS_MIN_PASSWORD_LENGTH ->
            resources.getString(this.stringRes, MIN_PASSWORD_LENGTH)
        null -> null
        else -> resources.getString(this.stringRes)
    }
}
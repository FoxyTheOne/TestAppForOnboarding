package com.foxynest.testappforonboarding.other.utils

import android.util.Patterns

object EmailUtils {
    fun emailMatches(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}

package com.foxynest.testappforonboarding.other.utils

import android.util.Patterns

object EmailUtils {
    fun emailMatches(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    // Альтернативная реализация для тестов, не зависящая от Android
    fun emailMatchesForTest(email: String): Boolean {
        val emailRegex = "^[A-Za-z](.*)([@]{1})(.{1,})(\\.)(.{1,})".toRegex()
        return emailRegex.matches(email)
    }
}

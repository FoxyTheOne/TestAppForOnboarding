package com.foxynest.testappforonboarding.other.utils

/**
 * Проверка формата email без android.util.Patterns: её используют use case'ы domain-слоя,
 * а domain не должен зависеть от Android. Выражение совпадает с Patterns.EMAIL_ADDRESS,
 * поэтому поведение не изменилось, зато проверку можно тестировать обычными JVM-тестами
 */
object EmailUtils {
    private val EMAIL_ADDRESS = Regex(
        "[a-zA-Z0-9+._%\\-]{1,256}" +
                "@" +
                "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
                "(\\.[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25})+"
    )

    fun emailMatches(email: String): Boolean = EMAIL_ADDRESS.matches(email)
}
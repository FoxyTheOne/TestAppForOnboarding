package com.foxynest.testappforonboarding.domain.use_cases.authUseCases

import com.foxynest.testappforonboarding.domain.entities.ErrorType

/**
 * Результат проверки поля: прошла ли и какая ошибка. Общий для всех Validate-use case-ов:
 * можно было бы сделать свой класс результата в каждом, но поля у всех одинаковые.
 * @param errorMessage несмотря на имя – тип ошибки, а не текст (текст подбирает экран)
 */
data class ValidationResult(
    val successful: Boolean,
    val errorMessage: ErrorType? = null
)

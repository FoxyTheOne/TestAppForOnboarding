package com.foxynest.testappforonboarding.other.extensions

import com.foxynest.testappforonboarding.domain.entities.onboarding.AnswerNames

private fun Map<String, String>.hasValidAnswers(): Boolean {
    // Проверяем обязательные поля
    val requiredFields = listOf(
        AnswerNames.USER_NAME_ANSWER.toString(),
        AnswerNames.CHILD_GENDER_ANSWER.toString(),
        AnswerNames.CHILD_NAME_ANSWER.toString(),
        AnswerNames.CHILD_BIRTH_DATE_ANSWER.toString(),
        AnswerNames.CHILD_HOBBY_ANSWER.toString()
    )

    return requiredFields.any { field ->
        this[field]?.isNotBlank() == true
    }
}
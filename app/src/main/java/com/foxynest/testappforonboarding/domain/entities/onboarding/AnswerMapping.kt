package com.foxynest.testappforonboarding.domain.entities.onboarding

/**
 * Теперь при добавлении нового вопроса нужно будет:
 *     Добавить новый enum в AnswerNames
 *     Добавить новый mapping в answerMappings
 *     Обновить OnboardingAnswers data class (если нужно новое поле)
 */
data class AnswerMapping(
    val answerName: AnswerNames,
    val getter: suspend () -> String?,
    val setter: suspend (String) -> Unit,
    val defaultValue: String
)
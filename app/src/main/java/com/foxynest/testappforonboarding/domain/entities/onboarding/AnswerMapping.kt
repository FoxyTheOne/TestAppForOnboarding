package com.foxynest.testappforonboarding.domain.entities.onboarding

data class AnswerMapping(
    val answerName: AnswerNames,
    val getter: suspend () -> String?,
    val setter: suspend (String) -> Unit,
    val defaultValue: String
)
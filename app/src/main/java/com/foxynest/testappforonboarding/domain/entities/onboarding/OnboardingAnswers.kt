package com.foxynest.testappforonboarding.domain.entities.onboarding

import com.foxynest.testappforonboarding.domain.entities.isNotBlankValue

/**
 * Ответы анкеты – domain-модель, чистый Kotlin.
 *
 * В DataStore хранится не она, а `OnboardingAnswersDto` (StoredModels.kt) с зафиксированными ключами JSON,
 * поэтому поля здесь можно переименовывать свободно – достаточно поправить маппинг в StoredModels.kt.
 *
 * Пол, «да/нет» и хобби – ключи (`BOY`, `YES`, `chess, books`), а не текст на каком-то языке.
 * Дата рождения – строка `yyyy-MM-dd`.
 */
data class OnboardingAnswers(
    val userName: String = "",
    val childGender: String = "",
    val isItABoy: String = "",
    val childName: String = "",
    val childBirthDate: String = "",
    val childHobbies: String = ""
)

/** Единственная проверка полноты анкеты (раньше было три разных: isValidData, hasValidData, hasValidAnswers). */
fun OnboardingAnswers.isValidData(): Boolean =
    userName.isNotBlankValue() &&
            childGender.isNotBlankValue() &&
            childName.isNotBlankValue() &&
            childBirthDate.isNotBlankValue() &&
            childHobbies.isNotBlankValue()
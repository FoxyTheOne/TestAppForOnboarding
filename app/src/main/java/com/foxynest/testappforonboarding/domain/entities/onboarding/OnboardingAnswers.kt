package com.foxynest.testappforonboarding.domain.entities.onboarding

import com.foxynest.testappforonboarding.domain.entities.isNotBlankValue

/**
 * Хранится в DataStore как JSON (Gson). Имена полей = ключи в JSON:
 * переименование поля без миграции «потеряет» уже сохранённые у пользователей данные.
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
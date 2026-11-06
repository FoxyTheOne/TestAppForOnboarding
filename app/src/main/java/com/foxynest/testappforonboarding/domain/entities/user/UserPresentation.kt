package com.foxynest.testappforonboarding.domain.entities.user

import com.foxynest.testappforonboarding.domain.entities.isNotBlankValue
import com.foxynest.testappforonboarding.domain.entities.onboarding.OnboardingAnswers

/**
 * Пользователь целиком: данные аккаунта (из Firebase Auth) + анкета (из Firestore или с устройства).
 * Модель domain-слоя: чистый Kotlin, без Android (раньше была Parcelable ради аргумента навигации).
 *
 * Для незарегистрированного пользователя uid и authEmail пустые, а анкета берётся из DataStore
 * через [toUserPresentation] – так экраны профиля и главной работают с одной моделью в обоих случаях.
 */
data class UserPresentation(
    val uid: String = "",

    // Email для входа в аккаунт (Firebase Auth)
    val authEmail: String = "",
    val isEmailVerified: Boolean = false,

    // Остальные поля профиля
    val userName: String = "",
    val childGender: String = "",
    val isItABoy: String = "",
    val childName: String = "",
    val childHobbies: String = "",
    val childBirthDate: String = ""
)

/** Защита от записи в Firestore профиля с пустыми полями (set + merge перезаписал бы хорошие данные пустыми). */
fun UserPresentation.isValidData(): Boolean =
    userName.isNotBlankValue() &&
            childGender.isNotBlankValue() &&
            childName.isNotBlankValue() &&
            childHobbies.isNotBlankValue() &&
            childBirthDate.isNotBlankValue()

fun OnboardingAnswers.toUserPresentation(): UserPresentation = UserPresentation(
    userName = userName,
    childGender = childGender,
    isItABoy = isItABoy,
    childName = childName,
    childHobbies = childHobbies,
    childBirthDate = childBirthDate
)
package com.foxynest.testappforonboarding.other.extensions

import com.foxynest.testappforonboarding.domain.entities.onboarding.OnboardingAnswers

fun OnboardingAnswers?.hasValidData(): Boolean {
    if (this == null) return false

    return userName.isNotBlank() &&
            childName.isNotBlank() &&
            childHobbies.isNotBlank() &&
            childBirthDate.isNotBlank()
}
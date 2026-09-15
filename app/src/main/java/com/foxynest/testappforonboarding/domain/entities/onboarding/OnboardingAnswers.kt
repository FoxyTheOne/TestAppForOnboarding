package com.foxynest.testappforonboarding.domain.entities.onboarding

import android.os.Parcelable
import com.foxynest.testappforonboarding.domain.entities.user.UserPresentation
import kotlinx.android.parcel.Parcelize

@Parcelize
data class OnboardingAnswers(
    val userName: String = "",
    val childGender: String = "",
    val isItABoy: String = "",
    val childName: String = "",
    val childBirthDate: String = "",
    val childHobbies: String = ""
) : Parcelable

fun OnboardingAnswers.isValidData(): Boolean {
    return userName.isNotBlank() && userName != "null" &&
            childName.isNotBlank() && childName != "null" &&
            childBirthDate.isNotBlank() && childBirthDate != "null"
}
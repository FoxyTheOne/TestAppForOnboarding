package com.foxynest.testappforonboarding.domain.entities.onboarding

import android.os.Parcelable
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
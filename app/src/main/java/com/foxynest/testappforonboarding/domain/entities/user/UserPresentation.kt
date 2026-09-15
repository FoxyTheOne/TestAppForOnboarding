package com.foxynest.testappforonboarding.domain.entities.user

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
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
) : Parcelable

fun UserPresentation.isValidData(): Boolean {
//    return authEmail.isNotBlank() && authEmail != "null" &&
    return userName.isNotBlank() && userName != "null" &&
            childName.isNotBlank() && childName != "null" &&
            childHobbies.isNotBlank() && childHobbies != "null" &&
            childBirthDate.isNotBlank() && childBirthDate != "null"
}
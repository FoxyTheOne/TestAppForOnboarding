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
    val childHobby: String = "",
    val childBirthDate: String = ""
) : Parcelable
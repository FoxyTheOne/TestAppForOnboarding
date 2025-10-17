package com.foxynest.testappforonboarding.domain.entities.user

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

//@Parcelize
//data class UserPresentation(
//    val uid: String = "",
//    val email: String = "",
//    val isEmailVerified: Boolean = false,
//    val userName: String = "",
//    val childGender: String = "",
//    val isItABoy: String = "",
//    val childName: String = "",
//    val childHobby: String = "",
//    val childBirthDate: String = ""
//) :
//    Parcelable

@Parcelize
data class UserPresentation(
    val uid: String = "",

    // Email для входа в аккаунт (Firebase Auth) - НЕ меняется через приложение
    val authEmail: String = "",
    val isEmailVerified: Boolean = false,

    // Email для отображения в профиле (Firestore) - можно менять свободно
    val displayEmail: String = "",

    // Остальные поля профиля
    val userName: String = "",
    val childGender: String = "",
    val isItABoy: String = "",
    val childName: String = "",
    val childHobby: String = "",
    val childBirthDate: String = ""
) : Parcelable
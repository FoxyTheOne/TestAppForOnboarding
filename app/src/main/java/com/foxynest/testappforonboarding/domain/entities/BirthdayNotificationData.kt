package com.foxynest.testappforonboarding.domain.entities

import android.os.Parcelable
import com.foxynest.testappforonboarding.domain.entities.user.UserPresentation
import kotlinx.android.parcel.Parcelize
import java.util.Calendar

@Parcelize
data class BirthdayNotificationData(
    val childName: String = "",
    val childBirthDate: String = ""
) : Parcelable

fun BirthdayNotificationData.isValidData(): Boolean {
    return childName.isNotBlank() && childName != "null" &&
            childBirthDate.isNotBlank() && childBirthDate != "null"
}
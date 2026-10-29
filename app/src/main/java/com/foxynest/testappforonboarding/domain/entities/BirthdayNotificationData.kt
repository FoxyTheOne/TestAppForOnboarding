package com.foxynest.testappforonboarding.domain.entities

data class BirthdayNotificationData(
    val childName: String = "",
    val childBirthDate: String = ""
)

fun BirthdayNotificationData.isValidData(): Boolean =
    childName.isNotBlankValue() && childBirthDate.isNotBlankValue()
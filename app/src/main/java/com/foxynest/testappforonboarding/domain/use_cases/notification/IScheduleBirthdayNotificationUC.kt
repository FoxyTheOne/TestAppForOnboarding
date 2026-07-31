package com.foxynest.testappforonboarding.domain.use_cases.notification

interface IScheduleBirthdayNotificationUC {
    suspend fun scheduleWithoutParameters()
    suspend fun scheduleWithParameters(childName: String, childBirthDate: String)
    suspend fun checkAndNotifyIfBirthdayToday(childName: String, childBirthDate: String)
}
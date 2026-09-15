package com.foxynest.testappforonboarding.domain.use_cases.notification

interface IScheduleBirthdayNotificationUC {
    //    suspend fun scheduleWithoutParameters()
    suspend fun scheduleBirthdayNotification(childName: String, childBirthDate: String)
//    suspend fun checkAndNotifyRightNowIfBirthdayToday(childName: String, childBirthDate: String)
}
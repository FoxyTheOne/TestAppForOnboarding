package com.foxynest.testappforonboarding.domain.iRepository

interface IBirthdayNotificationRepository {
    suspend fun scheduleBirthdayNotification(childName: String, birthDate: String)
    suspend fun checkAndNotifyIfBirthdayToday(childName: String, birthDate: String)
    suspend fun cancelBirthdayNotification()
}
package com.foxynest.testappforonboarding.domain.iRepository

interface IBirthdayNotificationManager {
    suspend fun scheduleBirthdayNotification(childName: String, birthDate: String)
    suspend fun cancelBirthdayNotification()
}
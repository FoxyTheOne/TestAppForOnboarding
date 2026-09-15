package com.foxynest.testappforonboarding.domain.iRepository

import java.util.Calendar

interface IBirthdayNotificationRepository {
//    suspend fun scheduleBirthdayNotification(childName: String, birthDate: String)
//    suspend fun checkAndNotifyIfBirthdayToday(childName: String, birthDate: String)
//    suspend fun cancelBirthdayNotification()


    suspend fun scheduleBirthdayNotification(childName: String, birthDay: String)
    suspend fun scheduleNextYearBirthday(childName: String, birthDay: String)
    suspend fun showAndScheduleNextYearIfNeeded(childName: String, birthDay: String): Boolean
    suspend fun cancelBirthdayNotification()
}
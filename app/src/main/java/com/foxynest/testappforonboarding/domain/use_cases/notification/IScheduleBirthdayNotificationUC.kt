package com.foxynest.testappforonboarding.domain.use_cases.notification

interface IScheduleBirthdayNotificationUC {
    /** Невалидные данные игнорируются – текущее расписание НЕ сбрасывается (например, профиль не загрузился без сети). */
    suspend fun update(childName: String, childBirthDate: String, userIsInApp: Boolean)
    suspend fun rearmFromSavedData(userIsInApp: Boolean)
    suspend fun cancel()
}
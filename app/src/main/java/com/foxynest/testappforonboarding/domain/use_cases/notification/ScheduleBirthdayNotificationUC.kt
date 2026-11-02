package com.foxynest.testappforonboarding.domain.use_cases.notification

import com.foxynest.testappforonboarding.domain.entities.BirthdayNotificationData
import com.foxynest.testappforonboarding.domain.entities.isValidData
import com.foxynest.testappforonboarding.domain.iRepository.IBirthdayNotificationRepository
import javax.inject.Inject

class ScheduleBirthdayNotificationUC @Inject constructor(
    private val birthdayNotificationRepository: IBirthdayNotificationRepository
) : IScheduleBirthdayNotificationUC {
    override suspend fun update(childName: String, childBirthDate: String, userIsInApp: Boolean) {
        if (BirthdayNotificationData(childName, childBirthDate).isValidData()) {
            birthdayNotificationRepository.update(childName, childBirthDate, userIsInApp)
        } else {
            // Раньше здесь была отмена уведомления: пустые данные (нет сети, профиль ещё не загружен)
            // молча сбрасывали корректное расписание. Отмена теперь только явная – через cancel().
            birthdayNotificationRepository.rearmFromSavedData(userIsInApp)
        }
    }

    override suspend fun rearmFromSavedData(userIsInApp: Boolean) =
        birthdayNotificationRepository.rearmFromSavedData(userIsInApp)

    override suspend fun cancel() = birthdayNotificationRepository.cancel()

    override suspend fun shouldAskNotificationPermission(): Boolean =
        !birthdayNotificationRepository.wasNotificationPermissionAsked()

    override suspend fun markNotificationPermissionAsked() =
        birthdayNotificationRepository.markNotificationPermissionAsked()
}
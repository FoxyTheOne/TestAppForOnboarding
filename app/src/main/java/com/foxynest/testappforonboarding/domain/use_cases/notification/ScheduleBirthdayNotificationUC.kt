package com.foxynest.testappforonboarding.domain.use_cases.notification

import com.foxynest.testappforonboarding.domain.entities.BirthdayNotificationData
import com.foxynest.testappforonboarding.domain.entities.isValidData
import com.foxynest.testappforonboarding.domain.iRepository.IBirthdayNotificationRepository
import javax.inject.Inject

/**
 * Domain layer, UseCase. Бизнес-логика, Kotlin. Работает только с Repository.
 *
 * Interactor ответственен за обеспечение данными отдельные экраны (для каждого экрана - отдельный Interactor).
 * При работе с model, здесь происходит преобразование local -> presentation, т.е.
 * преобразование моделей в модели нижнего уровня перед тем, как нижний уровень сможет их использовать.
 */
class ScheduleBirthdayNotificationUC @Inject constructor(
    private val notificationManager: IBirthdayNotificationRepository
) : IScheduleBirthdayNotificationUC {
    override suspend fun scheduleBirthdayNotification(
        childName: String,
        childBirthDate: String
    ) {
        if (BirthdayNotificationData(
                childName = childName,
                childBirthDate = childBirthDate
            ).isValidData()
        ) {
            notificationManager.scheduleBirthdayNotification(childName, childBirthDate)
        } else {
            // Нет данных – отменяем уведомление
            notificationManager.cancelBirthdayNotification()
        }
    }

    // НЕМЕДЛЕННАЯ ПРОВЕРКА (без планирования, на случай если ДР сегодня)
    override suspend fun checkAndNotifyRightNowIfBirthdayToday(
        childName: String,
        childBirthDate: String
    ) {
        notificationManager.checkAndNotifyIfBirthdayToday(childName, childBirthDate)
    }
}


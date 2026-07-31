package com.foxynest.testappforonboarding.domain.use_cases.notification

import com.foxynest.testappforonboarding.domain.iRepository.IBirthdayNotificationManager
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.IGetCurrentUserPresentationUC
import com.foxynest.testappforonboarding.other.Status
import javax.inject.Inject

/**
 * Domain layer, UseCase. Бизнес-логика, Kotlin. Работает только с Repository.
 *
 * Interactor ответственен за обеспечение данными отдельные экраны (для каждого экрана - отдельный Interactor).
 * При работе с model, здесь происходит преобразование local -> presentation, т.е.
 * преобразование моделей в модели нижнего уровня перед тем, как нижний уровень сможет их использовать.
 */
class ScheduleBirthdayNotificationUC @Inject constructor(
    private val getCurrentUserUC: IGetCurrentUserPresentationUC,
    private val notificationManager: IBirthdayNotificationManager
) : IScheduleBirthdayNotificationUC {
    override suspend fun invoke() {
        val result = getCurrentUserUC.invoke()
        if (result.status == Status.SUCCESS) {
            val user = result.data
            if (user != null && user.childName.isNotBlank() && user.childBirthDate.isNotBlank()) {
                notificationManager.scheduleBirthdayNotification(
                    user.childName,
                    user.childBirthDate
                )
            } else {
                // Нет данных – отменяем уведомление
                notificationManager.cancelBirthdayNotification()
            }
        } else {
            notificationManager.cancelBirthdayNotification()
        }
    }
}


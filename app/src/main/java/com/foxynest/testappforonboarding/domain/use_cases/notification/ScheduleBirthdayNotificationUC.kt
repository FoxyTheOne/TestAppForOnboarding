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
    // TODO повторный запрос данных мне не нравится, оставляю на крайний случай. Получается, мы сохраняем данные и сразу же запрашиваем их здесь, а вдруг ещё не успели данные сохраниться? Не будет ли конфликтов? Хотелось бы этот метод вообще убрать, но пока не знаю, как
    override suspend fun scheduleWithoutParameters() {
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

    override suspend fun scheduleWithParameters(
        childName: String,
        childBirthDate: String
    ) {
        if (childName.isNotBlank() &&
            childName != "null" &&
            childBirthDate != "null" &&
            childBirthDate.isNotBlank()
        ) {
            notificationManager.scheduleBirthdayNotification(childName, childBirthDate)
        } else {
            // Нет данных – отменяем уведомление
            notificationManager.cancelBirthdayNotification()
        }
    }

    override suspend fun checkAndNotifyIfBirthdayToday(
        childName: String,
        childBirthDate: String
    ) {
        // НЕМЕДЛЕННАЯ ПРОВЕРКА
        notificationManager.checkAndNotifyIfBirthdayToday(childName, childBirthDate)
    }
}


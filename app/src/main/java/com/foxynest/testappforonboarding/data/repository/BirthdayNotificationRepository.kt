package com.foxynest.testappforonboarding.data.repository

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.foxynest.testappforonboarding.data.dataSource.datastorePreferences.IAppDatastorePreferences
import com.foxynest.testappforonboarding.data.workManager.BirthdayNotificationWorker
import com.foxynest.testappforonboarding.domain.entities.BirthdayNotificationData
import com.foxynest.testappforonboarding.domain.iRepository.IBirthdayNotificationRepository
import com.foxynest.testappforonboarding.other.constants.BirthdayNotificationConstants.BIRTHDAY_CHECK_INTERVAL_HOURS
import com.foxynest.testappforonboarding.other.constants.BirthdayNotificationConstants.BIRTHDAY_FLEX_INTERVAL_HOURS
import com.foxynest.testappforonboarding.other.constants.BirthdayNotificationConstants.BIRTHDAY_NOTIF_UNIQUE_WORK_NAME
import com.foxynest.testappforonboarding.other.constants.BirthdayNotificationConstants.BIRTHDAY_NOTIF_WORK_TAG
import com.foxynest.testappforonboarding.other.extensions.calculateWorkerDelayToNextMorning
import com.foxynest.testappforonboarding.other.timeProvider.ITimeProvider
import com.foxynest.testappforonboarding.other.utils.NotificationHelper
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class BirthdayNotificationRepository @Inject constructor(
    private val workManager: WorkManager,
    private val notificationHelper: NotificationHelper,
    private val timeProvider: ITimeProvider,
    private val dataStore: IAppDatastorePreferences // для хранения данных ребенка
) : IBirthdayNotificationRepository {
    override suspend fun scheduleBirthdayNotification(childName: String, birthDate: String) {
        // 1. Сохраняем данные для Worker
        val birthdayNotificationData = BirthdayNotificationData(childName = childName, childBirthDate = birthDate)
        dataStore.saveBirthdayNotificationData(birthdayNotificationData)

        // 2. Отменяем старую работу
        workManager.cancelUniqueWork(BIRTHDAY_NOTIF_UNIQUE_WORK_NAME)

        // 3. Создаем периодическую работу
        val initialDelay = calculateInitialDelay()
        val workRequest = createPeriodicWorkRequest(initialDelay)

        // 4. Запускаем
        workManager.enqueueUniquePeriodicWork(
            BIRTHDAY_NOTIF_UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE, // Заменяем одинаковые уведомления
            workRequest
        )
        // Есть ли негативные последствия от частой «перезаписи»?
        // Краткий ответ: нет. В вашем случае это не создает проблем.
        //    Накладные расходы: Отмена и создание новой задачи — это легковесная операция для WorkManager. Это не тяжелый процесс, который может нагрузить систему или привести к зависаниям.
        //    Влияние на время уведомления: Нет, это никак не повлияет на точность времени. PeriodicWorkRequest по своей сути не является высокоточным инструментом. Android оптимизирует выполнение фоновых задач для экономии батареи, поэтому уведомление все равно может прийти не ровно в 9:00, а с небольшой задержкой. Частая перезапись задачи не усугубляет эту ситуацию.
        // !!! Единственная реальная опасность, которую описывают в интернете, — это когда разработчик по ошибке использует enqueue() (неуникальную задачу) в onCreate() Activity. Тогда действительно каждое открытие экрана создает новую параллельную задачу. Но вы используете уникальную (enqueueUniquePeriodicWork), поэтому эта проблема вас не касается.
    }

    override suspend fun checkAndNotifyIfBirthdayToday(childName: String, birthDate: String) {
        // Используем timeProvider для получения текущей даты
        val today = timeProvider.getCurrentTime()
        notificationHelper.checkAndNotifyIfBirthdayToday(
            childName = childName,
            birthDate = birthDate,
            today = today
        )
    }

    override suspend fun cancelBirthdayNotification() {
        workManager.cancelUniqueWork(BIRTHDAY_NOTIF_UNIQUE_WORK_NAME)
        dataStore.clearBirthdayNotificationData()
    }

    private fun calculateInitialDelay(): Long {
        val now = timeProvider.getCurrentTime()
        return now.calculateWorkerDelayToNextMorning()
    }

    private fun createPeriodicWorkRequest(initialDelay: Long): PeriodicWorkRequest {
        return PeriodicWorkRequestBuilder<BirthdayNotificationWorker>(
            BIRTHDAY_CHECK_INTERVAL_HOURS, TimeUnit.HOURS,
            BIRTHDAY_FLEX_INTERVAL_HOURS, TimeUnit.HOURS
        )
            .setInitialDelay(
                initialDelay,
                TimeUnit.MILLISECONDS
            ) // Иначе при запуске в 23 часа поздравления будут в это же время
            .addTag(BIRTHDAY_NOTIF_WORK_TAG)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .build()
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                15, TimeUnit.MINUTES
            ) // Политика повторных попыток
            .build()
    }
}
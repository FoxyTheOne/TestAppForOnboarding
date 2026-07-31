package com.foxynest.testappforonboarding.data.repository

import android.os.Build
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.foxynest.testappforonboarding.data.workManager.BirthdayNotificationWorker
import com.foxynest.testappforonboarding.domain.iRepository.IBirthdayNotificationManager
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class BirthdayNotificationManager @Inject constructor(
    private val workManager: WorkManager
) : IBirthdayNotificationManager {

    companion object {
        private const val WORK_TAG = "birthday_notification_work"
        private const val UNIQUE_WORK_NAME = "birthday_notification"
    }

    override suspend fun scheduleBirthdayNotification(childName: String, birthDate: String) {
        // Отменяем старую работу
        workManager.cancelUniqueWork(UNIQUE_WORK_NAME)

        // Периодическая работа – раз в день (минимальный интервал ~15 минут, но для тестов можно 15 минут,
        // в продакшене используем 24 часа с flexible периодом)
        // Для Android 12+ можно использовать setPeriodic(PeriodicWorkRequestBuilder)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED) // Не требует сети
            .build()

        // Если API меньше API 26
        val workRequest = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            PeriodicWorkRequestBuilder<BirthdayNotificationWorker>(
                repeatInterval = 1, TimeUnit.DAYS,
                flexTimeInterval = 1, TimeUnit.HOURS // гибкость для экономии батареи
            )
        } else {
            PeriodicWorkRequestBuilder<BirthdayNotificationWorker>(
                repeatInterval = java.time.Duration.ofDays(1),
                flexTimeInterval = java.time.Duration.ofHours(1) // гибкость для экономии батареи
            )
        }
            .setConstraints(constraints)
            .addTag(WORK_TAG)
            .build()

        workManager.enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
    }

    override suspend fun cancelBirthdayNotification() {
        workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
    }
}
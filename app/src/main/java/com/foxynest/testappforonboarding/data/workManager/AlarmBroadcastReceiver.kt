package com.foxynest.testappforonboarding.data.workManager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.foxynest.testappforonboarding.other.constants.BirthdayNotificationConstants.BIRTH_NOTIF_EXTRA_BIRTH_DATE
import com.foxynest.testappforonboarding.other.constants.BirthdayNotificationConstants.BIRTH_NOTIF_EXTRA_CHILD_NAME
import com.foxynest.testappforonboarding.other.constants.BirthdayNotificationConstants.BIRTH_NOTIF_UNIQUE_WORK_NAME
import com.foxynest.testappforonboarding.other.constants.BirthdayNotificationConstants.BIRTH_NOTIF_WORK_TAG
import java.util.concurrent.TimeUnit

/**
 * Должен быть зарегистрирован в манифесте
 *
 * AlarmBroadcastReceiver получает данные и запускает воркер
 *
 * Ресивер зарегистрирован + BOOT_COMPLETED, чтобы после перезагрузки он получил событие и мог перепланировать будильник
 * После перезагрузки устройства AlarmReceiver получит BOOT_COMPLETED, запустит RescheduleAlarmWorker, который перепланирует будильник
 */
class AlarmBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
//        val currentData = dataStore.getBirthdayNotificationDataFlow().first()
//        if (childName != currentData.childName || birthDate != currentData.childBirthDate) {
//            // Данные изменились – перезапускаем с актуальными
//            WorkManager.getInstance(context).enqueueUniqueWork(...)
//            return
//        }
//// иначе запускаем Worker с переданными

        Log.d(
            "AlarmBroadcastReceiver",
            "BIRTHDAY onReceive: action=${intent.action}, extras=${intent.extras}"
        )

        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                WorkManager.getInstance(context).cancelAllWorkByTag(BIRTH_NOTIF_WORK_TAG)

                // Запустим Worker, который перепланирует будильник после перезагрузки
                val workRequest = OneTimeWorkRequestBuilder<RescheduleAlarmWorker>()
                    .setConstraints(Constraints.NONE)
                    .addTag(BIRTH_NOTIF_WORK_TAG)
                    .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        15, TimeUnit.MINUTES
                    )
                    .build()

                WorkManager.getInstance(context).enqueueUniqueWork(
                    BIRTH_NOTIF_UNIQUE_WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    workRequest
                )
            }

            else -> {
                WorkManager.getInstance(context).cancelAllWorkByTag(BIRTH_NOTIF_WORK_TAG)

                // Обычное срабатывание будильника - запускаем Worker
                // Получаем данные из Intent
                val childName = intent.getStringExtra(BIRTH_NOTIF_EXTRA_CHILD_NAME) ?: return
                val birthDate = intent.getStringExtra(BIRTH_NOTIF_EXTRA_BIRTH_DATE) ?: return

                // Запускаем Worker для проверки и показа уведомления
                val workRequest = OneTimeWorkRequestBuilder<BirthdayNotificationWorker>()
                    .setInputData(
                        workDataOf(
                            BIRTH_NOTIF_EXTRA_CHILD_NAME to childName,
                            BIRTH_NOTIF_EXTRA_BIRTH_DATE to birthDate
                        )
                    )
                    .setConstraints(Constraints.NONE)
                    .addTag(BIRTH_NOTIF_WORK_TAG)
                    .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        15, TimeUnit.MINUTES
                    )
                    .build()

                WorkManager.getInstance(context).enqueueUniqueWork(
                    BIRTH_NOTIF_UNIQUE_WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    workRequest
                )
            }
        }
    }
}
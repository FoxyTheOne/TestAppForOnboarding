package com.foxynest.testappforonboarding.data.workManager

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.foxynest.testappforonboarding.data.dataSource.datastorePreferences.IAppDatastorePreferences
import com.foxynest.testappforonboarding.data.repository.BirthdayNotificationRepository
import com.foxynest.testappforonboarding.domain.entities.isValidData
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class RescheduleAlarmWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: BirthdayNotificationRepository,
    private val appDatastorePreferences: IAppDatastorePreferences
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.i("RescheduleAlarmWorker", "BIRTHDAY Birthday RescheduleAlarmWorker started")
        try {
            val birthData = appDatastorePreferences.getBirthdayNotificationDataFlow().first()
            val name = birthData.childName
            val birthDay = birthData.childBirthDate
            if (birthData.isValidData()) {
                // Перепланируем будильник
                repository.scheduleBirthdayNotification(name, birthDay)
                Log.d("RescheduleAlarmWorker", "BIRTHDAY Birthday alarm rescheduled")
            }
            return Result.success()
        } catch (e: Exception) {
//            return Result.failure()
//            В RescheduleAlarmWorker при исключении вы возвращаете Result.failure(). Это приведёт к тому, что WorkManager будет повторять попытку с экспоненциальной задержкой (по умолчанию), что может создать лишнюю нагрузку. Лучше возвращать Result.success(), чтобы не перезапускать задачу, если перепланирование не удалось (например, нет данных) – в следующий раз оно будет вызвано при перезагрузке снова.
           Log.d("RescheduleAlarmWorker", e.message.toString())
            return Result.success()
        }
    }
}

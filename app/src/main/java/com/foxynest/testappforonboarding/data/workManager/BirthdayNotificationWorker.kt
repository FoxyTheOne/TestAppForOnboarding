package com.foxynest.testappforonboarding.data.workManager

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.foxynest.testappforonboarding.data.dataSource.datastorePreferences.IAppDatastorePreferences
import com.foxynest.testappforonboarding.data.repository.AuthRepository
import com.foxynest.testappforonboarding.data.repository.OnboardingRepository
import com.foxynest.testappforonboarding.domain.entities.isValidData
import com.foxynest.testappforonboarding.other.Resource
import com.foxynest.testappforonboarding.other.Status
import com.foxynest.testappforonboarding.other.constants.BirthdayNotificationConstants.BIRTHDAY_NOTIF_UNIQUE_WORK_NAME
import com.foxynest.testappforonboarding.other.extensions.calculateWorkerDelayToNextMorning
import com.foxynest.testappforonboarding.other.extensions.createUserFromAnswersForNotRegisteredUser
import com.foxynest.testappforonboarding.other.extensions.isTodayBirthday
import com.foxynest.testappforonboarding.other.timeProvider.ITimeProvider
import com.foxynest.testappforonboarding.other.utils.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Отвечает за планирование работы:
 *     Создаёт PeriodicWorkRequest с нужными параметрами (интервал, ограничения, политика замены).
 *     Отвечает за отмену (cancel) запланированной работы.
 *     Является сервисным слоем (Data layer), инкапсулирующим работу с WorkManager.
 */
@HiltWorker
class BirthdayNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val dataStore: IAppDatastorePreferences,
    private val notificationHelper: NotificationHelper,
    private val timeProvider: ITimeProvider
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // 1. Получаем сохраненные данные
        val birthdayNotificationData = dataStore.getBirthdayNotificationDataFlow().first()
        val childName = birthdayNotificationData.childName
        val birthDate = birthdayNotificationData.childBirthDate

        if (!birthdayNotificationData.isValidData()) {
            return Result.failure()
        }

        // 2. Проверяем, сегодня ли день рождения
        val today = timeProvider.getCurrentTime()
        if (birthDate.isTodayBirthday(today)) {
            notificationHelper.showBirthdayNotificationIfPermitted(childName)
        }

        // 3. Всегда возвращаем success — работа будет повторяться автоматически
        return Result.success()
    }

    @AssistedFactory
    interface Factory {
        fun create(context: Context, params: WorkerParameters): BirthdayNotificationWorker
    }
}

/**
 * Почему @AssistedFactory внутри класса Worker?
 *
 * Это стандартная практика для Hilt + WorkManager. Worker создаётся не через DI-контейнер напрямую, а через фабрику, потому что WorkManager сам создаёт экземпляры Worker, передавая Context и WorkerParameters. Hilt не может автоматически внедрить зависимости в конструктор Worker, потому что эти параметры передаются во время выполнения. Поэтому мы:
 *     Помечаем конструктор @AssistedInject.
 *     Параметры context и params помечаем @Assisted.
 *     Создаём внутренний интерфейс Factory с методом create, который принимает Context и WorkerParameters, а остальные зависимости (например, getCurrentUserUC, notificationHelper) Hilt будет внедрять автоматически при создании фабрики.
 *
 * Почему не в отдельном модуле?
 * Фабрика тесно связана с конкретным Worker – она знает, какие параметры нужны для его создания. Обычно её объявляют внутри класса Worker (или рядом) для удобства и читаемости. В Dagger/Hilt это распространённый паттерн. Вы можете вынести её в отдельный файл, но это не обязательно и может усложнить навигацию. Оставляйте внутри – это правильно.
 */
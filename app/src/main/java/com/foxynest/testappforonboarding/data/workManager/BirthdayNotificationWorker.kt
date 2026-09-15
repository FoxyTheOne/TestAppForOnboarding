package com.foxynest.testappforonboarding.data.workManager

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.foxynest.testappforonboarding.data.dataSource.datastorePreferences.IAppDatastorePreferences
import com.foxynest.testappforonboarding.domain.iRepository.IBirthdayNotificationRepository
import com.foxynest.testappforonboarding.other.constants.BirthdayNotificationConstants.BIRTH_NOTIF_EXTRA_BIRTH_DATE
import com.foxynest.testappforonboarding.other.constants.BirthdayNotificationConstants.BIRTH_NOTIF_EXTRA_CHILD_NAME
import com.foxynest.testappforonboarding.other.extensions.parseDateToLocalDate
import com.foxynest.testappforonboarding.other.timeProvider.ITimeProvider
import com.foxynest.testappforonboarding.other.utils.BirthdayDateHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * Отвечает за планирование работы:
 *     Создаёт PeriodicWorkRequest с нужными параметрами (интервал, ограничения, политика замены).
 *     Отвечает за отмену (cancel) запланированной работы.
 *     Является сервисным слоем (Data layer), инкапсулирующим работу с WorkManager.
 */
//@HiltWorker
//class BirthdayNotificationWorker @AssistedInject constructor(
//    @Assisted context: Context,
//    @Assisted private val params: WorkerParameters,
//    private val timeProvider: ITimeProvider,
//    private val notificationHelper: NotificationHelper
////    private val dataStore: IAppDatastorePreferences
//) : CoroutineWorker(context, params) {
//    companion object {
//        private val TAG = "BirthdayNotificationWorker"
//    }
//
//    override suspend fun doWork(): Result {
//        try {
//            Log.i(TAG, "info: BirthdayNotificationWorker started at ${System.currentTimeMillis()}")
//
//            // 1. Получаем сохраненные данные
////            val birthdayNotificationData = dataStore.getBirthdayNotificationDataFlow().first()
////            Log.d(TAG, "Read birthday data: $birthdayNotificationData")
////            val childName = birthdayNotificationData.childName
////            val birthDate = birthdayNotificationData.childBirthDate
//
////            if (!birthdayNotificationData.isValidData()) {
////                Log.d(
////                    TAG,
////                    "BirthdayNotificationWorker failure - birthdayNotificationData is not valid"
////                )
////            return Result.failure()
//////                return Result.success() // Если данные в DataStore невалидны – возвращайте Result.success() (а не failure), чтобы не прерывать периодичность
////            }
//
//            val childName = params.inputData.getString(BIRTHDAY_NOTIF_CHILD_NAME) ?: ""
//            val birthDateString = params.inputData.getString(BIRTHDAY_NOTIF_BIRTHDAY_DAY) ?: ""
//            Log.d(TAG, "Read birthday data: $childName, $birthDateString")
//
//
//            if (!childName.isNotBlank() && childName == "null" &&
//                !birthDateString.isNotBlank() && birthDateString == "null") {
//                Log.d(
//                    TAG,
//                    "Birthday worker failure - birthdayNotificationData is not valid"
//                )
//                return Result.failure()
////                return Result.success() // Если данные в DataStore невалидны – возвращайте Result.success() (а не failure), чтобы не прерывать периодичность
//            }
//
//            // 2. Проверяем, сегодня ли день рождения
//            val today = timeProvider.getCurrentTime()
//            if (birthDateString.isBirthdayToday(today)) {
//                Log.d(TAG, "Birthday is today")
//                notificationHelper.showBirthdayNotificationIfPermitted(childName)
//            } else {
//                Log.d(TAG, "Birthday is not today")
//            }
//
//            // 3. Всегда возвращаем success — работа будет повторяться автоматически
//            return Result.success()
//        } catch (e: Exception) {
//            Log.e(TAG, "Birthday worker failed", e)
//            return Result.failure()
//        }
//    }
//
////    @AssistedFactory
////    interface Factory {
////        fun create(context: Context, params: WorkerParameters): BirthdayNotificationWorker
////    }
//}

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


@HiltWorker
class BirthdayNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val appDatastorePreferences: IAppDatastorePreferences,
    private val timeProvider: ITimeProvider,
    private val birthdayNotificationRepository: IBirthdayNotificationRepository
) : CoroutineWorker(context, params) {

    companion object {
        private val TAG = "BirthdayNotificationWorker"
    }

    override suspend fun doWork(): Result {
        try {
            Log.i(
                TAG,
                "BIRTHDAY info: BirthdayNotificationWorker started at ${System.currentTimeMillis()}"
            )

            // 1. Получаем данные из InputData или из DataStore (на случай, если интент не передал)
            var childName = inputData.getString(BIRTH_NOTIF_EXTRA_CHILD_NAME)
            var birthDay = inputData.getString(BIRTH_NOTIF_EXTRA_BIRTH_DATE)
            if (childName.isNullOrBlank() || childName == "null" ||
                birthDay.isNullOrBlank() || birthDay == "null"
            ) {
                // Если в интенте нет, читаем из DataStore
                val birthdayData = appDatastorePreferences.getBirthdayNotificationDataFlow().first()
                childName = birthdayData.childName
                birthDay = birthdayData.childBirthDate
            }
            Log.d(TAG, "BIRTHDAY Read birthday data: $childName, $birthDay")

            if (childName.isBlank() || childName == "null" ||
                birthDay.isBlank() || birthDay == "null"
            ) {
                // Нет данных – нечего делать, но возвращаем success, чтобы не зацикливать ошибки
                Log.d(
                    TAG,
                    "BIRTHDAY Birthday worker failure - birthdayNotificationData is not valid"
                )
                return Result.success()
            }

            // 2. Проверяем день рождения (сегодня или вчера)
//            val birthDate = LocalDate.parse(birthDay) ?: return Result.success()
            val birthDate = birthDay.parseDateToLocalDate() ?: return Result.success()
            val now = timeProvider.getCurrentDateTime()

            val (isToday, isYesterday) = BirthdayDateHelper.isBirthdayTodayOrYesterday(
                birthDate,
                now.toLocalDate()
            )

            if (isToday || isYesterday) {
                birthdayNotificationRepository.showAndScheduleNextYearIfNeeded(childName, birthDay)
            }

            return Result.success()
        } catch (e: Exception) {
            // Логируем ошибку, но возвращаем success – чтобы не прерывать будущие запуски
            Log.e(TAG, "BIRTHDAY Error in birthday doWork()", e)
            return Result.success()
        }
    }
}
package com.foxynest.testappforonboarding.data.repository

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.foxynest.testappforonboarding.data.dataSource.datastorePreferences.IAppDatastorePreferences
import com.foxynest.testappforonboarding.data.workManager.AlarmBroadcastReceiver
import com.foxynest.testappforonboarding.domain.entities.BirthdayNotificationData
import com.foxynest.testappforonboarding.domain.iRepository.IBirthdayNotificationRepository
import com.foxynest.testappforonboarding.other.constants.BirthdayNotificationConstants.BIRTH_NOTIF_EXTRA_BIRTH_DATE
import com.foxynest.testappforonboarding.other.constants.BirthdayNotificationConstants.BIRTH_NOTIF_EXTRA_CHILD_NAME
import com.foxynest.testappforonboarding.other.extensions.parseDateToLocalDate
import com.foxynest.testappforonboarding.other.timeProvider.ITimeProvider
import com.foxynest.testappforonboarding.other.utils.BirthdayDateHelper
import com.foxynest.testappforonboarding.other.utils.NotificationHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

//class BirthdayNotificationRepository @Inject constructor(
//    private val workManager: WorkManager,
//    private val notificationHelper: NotificationHelper,
//    private val timeProvider: ITimeProvider,
////    private val dataStore: IAppDatastorePreferences // для хранения данных ребенка
//) : IBirthdayNotificationRepository {
//    companion object {
//        private val TAG = "BirthdayNotificationRepository"
//    }
//
//    override suspend fun scheduleBirthdayNotification(childName: String, birthDate: String) {
//        Log.d(TAG, "scheduleBirthdayNotification called: name=$childName, birthDate=$birthDate")
////        // 1. Сохраняем данные для Worker
////        val birthdayNotificationData =
////            BirthdayNotificationData(childName = childName, childBirthDate = birthDate)
////        dataStore.saveBirthdayNotificationData(birthdayNotificationData)
//
//
//        // TODO В логах видно, что scheduleBirthdayNotification вызывается десятки раз за короткий промежуток – каждый раз работа отменяется и создаётся заново. Это не должно мешать выполнению последней задачи, но для надёжности можно ограничить вызовы, например, проверять, что дата рождения действительно изменилась, прежде чем перепланировать.
//        val oldChildName = ""
//        val oldBirthDateString = ""
//        try {
//            val workInfos =
//                workManager.getWorkInfosForUniqueWork(BIRTHDAY_NOTIF_UNIQUE_WORK_NAME).get()
//
//            Log.d(TAG, "Work states: ${workInfos.map { it.state }}")
//            workInfos.forEach { info ->
//                val oldChildName = info.outputData.getString(BIRTHDAY_NOTIF_CHILD_NAME) ?: ""
//                val oldBirthDateString = info.outputData.getString(BIRTHDAY_NOTIF_BIRTHDAY_DAY) ?: ""
//                Log.d(
//                    TAG,
//                    "ID: ${info.id}, State: ${info.state}, RunAttemptCount: ${info.runAttemptCount}, oldChildName $oldChildName, oldBirthDateString $oldBirthDateString"
//                )
//            }
//        } catch (e: Exception) {
//            Log.e(TAG, "Failed to get work info", e)
//        }
//
//        if (oldChildName == childName && oldBirthDateString == birthDate) {
//            Log.d(TAG, "Data unchanged, skip birthday rescheduling")
//            return
//        } else {
//            Log.d(TAG, "Data CHANGED, birthday rescheduling: oldChildName $oldChildName, childName $childName, oldBirthDateString = $oldBirthDateString, birthDate = $birthDate")
//        }
//
//        // 2. Отменяем старую работу
//        workManager.cancelUniqueWork(BIRTHDAY_NOTIF_UNIQUE_WORK_NAME)
//
//        // 3. Создаем периодическую работу
//        val initialDelay = calculateInitialDelay()
//        Log.d(
//            TAG,
//            "Birthday notification Initial delay = $initialDelay ms or (${initialDelay / 60000} min)"
//        )
//        val workRequest = try {
//            createPeriodicWorkRequest(initialDelay, childName = childName, birthDate = birthDate)
//        } catch (e: Exception) {
//            Log.e(TAG, "Failed to create birthday work request", e)
//            return
//        }
//
//        // 4. Запускаем
//        workManager.enqueueUniquePeriodicWork(
//            BIRTHDAY_NOTIF_UNIQUE_WORK_NAME,
//            ExistingPeriodicWorkPolicy.REPLACE, // Заменяем одинаковые уведомления
//            workRequest
//        )
//        // Есть ли негативные последствия от частой «перезаписи»?
//        // Краткий ответ: нет. В вашем случае это не создает проблем.
//        //    Накладные расходы: Отмена и создание новой задачи — это легковесная операция для WorkManager. Это не тяжелый процесс, который может нагрузить систему или привести к зависаниям.
//        //    Влияние на время уведомления: Нет, это никак не повлияет на точность времени. PeriodicWorkRequest по своей сути не является высокоточным инструментом. Android оптимизирует выполнение фоновых задач для экономии батареи, поэтому уведомление все равно может прийти не ровно в 9:00, а с небольшой задержкой. Частая перезапись задачи не усугубляет эту ситуацию.
//        // !!! Единственная реальная опасность, которую описывают в интернете, — это когда разработчик по ошибке использует enqueue() (неуникальную задачу) в onCreate() Activity. Тогда действительно каждое открытие экрана создает новую параллельную задачу. Но вы используете уникальную (enqueueUniquePeriodicWork), поэтому эта проблема вас не касается.
//        Log.d(
//            TAG,
//            "Birthday WorkManager instance factory: ${workManager.configuration.workerFactory}"
//        )
//
//        try {
//            val workInfos =
//                workManager.getWorkInfosForUniqueWork(BIRTHDAY_NOTIF_UNIQUE_WORK_NAME).get()
//            Log.d(TAG, "Work states: ${workInfos.map { it.state }}")
//            workInfos.forEach { info ->
//                Log.d(
//                    TAG,
//                    "ID: ${info.id}, State: ${info.state}, RunAttemptCount: ${info.runAttemptCount}, init delay ${info.initialDelayMillis / 60000} min"
//                )
//            }
//
//            delay(10000L)
//
//            Log.d(TAG, "Work states: ${workInfos.map { it.state }}")
//            workInfos.forEach { info ->
//                Log.d(
//                    TAG,
//                    "ID: ${info.id}, State: ${info.state}, RunAttemptCount: ${info.runAttemptCount}, init delay ${info.initialDelayMillis/ 60000} min"
//                )
//            }
//        } catch (e: Exception) {
//            Log.e(TAG, "Failed to get work info", e)
//        }
//    }
//
//    override suspend fun checkAndNotifyIfBirthdayToday(childName: String, birthDate: String) {
//        // Используем timeProvider для получения текущей даты
//        val today = timeProvider.getCurrentTime()
//        notificationHelper.checkAndNotifyIfBirthdayToday(
//            childName = childName,
//            birthDate = birthDate,
//            today = today
//        )
//    }
//
//    override suspend fun cancelBirthdayNotification() {
//        workManager.cancelUniqueWork(BIRTHDAY_NOTIF_UNIQUE_WORK_NAME)
////        dataStore.clearBirthdayNotificationData()
//    }
//
//    private fun calculateInitialDelay(): Long {
//        val now = timeProvider.getCurrentTime()
//        return now.calculateDelayToNextMorning()
//    }
//
//    private fun createPeriodicWorkRequest(
//        initialDelay: Long,
//        childName: String,
//        birthDate: String
//    ): PeriodicWorkRequest {
//        return PeriodicWorkRequestBuilder<BirthdayNotificationWorker>(
//            BIRTHDAY_CHECK_INTERVAL_HOURS, TimeUnit.HOURS,
//            BIRTHDAY_FLEX_INTERVAL_HOURS, TimeUnit.HOURS
//        )
//            .setInputData(
//                workDataOf(
//                    BIRTHDAY_NOTIF_CHILD_NAME to childName,
//                    BIRTHDAY_NOTIF_BIRTHDAY_DAY to birthDate
//                )
//            )
//            .setConstraints(
//                Constraints(
//                    requiredNetworkType = NetworkType.NOT_REQUIRED,
//                    requiresBatteryNotLow = false,
//                    requiresCharging = false,
//                    requiresDeviceIdle = false,
//                    requiresStorageNotLow = false
//                )
////                Constraints.Builder()
////                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
////                    .build()
//            )
//            .setInitialDelay(
//                initialDelay,
//                TimeUnit.MILLISECONDS
//            ) // Иначе при запуске в 23 часа поздравления будут в это же время
////            .setInitialDelay(16, TimeUnit.MINUTES)
//            .addTag(BIRTHDAY_NOTIF_WORK_TAG)
//            .setBackoffCriteria(
//                BackoffPolicy.EXPONENTIAL,
//                15, TimeUnit.MINUTES
//            ) // Политика повторных попыток
//            .build()
//    }
//}


class BirthdayNotificationRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val alarmManager: AlarmManager,
    private val appDatastorePreferences: IAppDatastorePreferences,
    private val timeProvider: ITimeProvider,
    private val notificationHelper: NotificationHelper
) : IBirthdayNotificationRepository {
    companion object {
        private val TAG = "BirthdayNotificationRepository"
        const val ACTION_ALARM_TRIGGER = "com.example.ACTION_ALARM_TRIGGER"
    }

    /**
     * Основной метод планирования.
     * Вычисляет ближайшее 9:00 утра в день рождения (сегодня, завтра или в следующем году).
     * Если день рождения сегодня и время уже прошло (после 9:00) – показывает уведомление сразу,
     * затем планирует на следующий год.
     * Если день рождения сегодня до 9:00 – ставит будильник на сегодня.
     * Если день рождения завтра или позже – ставит будильник на этот день.
     */
    override suspend fun scheduleBirthdayNotification(childName: String, birthDay: String) {
        Log.d(TAG, "BIRTHDAY scheduleBirthdayNotification: name=$childName, birthDay=$birthDay")

        // 1. Сохраняем данные ребёнка в DataStore
        appDatastorePreferences.saveBirthdayNotificationData(
            BirthdayNotificationData(
                childName = childName,
                childBirthDate = birthDay
            )
        )

        // 2. Отменяем старый будильник (если есть)
        cancelAlarm()

        // 3. Парсим дату рождения
        val birthDate = birthDay.parseDateToLocalDate() ?: return
        val now = timeProvider.getCurrentDateTime()

        val (isToday, isYesterday) = BirthdayDateHelper.isBirthdayTodayOrYesterday(
            birthDate,
            now.toLocalDate()
        )

        when {
            isToday -> {
                // Сегодня – определяем время 9:00
                val today9am = LocalDateTime.of(now.toLocalDate(), LocalTime.of(9, 0))
                if (now.isBefore(today9am)) {
                    Log.d(TAG, "BIRTHDAY ДР сегодня, но ещё нет 9:00 - ставим будильник")
                    // Ставим будильник на сегодня 9:00
                    scheduleAlarm(
                        today9am.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                        childName,
                        birthDay
                    )
                } else {
                    Log.d(
                        TAG,
                        "BIRTHDAY ДР сегодня, но уже после 9:00 – показываем уведомление сразу (если ещё не уведомляли)"
                    )
                    // Уже после 9:00 – показываем сразу и планируем следующий год
                    showAndScheduleNextYearIfNeeded(childName, birthDay)
                }
            }

            isYesterday -> {
                Log.d(
                    TAG,
                    "BIRTHDAY ДР было вчера, но уведомление не было показано, показываем и планируем уведомление на следующий год"
                )
                // Вчера был ДР, но уведомление не пришло (телефон был выключен)
                showAndScheduleNextYearIfNeeded(childName, birthDay)
            }

            else -> {
                // ДР в будущем – ставим будильник на ближайшее 9:00
                Log.d(TAG, "BIRTHDAY Планируем уведомление на следующий год")
                val triggerTime =
                    BirthdayDateHelper.nextBirthdayMorning(birthDate, now)
                scheduleAlarm(triggerTime, childName, birthDay)
            }
        }
    }

    /**
     * Вспомогательный метод для планирования будильника на следующий год.
     * Вызывается из scheduleBirthdayNotification после показа уведомления,
     * а также может быть вызван из Worker для перепланирования после уведомления.
     */
    override suspend fun scheduleNextYearBirthday(childName: String, birthDay: String) {
        Log.d(TAG, "scheduleNextYearBirthday: name=$childName, birthDay=$birthDay")

        // Парсим дату рождения
        val birthDate = birthDay.parseDateToLocalDate() ?: return
        val now = timeProvider.getCurrentDateTime()

        // Следующий год – текущий год + 1, но с учётом, что если сейчас уже после ДР в этом году,
        // то следующий год может быть +1, но мы используем nextBirthdayMorning, который сам вычислит
        val triggerTime = BirthdayDateHelper.nextBirthdayMorning(birthDate, now)

        scheduleAlarm(triggerTime, childName, birthDay)
        Log.d(TAG, "scheduleNextYearBirthday: planned for $triggerTime")
    }

    override suspend fun cancelBirthdayNotification() {
        cancelAlarm()
        appDatastorePreferences.saveBirthdayNotificationData(BirthdayNotificationData())
    }

    /**
     * Проверяет, было ли уже уведомление в текущем году.
     * Если нет – показывает уведомление, сохраняет год и планирует на следующий год.
     * Возвращает true, если уведомление было показано.
     */
    override suspend fun showAndScheduleNextYearIfNeeded(
        childName: String,
        birthDay: String
    ): Boolean {
//        val currentYear = LocalDate.now().year
        val currentYear = timeProvider.getCurrentDate().year
        val lastNotifiedYear = appDatastorePreferences.getLastNotifiedYear()
        if (lastNotifiedYear < currentYear) {
            notificationHelper.showBirthdayNotificationIfPermitted(childName, birthDay)
            appDatastorePreferences.saveLastNotifiedYear(currentYear)
            scheduleNextYearBirthday(childName, birthDay)
            return true
        }
        return false
    }

    // ---------- Вспомогательные методы ----------

//    private fun parseBirthDate(dateStr: String): LocalDate? {
//        return try {
//            LocalDate.parse(dateStr) // формат yyyy-MM-dd
//        } catch (e: Exception) {
//            Log.e(TAG, "Ошибка парсинга даты: $dateStr", e)
//            null
//        }
//    }

    /**
     * Установка будильника через AlarmManager.
     * Использует setWindow с окном 15 минут (не требует разрешений).
     */
    private fun scheduleAlarm(triggerTime: Long, childName: String, birthDate: String) {
        val intent = Intent(context, AlarmBroadcastReceiver::class.java).apply {
            action = ACTION_ALARM_TRIGGER
            putExtra(BIRTH_NOTIF_EXTRA_CHILD_NAME, childName)
            putExtra(BIRTH_NOTIF_EXTRA_BIRTH_DATE, birthDate)
        }
        // Используем уникальный requestCode на основе имени + даты, чтобы различать детей
//        val requestCode = (childName + birthDate).hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            //            requestCode, - надо сохранять в дата стор, чтобы потом прочитать оттуда и отменить нужный реквест код
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Используем setWindow с окном 15 минут (можно 30)
        // Это не требует разрешений и не вызывает SecurityException
        alarmManager.setWindow(
            AlarmManager.RTC, // не будит телефон
            triggerTime,
            15 * 60 * 1000L, // окно 15 минут
            pendingIntent
        )
        Log.d(TAG, "BIRTHDAY Alarm scheduled at $triggerTime")
    }

    private fun scheduleAlarm(targetTime: LocalDateTime, childName: String, birthDate: String) {
        val triggerTime = targetTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        scheduleAlarm(triggerTime, childName, birthDate)
    }

    private fun cancelAlarm() {
        val intent = Intent(context, AlarmBroadcastReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }
}
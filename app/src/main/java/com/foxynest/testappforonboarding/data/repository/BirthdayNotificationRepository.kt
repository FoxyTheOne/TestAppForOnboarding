package com.foxynest.testappforonboarding.data.repository

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.foxynest.testappforonboarding.data.dataSource.datastorePreferences.IAppDatastorePreferences
import com.foxynest.testappforonboarding.data.notification.BirthdayNotificationReceiver
import com.foxynest.testappforonboarding.data.notification.NotificationHelper
import com.foxynest.testappforonboarding.domain.birthday.BirthdayRules
import com.foxynest.testappforonboarding.domain.entities.BirthdayNotificationData
import com.foxynest.testappforonboarding.domain.entities.isValidData
import com.foxynest.testappforonboarding.domain.entities.language.LanguageEnum
import com.foxynest.testappforonboarding.domain.iRepository.IBirthdayNotificationRepository
import com.foxynest.testappforonboarding.other.extensions.parseDateToLocalDate
import com.foxynest.testappforonboarding.other.extensions.toEpochMillis
import com.foxynest.testappforonboarding.other.timeProvider.ITimeProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android-часть напоминания: хранит данные ребёнка, показывает уведомление и ставит будильник на следующий раз.
 * Решения «пора ли поздравлять» и «на когда ставить будильник» принимает [BirthdayRules].
 *
 * Как устроен цикл (подробно – `docs/BIRTHDAY_NOTIFICATION.md`):
 * 1. Кто-то вызывает [update] или [rearmFromSavedData] (приложение, будильник, перезагрузка…).
 * 2. [notifyIfNeededAndArm]: если сегодня день рождения и ещё не поздравляли – показываем уведомление.
 * 3. В любом случае ставим будильник на ближайшие 9:00 дня рождения.
 * 4. Будильник срабатывает → `BirthdayNotificationReceiver` → снова шаг 1. Цепочка не обрывается сама.
 *
 * Почему AlarmManager, а не WorkManager: нам нужно «в конкретный день около 9:00», а WorkManager
 * умеет «когда-нибудь, когда системе удобно» – в экономном режиме задача может уехать на часы.
 *
 * Единственный источник данных для будильника – DataStore, а не extras интента:
 * интент «замораживает» данные на момент установки, а имя или дату могли с тех пор исправить.
 *
 * `@Singleton` – один объект на всё приложение, иначе у каждого экземпляра был бы свой [mutex] и он ничего бы не защищал.
 */
@Singleton
class BirthdayNotificationRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val alarmManager: AlarmManager,
    private val appDatastorePreferences: IAppDatastorePreferences,
    private val timeProvider: ITimeProvider,
    private val notificationHelper: NotificationHelper
) : IBirthdayNotificationRepository {
    companion object {
        private const val TAG = "BirthdayNotificationRepo"
        private const val ALARM_REQUEST_CODE = 0

        // Время примерное: система может доставить будильник в любой момент этого окна, экономя батарею
        private const val ALARM_WINDOW_MILLIS = 30 * 60 * 1000L
    }

    // Ресивер и UI могут вызвать планирование одновременно – без блокировки возможно двойное поздравление:
    // оба прочитают «ещё не поздравляли» раньше, чем кто-то из них запишет «поздравили».
    // Mutex – «очередь из одного»: вторая корутина ждёт (не блокируя поток), пока первая выйдет из withLock
    private val mutex = Mutex()

    override suspend fun update(childName: String, birthDate: String, userIsInApp: Boolean) =
        mutex.withLock {
            val newData =
                BirthdayNotificationData(childName = childName, childBirthDate = birthDate)
            if (appDatastorePreferences.getBirthdayNotificationData() != newData) {
                appDatastorePreferences.saveBirthdayNotificationData(newData)
            }
            notifyIfNeededAndArm(newData, userIsInApp)
        }

    override suspend fun rearmFromSavedData(userIsInApp: Boolean) = mutex.withLock {
        notifyIfNeededAndArm(appDatastorePreferences.getBirthdayNotificationData(), userIsInApp)
    }

    override suspend fun cancel() = mutex.withLock {
        cancelAlarm()
        appDatastorePreferences.clearBirthdayNotificationData()
    }

    override suspend fun wasNotificationPermissionAsked(): Boolean =
        appDatastorePreferences.wasNotificationPermissionAsked()

    override suspend fun markNotificationPermissionAsked() =
        appDatastorePreferences.markNotificationPermissionAsked()

    /** Сердце репозитория: поздравить (если пора) и поставить будильник на следующий раз. */
    private suspend fun notifyIfNeededAndArm(data: BirthdayNotificationData, userIsInApp: Boolean) {
        val birthDate = data.childBirthDate.parseDateToLocalDate()
        if (!data.isValidData() || birthDate == null) {
            // Сохранённых данных нет (не проходили анкету или вышли из аккаунта) – будильник не нужен
            cancelAlarm()
            return
        }

        val now = timeProvider.getCurrentDateTime()
        val lastNotifiedKey = appDatastorePreferences.getBirthdayLastNotifiedKey()
        if (BirthdayRules.shouldNotifyNow(birthDate, now, lastNotifiedKey, userIsInApp)) {
            val language = LanguageEnum.languageFromString(
                appDatastorePreferences.getAppLanguageFlow().first()
            )
            // «Поздравили» запоминаем только если уведомление действительно показано:
            // без разрешения на уведомления поздравление придёт позже в тот же день, когда разрешение дадут
            if (notificationHelper.showBirthdayNotification(data.childName, language)) {
                appDatastorePreferences.saveBirthdayLastNotifiedKey(
                    BirthdayRules.occurrenceKey(birthDate, now.toLocalDate())
                )
            }
        }

        scheduleAlarm(BirthdayRules.nextNotificationTime(birthDate, now))
    }

    /**
     * PendingIntent – «отложенное поручение»: мы отдаём его системе, и она выполнит его от имени приложения
     * (здесь – отправит broadcast нашему ресиверу), даже если процесс приложения давно закрыт.
     *
     * Флаги:
     * - FLAG_UPDATE_CURRENT – при установке: если такой PendingIntent уже есть, обновить его, а не создавать второй
     *   (поэтому будильник всегда один, сколько бы раз мы ни планировали);
     * - FLAG_NO_CREATE – при отмене: только найти существующий, не создавая новый (вернёт null, если будильника нет);
     * - FLAG_IMMUTABLE – обязателен с Android 12: никто другой не сможет подменить содержимое интента.
     */
    private fun alarmIntent(flags: Int): PendingIntent? {
        // Отмена и установка обязаны использовать один и тот же Intent (action + компонент),
        // иначе PendingIntent не совпадёт и cancel() ничего не отменит – так было в прежней версии
        val intent = Intent(context, BirthdayNotificationReceiver::class.java)
            .setAction(BirthdayNotificationReceiver.ACTION_BIRTHDAY_ALARM)
        return PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            flags or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun scheduleAlarm(triggerAt: LocalDateTime) {
        val pendingIntent = alarmIntent(PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        // RTC (не RTC_WAKEUP): не будит устройство, доставляется, когда телефон проснётся.
        // Это «обычное уведомление», а не будильник-будилка: экран не включается, звук – как у любого уведомления.
        // setWindow не требует разрешения на точные будильники (на Android 14+ оно по умолчанию запрещено).
        // Будильники AlarmManager стираются при перезагрузке – их восстанавливает ресивер по BOOT_COMPLETED
        alarmManager.setWindow(
            AlarmManager.RTC,
            triggerAt.toEpochMillis(),
            ALARM_WINDOW_MILLIS,
            pendingIntent
        )
        Log.d(TAG, "Birthday alarm scheduled at $triggerAt")
    }

    private fun cancelAlarm() {
        val pendingIntent = alarmIntent(PendingIntent.FLAG_NO_CREATE) ?: return
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
        Log.d(TAG, "Birthday alarm cancelled")
    }
}
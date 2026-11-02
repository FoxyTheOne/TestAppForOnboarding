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
 * Будильник AlarmManager (неточный, не будит устройство) + перепланирование по системным событиям.
 * Единственный источник данных для будильника – DataStore, а не extras интента.
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

    // Ресивер и UI могут вызвать планирование одновременно – без блокировки возможно двойное поздравление
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

    private suspend fun notifyIfNeededAndArm(data: BirthdayNotificationData, userIsInApp: Boolean) {
        val birthDate = data.childBirthDate.parseDateToLocalDate()
        if (!data.isValidData() || birthDate == null) {
            cancelAlarm()
            return
        }

        val now = timeProvider.getCurrentDateTime()
        val lastNotifiedKey = appDatastorePreferences.getBirthdayLastNotifiedKey()
        if (BirthdayRules.shouldNotifyNow(birthDate, now, lastNotifiedKey, userIsInApp)) {
            val language = LanguageEnum.languageFromString(
                appDatastorePreferences.getAppLanguageFlow().first()
            )
            if (notificationHelper.showBirthdayNotification(data.childName, language)) {
                appDatastorePreferences.saveBirthdayLastNotifiedKey(
                    BirthdayRules.occurrenceKey(birthDate, now.toLocalDate())
                )
            }
        }

        scheduleAlarm(BirthdayRules.nextNotificationTime(birthDate, now))
    }

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
        // setWindow не требует разрешения на точные будильники (на Android 14+ оно по умолчанию запрещено)
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
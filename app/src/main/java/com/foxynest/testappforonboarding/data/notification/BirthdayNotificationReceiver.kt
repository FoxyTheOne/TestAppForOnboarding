package com.foxynest.testappforonboarding.data.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.foxynest.testappforonboarding.domain.use_cases.notification.IScheduleBirthdayNotificationUC
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * Один ресивер на все события, после которых нужно поздравить и/или перепланировать будильник:
 * - наш будильник;
 * - перезагрузка (будильники AlarmManager при выключении стираются);
 * - ручная смена времени и часового пояса (будильник хранится в абсолютном времени, 9:00 «уедет»);
 * - обновление приложения.
 *
 * exported="false": системные broadcast-ы всё равно доставляются (так же объявлен RescheduleReceiver в WorkManager),
 * а сторонние приложения не смогут подделать будильник и показать «поздравление» с произвольным текстом.
 *
 * Работа короткая (чтение DataStore + уведомление), поэтому goAsync() вместо WorkManager:
 * WorkManager в фоне может отложить задачу, особенно в бакетах App Standby rare/restricted.
 */
class BirthdayNotificationReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ReceiverEntryPoint {
        fun scheduleBirthdayNotificationUC(): IScheduleBirthdayNotificationUC
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in HANDLED_ACTIONS) return
        Log.d(TAG, "onReceive: ${intent.action}")

        val scheduleUC = EntryPointAccessors
            .fromApplication(context.applicationContext, ReceiverEntryPoint::class.java)
            .scheduleBirthdayNotificationUC()

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                // goAsync даёт около 10 секунд, дольше система убьёт процесс (ANR для broadcast)
                withTimeout(TIMEOUT_MILLIS) {
                    scheduleUC.rearmFromSavedData(userIsInApp = false)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to handle ${intent.action}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "BirthdayReceiver"
        private const val TIMEOUT_MILLIS = 8_000L
        const val ACTION_BIRTHDAY_ALARM = "com.foxynest.testappforonboarding.action.BIRTHDAY_ALARM"

        private val HANDLED_ACTIONS = setOf(
            ACTION_BIRTHDAY_ALARM,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED
        )
    }
}
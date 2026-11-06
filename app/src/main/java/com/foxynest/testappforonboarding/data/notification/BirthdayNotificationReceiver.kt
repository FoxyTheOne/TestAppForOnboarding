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
 *
 * Что такое BroadcastReceiver: «приёмник широковещательных сообщений». Система (или будильник) рассылает
 * сообщение с action, а Android запускает процесс приложения и вызывает [onReceive] у того, кто подписан
 * на этот action в манифесте. Ни экранов, ни ViewModel тут нет – только этот один вызов.
 */
class BirthdayNotificationReceiver : BroadcastReceiver() {

    /**
     * Как получить объект из Hilt там, где конструктор вызывает не Hilt, а система.
     * Можно было бы пометить ресивер `@AndroidEntryPoint` и написать `@Inject lateinit var`, но тогда поля
     * заполняются только внутри `super.onReceive()`, и про этот вызов легко забыть. EntryPoint – явный «запрос»:
     * «дай мне use case из графа зависимостей приложения». Use case привязан в SingletonComponent, поэтому он здесь доступен.
     */
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

        // onReceive выполняется на главном потоке, и сразу после выхода из него система считает работу законченной
        // и может убить процесс. goAsync() говорит: «подожди, я ещё работаю» – до вызова pendingResult.finish().
        // Поэтому finish() стоит в finally: его нужно вызвать при любом исходе, даже при ошибке
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                // goAsync даёт около 10 секунд, дольше система убьёт процесс (ANR для broadcast)
                withTimeout(TIMEOUT_MILLIS) {
                    // userIsInApp = false: это фон, поэтому до 9:00 не поздравляем (см. BirthdayRules.shouldNotifyNow)
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
        // Свой action для нашего будильника. В манифесте его нет: ресивер получает его по явному Intent
        // (с указанием класса), а такие доставляются и без intent-filter
        const val ACTION_BIRTHDAY_ALARM = "com.foxynest.testappforonboarding.action.BIRTHDAY_ALARM"

        // Ресивер может получить и что-то неожиданное – обрабатываем только то, что ждём
        private val HANDLED_ACTIONS = setOf(
            ACTION_BIRTHDAY_ALARM,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED
        )
    }
}
package com.foxynest.testappforonboarding

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.foxynest.testappforonboarding.data.dataSource.datastorePreferences.AppDatastorePreferences
import com.foxynest.testappforonboarding.data.notification.BirthdayNotificationReceiver
import com.foxynest.testappforonboarding.data.repository.BirthdayNotificationRepository
import com.foxynest.testappforonboarding.other.timeProvider.FixedTimeProvider
import com.foxynest.testappforonboarding.data.notification.NotificationHelper
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime

/**
 * Проверка на настоящих AlarmManager / NotificationManager / DataStore (эмулятор или телефон).
 * Все даты – в будущем относительно реальных часов: будильник на прошедшее время сработал бы сразу
 * и вмешался в проверки через настоящий ресивер.
 */
@RunWith(AndroidJUnit4::class)
class BirthdayNotificationRepositoryTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val notificationManager = context.getSystemService(NotificationManager::class.java)
    private val dataStore = AppDatastorePreferences(context)

    private fun repositoryAt(now: String) = BirthdayNotificationRepository(
        context = context,
        alarmManager = context.getSystemService(AlarmManager::class.java),
        appDatastorePreferences = dataStore,
        timeProvider = LocalDateTime.parse(now).let { FixedTimeProvider(it.toLocalDate(), it) },
        notificationHelper = NotificationHelper(context)
    )

    private fun isAlarmScheduled(): Boolean = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, BirthdayNotificationReceiver::class.java)
            .setAction(BirthdayNotificationReceiver.ACTION_BIRTHDAY_ALARM),
        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
    ) != null

    private fun hasBirthdayNotification() = notificationManager.activeNotifications.isNotEmpty()

    @Before
    fun setUp() = runBlocking {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            InstrumentationRegistry.getInstrumentation().uiAutomation
                .grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        }
        notificationManager.cancelAll()
        repositoryAt("2027-01-01T12:00").cancel()
    }

    @After
    fun tearDown() = runBlocking {
        notificationManager.cancelAll()
        repositoryAt("2027-01-01T12:00").cancel()
    }

    @Test
    fun savedEveningBeforeBirthday_noNotificationNow_alarmScheduled() = runBlocking {
        repositoryAt("2027-03-09T21:30").update("Ваня", "2006-03-10", userIsInApp = true)

        assertFalse(hasBirthdayNotification())
        assertTrue(isAlarmScheduled())
    }

    @Test
    fun savedOnBirthday_notifiesImmediately_onlyOnce() = runBlocking {
        val repository = repositoryAt("2027-03-10T07:00")
        repository.update("Ваня", "2006-03-10", userIsInApp = true)
        assertTrue(hasBirthdayNotification())
        assertTrue(isAlarmScheduled())

        // Пользователь смахнул уведомление, приложение открыли ещё раз в тот же день
        notificationManager.cancelAll()
        repositoryAt("2027-03-10T18:00").rearmFromSavedData(userIsInApp = true)
        assertFalse(hasBirthdayNotification())
    }

    @Test
    fun deviceBootedOnBirthdayAt6_waitsUntil9() = runBlocking {
        repositoryAt("2027-03-09T12:00").update("Ваня", "2006-03-10", userIsInApp = true)

        repositoryAt("2027-03-10T06:00").rearmFromSavedData(userIsInApp = false)
        assertFalse(hasBirthdayNotification())

        repositoryAt("2027-03-10T10:00").rearmFromSavedData(userIsInApp = false)
        assertTrue(hasBirthdayNotification())
    }

    @Test
    fun rearmFromSavedData_keepsScheduleAndData() = runBlocking {
        repositoryAt("2027-03-09T12:00").update("Ваня", "2006-03-10", userIsInApp = true)
        assertTrue(isAlarmScheduled())

        repositoryAt("2027-03-09T12:00").rearmFromSavedData(userIsInApp = true)
        assertTrue(isAlarmScheduled())
        assertEquals("2006-03-10", dataStore.getBirthdayNotificationData().childBirthDate)
    }

    @Test
    fun cancel_removesAlarmAndData() = runBlocking {
        repositoryAt("2027-03-09T12:00").update("Ваня", "2006-03-10", userIsInApp = true)
        assertNotNull(dataStore.getBirthdayNotificationData().childName.ifEmpty { null })

        repositoryAt("2027-03-09T12:00").cancel()

        assertFalse(isAlarmScheduled())
        assertNull(dataStore.getBirthdayNotificationData().childName.ifEmpty { null })
    }
}
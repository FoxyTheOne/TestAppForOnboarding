package com.foxynest.testappforonboarding.data.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.foxynest.testappforonboarding.R
import com.foxynest.testappforonboarding.data.notification.NotificationHelper.Companion.BIRTHDAY_NOTIFICATION_ID
import com.foxynest.testappforonboarding.domain.entities.language.LanguageEnum
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject

/**
 * Показ уведомления «С днём рождения». Только отображение: когда показывать, решает `BirthdayNotificationRepository`.
 *
 * Что нужно знать про уведомления в Android:
 * - **Канал** (NotificationChannel, Android 8+) – «категория» уведомлений приложения. Пользователь может отключить
 *   или настроить каждый канал отдельно в настройках телефона. Важность (звук, всплывание) задаётся каналом.
 * - **Разрешение** POST_NOTIFICATIONS нужно с Android 13. Без него `notify()` молча ничего не покажет.
 * - **id уведомления** ([BIRTHDAY_NOTIFICATION_ID]) – повторный показ с тем же id заменяет старое, а не добавляет второе.
 */
class NotificationHelper @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "NotificationHelper"
        private const val BIRTHDAY_CHANNEL_ID = "birthday_channel"
        private const val BIRTHDAY_NOTIFICATION_ID = 1001
    }

    /**
     * @return true – уведомление действительно отправлено в систему.
     * false – нет разрешения или уведомления выключены: не считаем, что поздравили,
     * чтобы после выдачи разрешения в тот же день поздравление всё-таки пришло.
     */
    fun showBirthdayNotification(childName: String, language: LanguageEnum): Boolean {
        val notificationManager = NotificationManagerCompat.from(context)
        if (!hasPostPermission() || !notificationManager.areNotificationsEnabled()) {
            Log.d(TAG, "Notifications are not allowed, skipping birthday notification")
            return false
        }

        // Ресивер работает с Application context. На Android 12 и ниже он НЕ знает о языке,
        // выбранном через AppCompatDelegate, поэтому создаём контекст с нужной локалью сами.
        val localizedContext = context.withAppLanguage(language)
        createChannel(localizedContext, notificationManager)

        val channel = notificationManager.getNotificationChannel(BIRTHDAY_CHANNEL_ID)
        if (channel?.importance == NotificationManager.IMPORTANCE_NONE) {
            Log.d(TAG, "Birthday channel is disabled by user")
            return false
        }

        // Стартовый экран приложения берём у системы: data-слой не должен знать о классах экранов (presentation)
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            // Без CLEAR_TASK: не уничтожаем текущий стек экранов, если приложение уже открыто
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        }
        // Что произойдёт при нажатии на уведомление: откроется приложение
        val pendingIntent = intent?.let {
            PendingIntent.getActivity(
                context, 0, it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        // Версия для экрана блокировки без имени ребёнка: его видят все, кто берёт телефон в руки
        val publicNotification = NotificationCompat.Builder(localizedContext, BIRTHDAY_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications_black_24dp)
            .setContentTitle(localizedContext.getString(R.string.birthday_notification_public_title))
            .build()

        val notification = NotificationCompat.Builder(localizedContext, BIRTHDAY_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications_black_24dp)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicNotification)
            .setContentTitle(
                localizedContext.getString(
                    R.string.birthday_notification_title,
                    childName
                )
            )
            .setContentText(localizedContext.getString(R.string.birthday_notification_text))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(localizedContext.getString(R.string.birthday_notification_big_text))
            )
            // Обычное уведомление: без полноэкранного показа и без пробуждения экрана
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        return try {
            notificationManager.notify(BIRTHDAY_NOTIFICATION_ID, notification)
            true
        } catch (e: SecurityException) {
            // Разрешение могли отозвать между проверкой и показом
            Log.e(TAG, "No permission to post notification", e)
            false
        }
    }

    private fun hasPostPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED

    private fun createChannel(localizedContext: Context, manager: NotificationManagerCompat) {
        // Повторный вызов с тем же id безопасен: обновит имя/описание канала (например, после смены языка)
        val channel = NotificationChannel(
            BIRTHDAY_CHANNEL_ID,
            localizedContext.getString(R.string.birthday_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = localizedContext.getString(R.string.birthday_channel_description)
        }
        manager.createNotificationChannel(channel)
    }

    /** Копия контекста с нужным языком: getString() у неё возвращает строки из values-ru / values. */
    private fun Context.withAppLanguage(language: LanguageEnum): Context {
        val locale = when (language) {
            LanguageEnum.RU -> Locale("ru")
            LanguageEnum.EN -> Locale("en")
            LanguageEnum.SYSTEM -> return this
        }
        val configuration = Configuration(resources.configuration).apply { setLocale(locale) }
        return createConfigurationContext(configuration)
    }
}
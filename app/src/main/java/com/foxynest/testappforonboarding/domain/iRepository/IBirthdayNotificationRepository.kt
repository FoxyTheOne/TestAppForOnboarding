package com.foxynest.testappforonboarding.domain.iRepository

/**
 * Что domain-слою нужно от напоминаний о дне рождения. Реализация – `data/repository/BirthdayNotificationRepository`.
 *
 * Интерфейс живёт в domain, а реализация в data – это «инверсия зависимостей» из чистой архитектуры:
 * domain не знает про AlarmManager и DataStore, он знает только «запланируй / отмени».
 * Какой класс подставить вместо интерфейса, Hilt узнаёт из `@Binds` в `di/SingletonModule.kt`.
 */
interface IBirthdayNotificationRepository {
    /**
     * Сохраняет данные ребёнка и перепланирует уведомление. Идемпотентен – можно вызывать сколько угодно раз.
     * («Идемпотентный» – повторный вызов с теми же данными ничего не ломает и не дублирует: тот же будильник, без второго поздравления.)
     * @param userIsInApp true – пользователь сейчас в приложении, поздравить можно сразу (в любое время дня)
     */
    suspend fun update(childName: String, birthDate: String, userIsInApp: Boolean)

    /**
     * Перепланирует уведомление по ранее сохранённым данным
     * (срабатывание будильника, перезагрузка, смена времени/часового пояса, обновление приложения, старт приложения).
     */
    suspend fun rearmFromSavedData(userIsInApp: Boolean)

    /** Отменяет будильник и удаляет сохранённые данные (выход из аккаунта, удаление аккаунта). */
    suspend fun cancel()

    /** Спрашивали ли разрешение на уведомления в контексте поздравлений (спрашиваем один раз, без навязчивости). */
    suspend fun wasNotificationPermissionAsked(): Boolean

    /** Запоминает, что разрешение уже спрашивали – больше не показываем объяснение. */
    suspend fun markNotificationPermissionAsked()
}
package com.foxynest.testappforonboarding.domain.iRepository

interface IBirthdayNotificationRepository {
    /**
     * Сохраняет данные ребёнка и перепланирует уведомление. Идемпотентен – можно вызывать сколько угодно раз.
     * @param userIsInApp true – пользователь сейчас в приложении, поздравить можно сразу (в любое время дня)
     */
    suspend fun update(childName: String, birthDate: String, userIsInApp: Boolean)

    /**
     * Перепланирует уведомление по ранее сохранённым данным
     * (срабатывание будильника, перезагрузка, смена времени/часового пояса, обновление приложения, старт приложения).
     */
    suspend fun rearmFromSavedData(userIsInApp: Boolean)

    /** Отменяет будильник и удаляет сохранённые данные (выход из аккаунта). */
    suspend fun cancel()
}
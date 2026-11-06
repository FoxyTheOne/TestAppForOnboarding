package com.foxynest.testappforonboarding.domain.use_cases.notification

/**
 * Единая точка входа для всего, что связано с поздравлением. Её вызывают:
 * - `MainViewModel` – после анкеты, при запуске приложения, при изменении профиля в Firebase;
 * - `BirthdayNotificationReceiver` – будильник, перезагрузка, смена времени, обновление приложения;
 * - `LogoutUC` / `DeleteAccountUC` – отмена (через репозиторий).
 */
interface IScheduleBirthdayNotificationUC {
    /** Невалидные данные игнорируются – текущее расписание НЕ сбрасывается (например, профиль не загрузился без сети). */
    suspend fun update(childName: String, childBirthDate: String, userIsInApp: Boolean)

    /** Перепланировать по уже сохранённым данным (они могли быть сохранены давно, до перезагрузки). */
    suspend fun rearmFromSavedData(userIsInApp: Boolean)

    /** Отменить будильник и забыть данные ребёнка. */
    suspend fun cancel()

    /** Нужно ли показать объяснение «зачем нам уведомления» и спросить разрешение (только если ещё не спрашивали). */
    suspend fun shouldAskNotificationPermission(): Boolean
    suspend fun markNotificationPermissionAsked()
}
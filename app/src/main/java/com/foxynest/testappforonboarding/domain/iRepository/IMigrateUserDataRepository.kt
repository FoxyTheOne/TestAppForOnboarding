package com.foxynest.testappforonboarding.domain.iRepository

/** Перевод сохранённых данных в новый формат (подробно – в `MigrateUserDataRepository`). */
interface IMigrateUserDataRepository {
    /** Ничего не делает, если данные уже в текущем формате. Безопасно вызывать при каждом запуске. */
    suspend fun migrateIfNeeded()
}
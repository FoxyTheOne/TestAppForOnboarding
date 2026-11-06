package com.foxynest.testappforonboarding.domain.iRepository

import com.foxynest.testappforonboarding.domain.entities.language.LanguageEnum

/** Выбранный в приложении язык (копия для фонового кода, см. `AppLanguageRepository`). */
interface IAppLanguageRepository {
    /** Несмотря на название – разовое чтение, не Flow. */
    suspend fun getAppLanguageFlow(): LanguageEnum
    suspend fun saveAppLanguage(languageCode: LanguageEnum)

    /** Язык из старой версии приложения для одноразового переноса в AppCompat. null – переносить нечего/уже перенесли. */
    suspend fun takeLanguageForAppCompatMigration(): LanguageEnum?
}
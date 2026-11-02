package com.foxynest.testappforonboarding.domain.iRepository

import com.foxynest.testappforonboarding.domain.entities.language.LanguageEnum

interface IAppLanguageRepository {
    suspend fun getAppLanguageFlow(): LanguageEnum
    suspend fun saveAppLanguage(languageCode: LanguageEnum)

    /** Язык из старой версии приложения для одноразового переноса в AppCompat. null – переносить нечего/уже перенесли. */
    suspend fun takeLanguageForAppCompatMigration(): LanguageEnum?
}
package com.foxynest.testappforonboarding.domain.use_cases.appLanguage

import com.foxynest.testappforonboarding.domain.entities.language.LanguageEnum
import com.foxynest.testappforonboarding.domain.iRepository.IAppLanguageRepository
import javax.inject.Inject

/** Одноразовый перенос языка из старой версии приложения в AppCompat (см. LanguageManager.migrateLegacyLanguageIfNeeded). */
class TakeLanguageForMigrationUC @Inject constructor(
    private val appLanguageRepository: IAppLanguageRepository
) : ITakeLanguageForMigrationUC {
    override suspend fun invoke(): LanguageEnum? =
        appLanguageRepository.takeLanguageForAppCompatMigration()
}
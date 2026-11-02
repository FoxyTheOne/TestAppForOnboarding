package com.foxynest.testappforonboarding.data.repository

import com.foxynest.testappforonboarding.data.dataSource.datastorePreferences.IAppDatastorePreferences
import com.foxynest.testappforonboarding.domain.entities.language.LanguageEnum
import com.foxynest.testappforonboarding.domain.entities.language.LanguageEnum.Companion.languageFromString
import com.foxynest.testappforonboarding.domain.iRepository.IAppLanguageRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Язык интерфейса применяет и хранит AppCompat (autoStoreLocales в манифесте).
 * Копия выбора в DataStore нужна фоновому коду: уведомление на Android 12 и ниже не знает языка AppCompat.
 */
class AppLanguageRepository @Inject constructor(
    private val appDatastorePreferences: IAppDatastorePreferences
) : IAppLanguageRepository {
    override suspend fun getAppLanguageFlow(): LanguageEnum =
        languageFromString(appDatastorePreferences.getAppLanguageFlow().first())

    override suspend fun saveAppLanguage(languageCode: LanguageEnum) =
        appDatastorePreferences.saveAppLanguage(languageCode)

    override suspend fun takeLanguageForAppCompatMigration(): LanguageEnum? {
        if (appDatastorePreferences.isLanguageMigratedToAppCompat()) return null
        val saved = getAppLanguageFlow()
        appDatastorePreferences.markLanguageMigratedToAppCompat()
        return saved.takeIf { it != LanguageEnum.SYSTEM }
    }
}
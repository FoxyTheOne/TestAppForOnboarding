package com.foxynest.testappforonboarding.data.repository

import android.util.Log
import com.foxynest.testappforonboarding.data.dataSource.datastorePreferences.IAppDatastorePreferences
import com.foxynest.testappforonboarding.domain.entities.language.LanguageEnum
import com.foxynest.testappforonboarding.domain.entities.language.LanguageEnum.Companion.languageFromString
import com.foxynest.testappforonboarding.domain.iRepository.IAppLanguageRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Data layer, Repository. Работает с Local и Remote data source.
 *
 * Repository - объект, предоставляющий доступ к данным с возможностью выбора источника данных в зависимости от условий.
 * Подписка на локальную базу данных Room. Раскладываем данные.
 * При работе с model, здесь происходит запрос в remote, преобразование remote -> local, сохранение результата в базу данных.
 */
class AppLanguageRepository @Inject constructor(
    private val appDatastorePreferences: IAppDatastorePreferences
) : IAppLanguageRepository {
    override suspend fun getAppLanguageFlow(): LanguageEnum {
        val languageCodeString = appDatastorePreferences.getAppLanguageFlow().first()
        Log.d(
            "appDatastorePreferences",
            "Вызван метод getAppLanguageFlow, languageCodeString = $languageCodeString "
        )
        return languageFromString(languageCodeString)
    }

    override suspend fun saveAppLanguage(languageCode: LanguageEnum) {
        appDatastorePreferences.saveAppLanguage(languageCode)
        Log.d(
            "appDatastorePreferences",
            "Вызван метод saveAppLanguage, languageCode = $languageCode"
        )
    }
}
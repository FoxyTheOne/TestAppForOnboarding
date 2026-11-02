package com.foxynest.testappforonboarding.presentation.language

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.foxynest.testappforonboarding.domain.entities.language.LanguageEnum
import com.foxynest.testappforonboarding.domain.use_cases.appLanguage.ISetAppLanguageUC
import com.foxynest.testappforonboarding.domain.use_cases.appLanguage.ITakeLanguageForMigrationUC
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Применение языка интерфейса. Presentation-слой: зависит от AppCompat.
 *
 * AppCompat сам сохраняет выбор (autoStoreLocales в манифесте) и применяет его до показа первого экрана:
 * на Android 13+ через системный LocaleManager, ниже – через собственное хранилище.
 * Поэтому больше не нужен runBlocking в Application.onCreate.
 */
@Singleton
class LanguageManager @Inject constructor(
    private val setAppLanguageUC: ISetAppLanguageUC,
    private val takeLanguageForMigrationUC: ITakeLanguageForMigrationUC
) {
    suspend fun setLanguage(language: LanguageEnum) {
        setAppLanguageUC(language)
        applyLanguage(language)
    }

    /**
     * Одноразовый перенос выбора, сделанного в старой версии (он хранился только в DataStore).
     * Если первый экран успел открыться раньше, AppCompat один раз пересоздаст его уже с нужным языком.
     */
    suspend fun migrateLegacyLanguageIfNeeded() {
        takeLanguageForMigrationUC()?.let(::applyLanguage)
    }

    private fun applyLanguage(language: LanguageEnum) {
        val localeList = when (language) {
            LanguageEnum.RU -> LocaleListCompat.forLanguageTags("ru")
            LanguageEnum.EN -> LocaleListCompat.forLanguageTags("en")
            LanguageEnum.SYSTEM -> LocaleListCompat.getEmptyLocaleList() // пустой список = следовать системе
        }
        AppCompatDelegate.setApplicationLocales(localeList)
    }
}
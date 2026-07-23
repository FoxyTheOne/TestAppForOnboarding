package com.foxynest.testappforonboarding.domain.entities.language

/**
 * Центральный сервис для применения локали
 */
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.foxynest.testappforonboarding.domain.use_cases.appLanguage.ISetAppLanguageUC
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LanguageManager @Inject constructor(
    private val setAppLanguageUC: ISetAppLanguageUC
) {
    /**
     * Устанавливает новую локаль и сохраняет выбор.
     * @param languageCode "system", "ru", "en"
     * @param save true – сохранить в DataStore, false – только применить (например, при системном изменении)
     */
    suspend fun setLanguage(languageCode: LanguageEnum, save: Boolean = true) {
        if (save) setAppLanguageUC(languageCode)
        applyLanguage(languageCode)
    }

    private fun applyLanguage(languageCode: LanguageEnum) {
        val localeList = when (languageCode) {
            LanguageEnum.RU -> LocaleListCompat.forLanguageTags("ru")
            LanguageEnum.EN -> LocaleListCompat.forLanguageTags("en")
            else -> LocaleListCompat.getEmptyLocaleList() // пустой список = следовать системе
        }
        AppCompatDelegate.setApplicationLocales(localeList)
    }
}
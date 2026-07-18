package com.foxynest.testappforonboarding.domain.entities.language

/**
 * Центральный сервис для применения локали
 */
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.foxynest.testappforonboarding.data.dataSource.sharedPreferences.IAppSharedPreference
import com.foxynest.testappforonboarding.domain.use_cases.appLanguage.IGetAppLanguageUC
import com.foxynest.testappforonboarding.domain.use_cases.appLanguage.ISetAppLanguageUC
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

//@Singleton
//class LanguageManager @Inject constructor(
////    @ApplicationContext private val context: Context,
//    private val getAppLanguageUC: IGetAppLanguageUC,
//    private val setAppLanguageUC: ISetAppLanguageUC
//) {
//
////    /**
////     * Вызывается в Application.attachBaseContext, чтобы установить локаль до создания Activity.
////     * Использует сохранённое значение или системный язык.
////     */
////    fun applyLanguageAtStart() {
////        val languageCode = runBlocking { getAppLanguageUC() }
////        applyLanguage(languageCode, forceSystemDefault = false)
////    }
//
//    /**
//     * Устанавливает новую локаль и сохраняет выбор.
//     * @param languageCode "system", "ru", "en"
//     * @param save true – сохранить в DataStore, false – только применить (например, при системном изменении)
//     */
//    suspend fun setLanguage(languageCode: LanguageEnum, save: Boolean = true) {
//        if (save) setAppLanguageUC(languageCode)
//        applyLanguage(languageCode)
//    }
//
//    private fun applyLanguage(languageCode: LanguageEnum) {
//        val localeList = when (languageCode) {
//            LanguageEnum.RU -> LocaleListCompat.forLanguageTags("ru")
//            LanguageEnum.EN -> LocaleListCompat.forLanguageTags("en")
//            else -> LocaleListCompat.getEmptyLocaleList() // пустой список = следовать системе
//        }
//        AppCompatDelegate.setApplicationLocales(localeList)
//    }
//}

@Singleton
class LanguageManager @Inject constructor(
    private val setAppLanguageUC: ISetAppLanguageUC
) {
    suspend fun setLanguage(languageCode: LanguageEnum, save: Boolean = true) {
        if (save) setAppLanguageUC(languageCode)
        applyLanguage(languageCode)
    }

    private fun applyLanguage(languageCode: LanguageEnum) {
        val localeList = when (languageCode) {
            LanguageEnum.RU -> LocaleListCompat.forLanguageTags("ru")
            LanguageEnum.EN -> LocaleListCompat.forLanguageTags("en")
            else -> LocaleListCompat.getEmptyLocaleList()
        }
        AppCompatDelegate.setApplicationLocales(localeList)
    }
}
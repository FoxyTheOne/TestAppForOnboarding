package com.foxynest.testappforonboarding

import android.app.Application
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.foxynest.testappforonboarding.data.dataSource.datastorePreferences.IAppDatastorePreferences
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Locale
import javax.inject.Inject

@HiltAndroidApp
class App : Application(), Configuration.Provider {
    @Inject
    lateinit var dataStore: IAppDatastorePreferences

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        // Прогрев (убирает чёрный экран при первом переключении)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val defaultLocale = Locale.getDefault()
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(defaultLocale.language)
            )
        }
        applySavedLanguage()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    private fun applySavedLanguage() {
        val languageCode = runBlocking {
            dataStore.getAppLanguageFlow().first()
        }
        setLocale(languageCode)
    }

    private fun setLocale(languageCode: String) {
        val localeList = when (languageCode) {
            "ru" -> LocaleListCompat.forLanguageTags("ru")
            "en" -> LocaleListCompat.forLanguageTags("en")
            else -> LocaleListCompat.getEmptyLocaleList()
        }
        AppCompatDelegate.setApplicationLocales(localeList)
    }
}
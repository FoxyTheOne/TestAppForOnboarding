package com.foxynest.testappforonboarding

import android.app.Application
import android.os.Build
import android.util.Log
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

    override val workManagerConfiguration: Configuration
        get() {
            Log.d(
                "App",
                "Providing Birthday WorkManager configuration with factory: $workerFactory"
            )
            val configuration = Configuration.Builder()
                .setWorkerFactory(workerFactory) // Подменяем стандартную фабрику на фабрику от Hilt
                .build()
            Log.d(
                "App",
                "Providing Birthday WorkManager with configuration: $configuration"
            )
            return configuration
        }

    override fun onCreate() {
        super.onCreate()
        Log.d(
            "App",
            "onCreate: Birthday WorkManager config provided with factory: ${workManagerConfiguration.workerFactory}"
        )

        // Прогрев (убирает чёрный экран при первом переключении)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val defaultLocale = Locale.getDefault()
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(defaultLocale.language)
            )
        }
        applySavedLanguage()
    }

    // runBlocking блокирует главный поток на время чтения из DataStore, что может вызвать задержку запуска приложения (особенно если данные читаются с диска). Это не критично, но не рекомендуется.
    // Но в данном случае нам нужно применить язык до создания activity и прочего, поэтому здесь это не критично
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
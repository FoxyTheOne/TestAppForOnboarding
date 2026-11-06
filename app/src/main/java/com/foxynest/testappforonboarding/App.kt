package com.foxynest.testappforonboarding

import android.app.Application
import android.util.Log
import com.foxynest.testappforonboarding.presentation.language.LanguageManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Класс приложения: создаётся первым, раньше любого экрана, и живёт, пока жив процесс.
 * `@HiltAndroidApp` – отсюда Hilt начинает строить граф зависимостей. Без этой аннотации `@Inject` нигде не работает.
 * Подключён в манифесте: `android:name=".App"`.
 */
@HiltAndroidApp
class App : Application() {
    @Inject
    lateinit var languageManager: LanguageManager

    override fun onCreate() {
        super.onCreate()
        // Без runBlocking: главный поток не ждёт чтения с диска при каждом запуске.
        // Сам язык применяет AppCompat (autoStoreLocales), здесь только одноразовый перенос старого выбора.
        MainScope().launch {
            try {
                languageManager.migrateLegacyLanguageIfNeeded()
            } catch (e: Exception) {
                Log.e("App", "Language migration failed", e)
            }
        }
    }
}
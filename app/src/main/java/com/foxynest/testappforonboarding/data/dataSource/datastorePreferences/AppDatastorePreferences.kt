package com.foxynest.testappforonboarding.data.dataSource.datastorePreferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.foxynest.testappforonboarding.domain.entities.BirthdayNotificationData
import com.foxynest.testappforonboarding.domain.entities.language.LanguageEnum
import com.foxynest.testappforonboarding.domain.entities.onboarding.OnboardingAnswers
import com.foxynest.testappforonboarding.other.constants.CommonConstants.DATASTORE_NAME
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AppDatastorePreferences @Inject constructor(@ApplicationContext val context: Context) :
    IAppDatastorePreferences {

    // Делегат для создания DataStore
    private val Context.dataStore by preferencesDataStore(DATASTORE_NAME)
    private val gson = Gson()

    // Ключи для онбординга
    object PreferencesKeys {
        // Migration. Теперь миграция запустится только один раз – при первом запуске после обновления приложения.
        val DATA_SCHEMA_VERSION = intPreferencesKey("data_schema_version")

        // Onboarding
        val IS_ONBOARDING_COMPLETED = booleanPreferencesKey("is_onboarding_completed")

        // ЕДИНСТВЕННЫЙ ключ для всех ответов онбординга
        val ONBOARDING_ANSWERS = stringPreferencesKey("onboarding_answers_json")

        // Переключение языка
        val APP_LANGUAGE = stringPreferencesKey("app_language")

        // Birthday notification
        val BIRTHDAY_NOTIF_DATA = stringPreferencesKey("birthday_notification_data")
    }

    // Сохраняем весь объект целиком
    override suspend fun saveOnboardingAnswers(answers: OnboardingAnswers) {
        val jsonString = gson.toJson(answers)
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.ONBOARDING_ANSWERS] = jsonString
        }
    }

    // Получаем весь объект (при ошибке – пустой объект)
    override fun getOnboardingAnswersFlow(): Flow<OnboardingAnswers> = context.dataStore.data
        .map { prefs ->
            val jsonString = prefs[PreferencesKeys.ONBOARDING_ANSWERS]
            if (jsonString.isNullOrBlank()) {
                OnboardingAnswers()  // данные не найдены – пустой объект
            } else {
                try {
                    // Десериализуем JSON обратно в OnboardingAnswers
                    val type = object : TypeToken<OnboardingAnswers>() {}.type
                    gson.fromJson(jsonString, type) ?: OnboardingAnswers()
                } catch (e: Exception) {
                    // Ошибка парсинга – возвращаем пустой объект
                    OnboardingAnswers()
                }
            }
        }

    override suspend fun saveBirthdayNotificationData(birthNotifData: BirthdayNotificationData) {
        val jsonString = gson.toJson(birthNotifData)
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.BIRTHDAY_NOTIF_DATA] = jsonString
        }
    }

    override fun getBirthdayNotificationDataFlow(): Flow<BirthdayNotificationData> = context.dataStore.data
        .map { prefs ->
            val jsonString = prefs[PreferencesKeys.BIRTHDAY_NOTIF_DATA]
            if (jsonString.isNullOrBlank()) {
                BirthdayNotificationData()  // данные не найдены – пустой объект
            } else {
                try {
                    // Десериализуем JSON обратно в BirthdayNotificationData
                    val type = object : TypeToken<BirthdayNotificationData>() {}.type
                    gson.fromJson(jsonString, type) ?: BirthdayNotificationData()
                } catch (e: Exception) {
                    // Ошибка парсинга – возвращаем пустой объект
                    BirthdayNotificationData()
                }
            }
        }

    override suspend fun clearBirthdayNotificationData() {
        context.dataStore.edit { prefs ->
            prefs.remove(PreferencesKeys.BIRTHDAY_NOTIF_DATA) // полностью удаляем ключ
        }
    }

    override suspend fun saveOnboardingState(isCompleted: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_ONBOARDING_COMPLETED] = isCompleted
        }
    }

    override fun isOnboardingCompleted(): Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.IS_ONBOARDING_COMPLETED] ?: false
        }

    // Очистка всех данных
    override suspend fun clearAllOnboardingData() {
        context.dataStore.edit { prefs ->
            // Очищаем все ключи онбординга
            prefs.remove(PreferencesKeys.ONBOARDING_ANSWERS)
            // Сбрасываем статус онбординга на false
            prefs.remove(PreferencesKeys.IS_ONBOARDING_COMPLETED)
        }
    }

    // Migration. Теперь миграция запустится только один раз – при первом запуске после обновления приложения.
    override suspend fun getSchemaVersion(): Int {
        return context.dataStore.data.map { prefs ->
            prefs[PreferencesKeys.DATA_SCHEMA_VERSION] ?: 0
        }.first()
    }

    override suspend fun setSchemaVersion(version: Int) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.DATA_SCHEMA_VERSION] = version
        }
    }

    // Переключение языка
    override suspend fun saveAppLanguage(languageCode: LanguageEnum) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.APP_LANGUAGE] = languageCode.toString().lowercase()
        }
    }

    override fun getAppLanguageFlow(): Flow<String> = context.dataStore.data
        .map { prefs -> prefs[PreferencesKeys.APP_LANGUAGE] ?: "system" }
        .catch { emit("system") } // на случай ошибки
}
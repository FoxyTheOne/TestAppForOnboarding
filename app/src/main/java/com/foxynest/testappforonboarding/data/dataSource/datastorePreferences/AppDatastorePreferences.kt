package com.foxynest.testappforonboarding.data.dataSource.datastorePreferences

import android.content.Context
import android.util.Log
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.foxynest.testappforonboarding.domain.entities.BirthdayNotificationData
import com.foxynest.testappforonboarding.domain.entities.language.LanguageEnum
import com.foxynest.testappforonboarding.domain.entities.onboarding.OnboardingAnswers
import com.foxynest.testappforonboarding.other.constants.CommonConstants.DATASTORE_NAME
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Делегат обязан быть top-level (см. документацию DataStore). Раньше он был свойством класса:
 * каждый новый экземпляр AppDatastorePreferences создавал бы свой DataStore на тот же файл,
 * что приводит к IllegalStateException "multiple DataStores active for the same file".
 */
private val Context.appDataStore by preferencesDataStore(
    name = DATASTORE_NAME,
    // Без обработчика повреждённый файл даёт CorruptionException при каждом чтении – приложение не запустится никогда
    corruptionHandler = ReplaceFileCorruptionHandler {
        Log.e(
            "AppDatastorePreferences",
            "DataStore file is corrupted, replacing with empty preferences",
            it
        )
        emptyPreferences()
    }
)

@Singleton
class AppDatastorePreferences @Inject constructor(
    @ApplicationContext private val context: Context
) : IAppDatastorePreferences {

    private val gson = Gson()

    private object PreferencesKeys {
        // Migration. Миграция запускается один раз на каждую новую версию схемы
        val DATA_SCHEMA_VERSION = intPreferencesKey("data_schema_version")

        // Onboarding
        val IS_ONBOARDING_COMPLETED = booleanPreferencesKey("is_onboarding_completed")

        // Подтверждённые ответы (показываются в профиле и уходят в Firebase)
        val ONBOARDING_ANSWERS = stringPreferencesKey("onboarding_answers_json")

        // Черновик во время прохождения опроса. Подтверждённые ответы не трогаются, пока опрос не завершён
        val ONBOARDING_DRAFT = stringPreferencesKey("onboarding_draft_json")

        // Переключение языка
        val APP_LANGUAGE = stringPreferencesKey("app_language")

        // Birthday notification
        val BIRTHDAY_NOTIF_DATA = stringPreferencesKey("birthday_notification_data")
        val BIRTHDAY_LAST_NOTIFIED_KEY = stringPreferencesKey("birthday_last_notified_key")
        val LEGACY_LAST_NOTIFIED_YEAR = intPreferencesKey("last_notified_year")
    }

    // Рекомендация документации: IOException при чтении превращаем в пустые настройки, остальные ошибки пробрасываем
    private val data: Flow<Preferences> = context.appDataStore.data.catch { e ->
        if (e is IOException) {
            Log.e(TAG, "Error reading DataStore", e)
            emit(emptyPreferences())
        } else {
            throw e
        }
    }

    private inline fun <reified T> Preferences.readJson(
        key: Preferences.Key<String>,
        default: T
    ): T {
        val json = this[key]
        if (json.isNullOrBlank()) return default
        return try {
            gson.fromJson(json, T::class.java) ?: default
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse ${key.name}", e)
            default
        }
    }

    private fun MutablePreferences.writeJson(key: Preferences.Key<String>, value: Any) {
        this[key] = gson.toJson(value)
    }

    // ONBOARDING ANSWERS
    override suspend fun saveOnboardingAnswers(answers: OnboardingAnswers) {
        context.appDataStore.edit { it.writeJson(PreferencesKeys.ONBOARDING_ANSWERS, answers) }
    }

    override fun getOnboardingAnswersFlow(): Flow<OnboardingAnswers> =
        data.map { it.readJson(PreferencesKeys.ONBOARDING_ANSWERS, OnboardingAnswers()) }

    // ONBOARDING DRAFT
    override suspend fun startOnboardingDraft() {
        context.appDataStore.edit {
            it.writeJson(
                PreferencesKeys.ONBOARDING_DRAFT,
                OnboardingAnswers()
            )
        }
    }

    // Чтение и запись внутри одного edit – атомарно, параллельные обновления разных полей не затирают друг друга
    override suspend fun updateOnboardingDraft(transform: (OnboardingAnswers) -> OnboardingAnswers) {
        context.appDataStore.edit { prefs ->
            val current = prefs.readJson(PreferencesKeys.ONBOARDING_DRAFT, OnboardingAnswers())
            prefs.writeJson(PreferencesKeys.ONBOARDING_DRAFT, transform(current))
        }
    }

    override fun getOnboardingDraftFlow(): Flow<OnboardingAnswers> =
        data.map { it.readJson(PreferencesKeys.ONBOARDING_DRAFT, OnboardingAnswers()) }

    override suspend fun clearOnboardingDraft() {
        context.appDataStore.edit { it.remove(PreferencesKeys.ONBOARDING_DRAFT) }
    }

    override suspend fun commitOnboardingDraft(isValid: (OnboardingAnswers) -> Boolean): OnboardingAnswers? {
        var committed: OnboardingAnswers? = null
        context.appDataStore.edit { prefs ->
            if (prefs[PreferencesKeys.ONBOARDING_DRAFT] == null) return@edit
            val draft = prefs.readJson(PreferencesKeys.ONBOARDING_DRAFT, OnboardingAnswers())
            if (!isValid(draft)) return@edit
            // Одна транзакция: либо записано всё, либо ничего
            prefs.writeJson(PreferencesKeys.ONBOARDING_ANSWERS, draft)
            prefs[PreferencesKeys.IS_ONBOARDING_COMPLETED] = true
            prefs.remove(PreferencesKeys.ONBOARDING_DRAFT)
            committed = draft
        }
        return committed
    }

    // BIRTHDAY NOTIFICATION
    override suspend fun saveBirthdayNotificationData(birthNotifData: BirthdayNotificationData) {
        context.appDataStore.edit {
            it.writeJson(
                PreferencesKeys.BIRTHDAY_NOTIF_DATA,
                birthNotifData
            )
        }
    }

    override suspend fun getBirthdayNotificationData(): BirthdayNotificationData =
        data.first().readJson(PreferencesKeys.BIRTHDAY_NOTIF_DATA, BirthdayNotificationData())

    override suspend fun clearBirthdayNotificationData() {
        context.appDataStore.edit { prefs ->
            prefs.remove(PreferencesKeys.BIRTHDAY_NOTIF_DATA)
            prefs.remove(PreferencesKeys.BIRTHDAY_LAST_NOTIFIED_KEY)
            prefs.remove(PreferencesKeys.LEGACY_LAST_NOTIFIED_YEAR)
        }
    }

    override suspend fun getBirthdayLastNotifiedKey(): String? =
        data.first()[PreferencesKeys.BIRTHDAY_LAST_NOTIFIED_KEY]

    override suspend fun saveBirthdayLastNotifiedKey(key: String) {
        context.appDataStore.edit { it[PreferencesKeys.BIRTHDAY_LAST_NOTIFIED_KEY] = key }
    }

    // ONBOARDING STATE
    override suspend fun saveOnboardingState(isCompleted: Boolean) {
        context.appDataStore.edit { it[PreferencesKeys.IS_ONBOARDING_COMPLETED] = isCompleted }
    }

    override fun isOnboardingCompleted(): Flow<Boolean> =
        data.map { it[PreferencesKeys.IS_ONBOARDING_COMPLETED] ?: false }

    override suspend fun clearAllOnboardingData() {
        context.appDataStore.edit { prefs ->
            prefs.remove(PreferencesKeys.ONBOARDING_ANSWERS)
            prefs.remove(PreferencesKeys.ONBOARDING_DRAFT)
            prefs.remove(PreferencesKeys.IS_ONBOARDING_COMPLETED)
        }
    }

    // MIGRATION
    override suspend fun getSchemaVersion(): Int =
        data.first()[PreferencesKeys.DATA_SCHEMA_VERSION] ?: 0

    override suspend fun setSchemaVersion(version: Int) {
        context.appDataStore.edit { it[PreferencesKeys.DATA_SCHEMA_VERSION] = version }
    }

    // LANGUAGE
    override suspend fun saveAppLanguage(languageCode: LanguageEnum) {
        context.appDataStore.edit {
            it[PreferencesKeys.APP_LANGUAGE] = LanguageEnum.stringFromLanguage(languageCode)
        }
    }

    override fun getAppLanguageFlow(): Flow<String> =
        data.map {
            it[PreferencesKeys.APP_LANGUAGE] ?: LanguageEnum.stringFromLanguage(LanguageEnum.SYSTEM)
        }

    private companion object {
        const val TAG = "AppDatastorePreferences"
    }
}
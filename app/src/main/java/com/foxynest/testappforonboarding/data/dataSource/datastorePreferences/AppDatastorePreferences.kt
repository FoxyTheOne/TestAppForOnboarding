package com.foxynest.testappforonboarding.data.dataSource.datastorePreferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.foxynest.testappforonboarding.data.dataSource.datastorePreferences.AppDatastorePreferences.PreferencesKeys.IS_ONBOARDING_COMPLETED
import com.foxynest.testappforonboarding.other.constants.CommonConstants.DATASTORE_NAME
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AppDatastorePreferences @Inject constructor(@ApplicationContext val context: Context) :
    IAppDatastorePreferences {

    // Делегат для создания DataStore
    private val Context.dataStore by preferencesDataStore(DATASTORE_NAME)

    // Ключи для онбординга
    // val PONY = booleanPreferencesKey("pony")
    object PreferencesKeys {
        val IS_ONBOARDING_COMPLETED = booleanPreferencesKey("is_onboarding_completed")
        val GENDER = stringPreferencesKey("gender")
        val NAME = stringPreferencesKey("name")
        val IS_IT_A_BOY = stringPreferencesKey("is_it_a_boy")
        val HOBBY = stringPreferencesKey("hobby")
    }

    override suspend fun saveOnboardingState(isCompleted: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_ONBOARDING_COMPLETED] = isCompleted
        }
    }

    override fun isOnboardingCompleted(): Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[IS_ONBOARDING_COMPLETED] ?: false
        }

    // Сохранение данных
    override suspend fun saveGender(gender: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.GENDER] = gender
        }
    }

//    override suspend fun saveName(name: Boolean) {
//        context.dataStore.edit { preferences ->
//            preferences[PreferencesKeys.PONY] = name
//        }
//    }

    override suspend fun saveName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NAME] = name
        }
    }

    override suspend fun saveIsItABoy(answer: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_IT_A_BOY] = answer
        }
    }

    override suspend fun saveHobbyAnswer(hobby: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HOBBY] = hobby
        }
    }

    // Получение данных
    override fun getGenderFlow(): Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.GENDER] }

    override fun getNameFlow(): Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.NAME] }

    override fun getIsItABoyFlow(): Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.IS_IT_A_BOY] }

    override fun getHobbyFlow(): Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.HOBBY] }
}
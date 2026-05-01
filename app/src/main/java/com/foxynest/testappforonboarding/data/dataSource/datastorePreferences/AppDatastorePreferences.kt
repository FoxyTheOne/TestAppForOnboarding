package com.foxynest.testappforonboarding.data.dataSource.datastorePreferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.foxynest.testappforonboarding.data.dataSource.datastorePreferences.AppDatastorePreferences.PreferencesKeys.IS_ONBOARDING_COMPLETED
import com.foxynest.testappforonboarding.domain.entities.onboarding.OnboardingAnswers
import com.foxynest.testappforonboarding.other.constants.CommonConstants.DATASTORE_NAME
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AppDatastorePreferences @Inject constructor(@ApplicationContext val context: Context) :
    IAppDatastorePreferences {

    // Делегат для создания DataStore
    private val Context.dataStore by preferencesDataStore(DATASTORE_NAME)

    private val gson = Gson()

    // Ключи для онбординга
    object PreferencesKeys {
        val IS_ONBOARDING_COMPLETED = booleanPreferencesKey("is_onboarding_completed")
        // ЕДИНСТВЕННЫЙ ключ для всех ответов онбординга
        val ONBOARDING_ANSWERS = stringPreferencesKey("onboarding_answers_json")
//        val USER_NAME = stringPreferencesKey("user_name")
//        val GENDER = stringPreferencesKey("gender")
//        val CHILD_NAME = stringPreferencesKey("child_name")
//        val IS_IT_A_BOY = stringPreferencesKey("is_it_a_boy")
//        val BIRTH_DATE = stringPreferencesKey("birth_date")
//        val CHILD_AGE = intPreferencesKey("child_age")
// //        val HOBBY = stringPreferencesKey("hobby")
//        val HOBBIES = stringPreferencesKey("hobbies")
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

//    override suspend fun clearAllOnboardingData() {
//        context.dataStore.edit { preferences ->
//            // Очищаем все ключи онбординга
//            preferences.remove(PreferencesKeys.USER_NAME)
//            preferences.remove(PreferencesKeys.GENDER)
//            preferences.remove(PreferencesKeys.CHILD_NAME)
//            preferences.remove(PreferencesKeys.IS_IT_A_BOY)
//            preferences.remove(PreferencesKeys.BIRTH_DATE)
//            preferences.remove(PreferencesKeys.CHILD_AGE)
//            preferences.remove(PreferencesKeys.HOBBIES)
//
//            // Сбрасываем статус онбординга на false
//            preferences[IS_ONBOARDING_COMPLETED] = false
//        }
//    }

//    // Сохранение данных
//    override suspend fun saveUserName(userName: String) {
//        context.dataStore.edit { preferences ->
//            preferences[PreferencesKeys.USER_NAME] = userName
//        }
//    }
//
//    override suspend fun saveGender(gender: String) {
//        context.dataStore.edit { preferences ->
//            preferences[PreferencesKeys.GENDER] = gender
//        }
//    }
//
//    override suspend fun saveName(name: String) {
//        context.dataStore.edit { preferences ->
//            preferences[PreferencesKeys.CHILD_NAME] = name
//        }
//    }
//
//    override suspend fun saveIsItABoy(answer: String) {
//        context.dataStore.edit { preferences ->
//            preferences[PreferencesKeys.IS_IT_A_BOY] = answer
//        }
//    }
//
//    override suspend fun saveBirthDate(birthDate: String) {
//        context.dataStore.edit { preferences ->
//            preferences[PreferencesKeys.BIRTH_DATE] = birthDate
//        }
//    }
//
//    override suspend fun saveChildAge(age: Int) {
//        context.dataStore.edit { preferences ->
//            preferences[PreferencesKeys.CHILD_AGE] = age
//        }
//    }
//
////    override suspend fun saveHobbyAnswer(hobby: String) {
////        context.dataStore.edit { preferences ->
////            preferences[PreferencesKeys.HOBBY] = hobby
////        }
////    }
//
//    override suspend fun saveHobbies(hobbies: String) {
//        context.dataStore.edit { preferences ->
//            preferences[PreferencesKeys.HOBBIES] = hobbies
//        }
//    }

//    // Получение данных
//    override fun getUserNameFlow(): Flow<String?> = context.dataStore.data
//        .map { preferences -> preferences[PreferencesKeys.USER_NAME] }
//
//    override fun getGenderFlow(): Flow<String?> = context.dataStore.data
//        .map { preferences -> preferences[PreferencesKeys.GENDER] }
//
//    override fun getNameFlow(): Flow<String?> = context.dataStore.data
//        .map { preferences -> preferences[PreferencesKeys.CHILD_NAME] }
//
//    override fun getIsItABoyFlow(): Flow<String?> = context.dataStore.data
//        .map { preferences -> preferences[PreferencesKeys.IS_IT_A_BOY] }
//
//    override fun getBirthDateFlow(): Flow<String?> = context.dataStore.data
//        .map { preferences -> preferences[PreferencesKeys.BIRTH_DATE] }
//
//    override fun getChildAgeFlow(): Flow<Int?> = context.dataStore.data
//        .map { preferences -> preferences[PreferencesKeys.CHILD_AGE] }
//
////    override fun getHobbyFlow(): Flow<String?> = context.dataStore.data
////        .map { preferences -> preferences[PreferencesKeys.HOBBY] }
//
//    override fun getHobbiesFlow(): Flow<String?> = context.dataStore.data
//        .map { preferences -> preferences[PreferencesKeys.HOBBIES] }
}
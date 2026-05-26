package com.foxynest.testappforonboarding.data.dataSource.datastorePreferences

import com.foxynest.testappforonboarding.domain.entities.onboarding.OnboardingAnswers
import kotlinx.coroutines.flow.Flow

interface IAppDatastorePreferences {
    // Migration
    suspend fun getSchemaVersion(): Int
    suspend fun setSchemaVersion(version: Int)

    // Onboarding
    suspend fun saveOnboardingState(isCompleted: Boolean)
    fun isOnboardingCompleted(): Flow<Boolean>
    suspend fun clearAllOnboardingData()

    // Сохранение данных
    suspend fun saveOnboardingAnswers(answers: OnboardingAnswers)

//    suspend fun saveUserName(userName: String)
//    suspend fun saveGender(gender: String)
//    suspend fun saveName(name: String)
//    suspend fun saveIsItABoy(answer: String)
//    suspend fun saveBirthDate(birthDate: String)
//    suspend fun saveChildAge(childAge: Int)
//    suspend fun saveHobbies(hobby: String)

    // Получение данных
    fun getOnboardingAnswersFlow(): Flow<OnboardingAnswers>

//    fun getUserNameFlow(): Flow<String?>
//    fun getGenderFlow(): Flow<String?>
//    fun getNameFlow(): Flow<String?>
//    fun getIsItABoyFlow(): Flow<String?>
//    fun getBirthDateFlow(): Flow<String?>
//    fun getChildAgeFlow(): Flow<Int?>
//    fun getHobbiesFlow(): Flow<String?>
}
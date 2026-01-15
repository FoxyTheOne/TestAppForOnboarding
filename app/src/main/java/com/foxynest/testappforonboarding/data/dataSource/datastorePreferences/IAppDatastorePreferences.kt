package com.foxynest.testappforonboarding.data.dataSource.datastorePreferences

import kotlinx.coroutines.flow.Flow

interface IAppDatastorePreferences {
    suspend fun saveOnboardingState(isCompleted: Boolean)
    fun isOnboardingCompleted(): Flow<Boolean>
    suspend fun clearAllOnboardingData()

    // Сохранение данных
    suspend fun saveUserName(userName: String)
    suspend fun saveGender(gender: String)
    suspend fun saveName(name: String)
    suspend fun saveIsItABoy(answer: String)
    suspend fun saveBirthDate(birthDate: String)
    suspend fun saveChildAge(childAge: Int)
    suspend fun saveHobbyAnswer(hobby: String)

    // Получение данных
    fun getUserNameFlow(): Flow<String?>
    fun getGenderFlow(): Flow<String?>
    fun getNameFlow(): Flow<String?>
    fun getIsItABoyFlow(): Flow<String?>
    fun getBirthDateFlow(): Flow<String?>
    fun getChildAgeFlow(): Flow<Int?>
    fun getHobbyFlow(): Flow<String?>
}
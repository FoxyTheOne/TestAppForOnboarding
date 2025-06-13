package com.foxynest.testappforonboarding.data.dataSource.datastorePreferences

import kotlinx.coroutines.flow.Flow

interface IAppDatastorePreferences {
    suspend fun saveOnboardingState(isCompleted: Boolean)
    fun isOnboardingCompleted(): Flow<Boolean>

    // Сохранение данных
    suspend fun saveGender(gender: String)
    suspend fun saveName(name: String)
    suspend fun saveIsItABoy(answer: String)
    suspend fun saveHobbyAnswer(hobby: String)

    // Получение данных
    fun getGenderFlow(): Flow<String?>
    fun getNameFlow(): Flow<String?>
    fun getIsItABoyFlow(): Flow<String?>
    fun getHobbyFlow(): Flow<String?>
}
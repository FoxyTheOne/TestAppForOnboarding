package com.foxynest.testappforonboarding.domain.iRepository

import kotlinx.coroutines.flow.Flow

interface IOnboardingRepository {
    suspend fun saveOnboardingState(isCompleted: Boolean)
    fun isOnboardingCompleted(): Flow<Boolean>

    suspend fun saveUserName(userName: String)
    suspend fun saveEmail(email: String)
    suspend fun saveGender(gender: String)
    suspend fun saveName(name: String)
    suspend fun saveIsItABoy(answer: String)
    suspend fun saveBirthDate(birthDate: String)
    suspend fun saveChildAge(age: Int)
    suspend fun saveHobbyAnswer(hobby: String)

    fun getUserNameFlow(): Flow<String?>
    fun getEmailFlow(): Flow<String?>
    fun getGenderFlow(): Flow<String?>
    fun getNameFlow(): Flow<String?>
    fun getIsItABoyFlow(): Flow<String?>
    fun getBirthDateFlow(): Flow<String?>
    fun getChildAgeFlow(): Flow<Int?>
    fun getHobbyFlow(): Flow<String?>
}
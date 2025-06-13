package com.foxynest.testappforonboarding.domain.onboardingAndPaywallUseCase

import kotlinx.coroutines.flow.Flow

interface IOnboardingUseCase {
    suspend fun saveOnboardingState(isCompleted: Boolean)
    fun isOnboardingCompleted(): Flow<Boolean>

    suspend fun saveGender(gender: String)
    suspend fun saveName(name: String)
    suspend fun saveIsItABoy(answer: String)
    suspend fun saveHobbyAnswer(hobby: String)

    fun getGenderFlow(): Flow<String?>
    fun getNameFlow(): Flow<String?>
    fun getIsItABoyFlow(): Flow<String?>
    fun getHobbyFlow(): Flow<String?>
}
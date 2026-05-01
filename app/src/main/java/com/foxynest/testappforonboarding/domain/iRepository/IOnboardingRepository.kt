package com.foxynest.testappforonboarding.domain.iRepository

import com.foxynest.testappforonboarding.domain.entities.ErrorType
import com.foxynest.testappforonboarding.domain.entities.onboarding.OnboardingAnswers
import com.foxynest.testappforonboarding.other.Resource
import kotlinx.coroutines.flow.Flow

interface IOnboardingRepository {
    suspend fun saveOnboardingState(isCompleted: Boolean): Resource<ErrorType>
    fun isOnboardingCompleted(): Flow<Boolean>
    suspend fun clearAllOnboardingData(): Resource<ErrorType>

    suspend fun saveOnboardingAnswers(answers: OnboardingAnswers)
//    suspend fun saveUserName(userName: String)
//    suspend fun saveGender(gender: String)
//    suspend fun saveName(name: String)
//    suspend fun saveIsItABoy(answer: String)
//    suspend fun saveBirthDate(birthDate: String)
//    suspend fun saveChildAge(age: Int)
//    suspend fun saveHobbies(hobbies: String)

    fun getOnboardingAnswersFlow(): Flow<OnboardingAnswers>
//    fun getUserNameFlow(): Flow<String?>
//    fun getGenderFlow(): Flow<String?>
//    fun getNameFlow(): Flow<String?>
//    fun getIsItABoyFlow(): Flow<String?>
//    fun getBirthDateFlow(): Flow<String?>
//    fun getChildAgeFlow(): Flow<Int?>
//    fun getHobbiesFlow(): Flow<String?>
}
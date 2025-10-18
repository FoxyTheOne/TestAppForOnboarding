package com.foxynest.testappforonboarding.domain.iRepository

import com.foxynest.testappforonboarding.domain.entities.ErrorType
import com.foxynest.testappforonboarding.domain.entities.user.UserPresentation
import com.foxynest.testappforonboarding.other.Resource
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow

interface IAuthRepository {
    suspend fun register(email: String, password: String, user: UserPresentation): Resource<ErrorType>
    suspend fun login(email: String, password: String): Resource<ErrorType>
    suspend fun logout(): Resource<ErrorType>
    suspend fun getCurrentUser(): Resource<UserPresentation?>
    fun getAuthState(): Flow<FirebaseUser?>
    suspend fun updateProfile(user: UserPresentation): Resource<ErrorType>
    // Email
    suspend fun sendEmailVerification(): Resource<ErrorType>
    suspend fun checkEmailVerified(): Boolean
    suspend fun updateDisplayEmail(newEmail: String): Resource<ErrorType>
}
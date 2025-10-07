package com.foxynest.testappforonboarding.domain.iRepository

import com.foxynest.testappforonboarding.domain.entities.UserPresentation
import com.foxynest.testappforonboarding.other.Resource
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow

interface IAuthRepository {
    suspend fun register(email: String, password: String, user: UserPresentation): Resource<Unit>
    suspend fun login(email: String, password: String): Resource<Unit>
    suspend fun logout(): Resource<Unit>
    suspend fun getCurrentUser(): Resource<UserPresentation?>
    fun getAuthState(): Flow<FirebaseUser?>
    suspend fun updateProfile(user: UserPresentation): Resource<Unit>
    suspend fun sendEmailVerification(): Resource<Unit>
    suspend fun checkEmailVerified(): Boolean
}
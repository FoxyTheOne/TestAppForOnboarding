package com.foxynest.testappforonboarding.domain.iRepository

import com.foxynest.testappforonboarding.domain.entities.ErrorType
import com.foxynest.testappforonboarding.domain.entities.user.UserPresentation
import com.foxynest.testappforonboarding.other.Resource
import kotlinx.coroutines.flow.Flow

/** Без типов Firebase: domain не знает, какой бэкенд используется. */
interface IAuthRepository {
    suspend fun register(
        email: String,
        password: String,
        user: UserPresentation
    ): Resource<ErrorType>

    suspend fun login(email: String, password: String): Resource<ErrorType>
    suspend fun logout(): Resource<ErrorType>
    suspend fun deleteAccount(password: String): Resource<ErrorType>
    suspend fun sendPasswordResetEmail(email: String): Resource<ErrorType>

    fun isLoggedIn(): Boolean
    fun observeAuthUser(): Flow<UserPresentation?>
    fun observeCurrentUser(): Flow<Resource<UserPresentation?>>
    suspend fun getCurrentUser(): Resource<UserPresentation?>

    suspend fun updateFirebaseProfile(user: UserPresentation): Resource<ErrorType>

    // Email
    suspend fun sendEmailVerification(): Resource<ErrorType>
    suspend fun checkEmailVerified(): Boolean
    suspend fun updateAuthEmail(newEmail: String, password: String): Resource<ErrorType>
}
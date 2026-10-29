package com.foxynest.testappforonboarding.data.repository

import com.foxynest.testappforonboarding.data.dataSource.firebase.FirebaseAuthDataSource
import com.foxynest.testappforonboarding.domain.entities.ErrorType
import com.foxynest.testappforonboarding.domain.entities.user.UserPresentation
import com.foxynest.testappforonboarding.domain.iRepository.IAuthRepository
import com.foxynest.testappforonboarding.other.Resource
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Не раскрывайте информацию о существовании email - используйте универсальные сообщения об ошибках.
 * Преобразование ошибок Firebase в ErrorType – в одном месте, FirebaseAuthDataSource.
 * UI показывает текст по ErrorType (локализованные строки), а не message.
 */
class AuthRepository @Inject constructor(
    private val firebaseAuthDataSource: FirebaseAuthDataSource
) : IAuthRepository {
    override suspend fun register(
        email: String,
        password: String,
        user: UserPresentation
    ): Resource<ErrorType> = firebaseAuthDataSource.register(email, password, user)

    override suspend fun login(email: String, password: String): Resource<ErrorType> =
        firebaseAuthDataSource.login(email, password)

    override suspend fun logout(): Resource<ErrorType> = firebaseAuthDataSource.logout()

    override suspend fun getCurrentFirebaseUser(): Resource<UserPresentation?> =
        firebaseAuthDataSource.getCurrentUser()

    override fun getAuthState(): Flow<FirebaseUser?> = firebaseAuthDataSource.getAuthState()

    override suspend fun updateFirebaseProfile(user: UserPresentation): Resource<ErrorType> =
        firebaseAuthDataSource.updateProfile(user)

    override suspend fun sendEmailVerification(): Resource<ErrorType> =
        firebaseAuthDataSource.sendEmailVerification()

    override suspend fun checkEmailVerified(): Boolean =
        firebaseAuthDataSource.checkEmailVerified()

    override suspend fun updateAuthEmail(newEmail: String, password: String): Resource<ErrorType> =
        firebaseAuthDataSource.updateAuthEmail(newEmail, password)
}
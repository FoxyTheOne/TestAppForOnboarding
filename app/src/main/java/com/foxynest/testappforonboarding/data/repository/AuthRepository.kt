package com.foxynest.testappforonboarding.data.repository

import com.foxynest.testappforonboarding.data.dataSource.firebase.FirebaseAuthDataSource
import com.foxynest.testappforonboarding.domain.entities.UserPresentation
import com.foxynest.testappforonboarding.domain.iRepository.IAuthRepository
import com.foxynest.testappforonboarding.other.Resource
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val firebaseAuthDataSource: FirebaseAuthDataSource
) : IAuthRepository {
    override suspend fun register(
        email: String,
        password: String,
        user: UserPresentation
    ): Resource<Unit> =
        firebaseAuthDataSource.register(email, password, user)

    override suspend fun login(email: String, password: String): Resource<Unit> =
        firebaseAuthDataSource.login(email, password)

    override suspend fun logout(): Resource<Unit> = firebaseAuthDataSource.logout()

    override suspend fun getCurrentUser(): Resource<UserPresentation?> =
        firebaseAuthDataSource.getCurrentUser()

    override fun getAuthState(): Flow<FirebaseUser?> = firebaseAuthDataSource.getAuthState()

    override suspend fun updateProfile(user: UserPresentation): Resource<Unit> =
        firebaseAuthDataSource.updateProfile(user)

    override suspend fun sendEmailVerification(): Resource<Unit> =
        firebaseAuthDataSource.sendEmailVerification()

    override suspend fun checkEmailVerified(): Boolean = firebaseAuthDataSource.checkEmailVerified()
}
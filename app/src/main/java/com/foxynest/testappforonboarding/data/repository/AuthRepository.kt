package com.foxynest.testappforonboarding.data.repository

import com.foxynest.testappforonboarding.data.dataSource.firebase.FirebaseAuthDataSource
import com.foxynest.testappforonboarding.domain.entities.ErrorType
import com.foxynest.testappforonboarding.domain.entities.user.UserPresentation
import com.foxynest.testappforonboarding.domain.iRepository.IAuthRepository
import com.foxynest.testappforonboarding.other.Resource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Реализация [IAuthRepository]: пока просто передаёт вызовы в [FirebaseAuthDataSource].
 *
 * Зачем тогда отдельный класс: репозиторий – место, где в будущем можно объединять источники данных
 * (например, «сначала локальный кэш, потом сеть»), не трогая domain и экраны. Data source отвечает
 * только за «как поговорить с Firebase».
 *
 * Преобразование ошибок Firebase в ErrorType – в одном месте, FirebaseAuthDataSource.
 * UI показывает текст по ErrorType (локализованные строки), а не message.
 */
class AuthRepository @Inject constructor(
    private val dataSource: FirebaseAuthDataSource
) : IAuthRepository {
    override suspend fun register(email: String, password: String, user: UserPresentation) =
        dataSource.register(email, password, user)

    override suspend fun login(email: String, password: String) = dataSource.login(email, password)

    override suspend fun logout() = dataSource.logout()

    override suspend fun deleteAccount(password: String) = dataSource.deleteAccount(password)

    override suspend fun sendPasswordResetEmail(email: String) =
        dataSource.sendPasswordResetEmail(email)

    override fun isLoggedIn(): Boolean = dataSource.isLoggedIn()

    override fun observeAuthUser(): Flow<UserPresentation?> = dataSource.observeAuthUser()

    override fun observeCurrentUser(): Flow<Resource<UserPresentation?>> =
        dataSource.observeCurrentUser()

    override suspend fun getCurrentUser(): Resource<UserPresentation?> = dataSource.getCurrentUser()

    override suspend fun updateFirebaseProfile(user: UserPresentation): Resource<ErrorType> =
        dataSource.updateProfile(user)

    override suspend fun sendEmailVerification() = dataSource.sendEmailVerification()

    override suspend fun checkEmailVerified(): Boolean = dataSource.checkEmailVerified()

    override suspend fun updateAuthEmail(newEmail: String, password: String) =
        dataSource.updateAuthEmail(newEmail, password)
}
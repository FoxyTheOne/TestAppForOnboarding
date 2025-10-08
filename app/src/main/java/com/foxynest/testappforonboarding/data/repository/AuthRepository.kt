package com.foxynest.testappforonboarding.data.repository

import com.foxynest.testappforonboarding.data.dataSource.firebase.FirebaseAuthDataSource
import com.foxynest.testappforonboarding.domain.entities.UserPresentation
import com.foxynest.testappforonboarding.domain.iRepository.IAuthRepository
import com.foxynest.testappforonboarding.other.Resource
import com.foxynest.testappforonboarding.other.Status
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Не раскрывайте информацию о существовании email - используйте универсальные сообщения об ошибках
 * Валидация на клиенте - только для формата, не для существования данных
 * Обработка ошибок на уровне repository - преобразуйте технические ошибки Firebase в пользовательские сообщения
 * Не делайте лишних запросов - позвольте Firebase обработать проверку существования пользователя
 */
class AuthRepository @Inject constructor(
    private val firebaseAuthDataSource: FirebaseAuthDataSource
) : IAuthRepository {
    override suspend fun register(
        email: String,
        password: String,
        user: UserPresentation
    ): Resource<Unit>
//    = firebaseAuthDataSource.register(email, password, user)
    {
        val result = firebaseAuthDataSource.register(email, password, user)
        return when (result.status) {
            Status.SUCCESS -> Resource.success(Unit)
            Status.ERROR -> {
                // Специфичная обработка ошибок для UI
                val userFriendlyMessage = when {
                    result.message?.contains("email-already-in-use") == true ->
                        "This email is already registered"

                    result.message?.contains("weak-password") == true ->
                        "Password is too weak"

                    result.message?.contains("invalid-email") == true ->
                        "Invalid email format"

                    else -> result.message ?: "Registration failed"
                }
                Resource.error(userFriendlyMessage, null)
            }

            else -> result
        }
    }

    override suspend fun login(email: String, password: String): Resource<Unit>
//    = firebaseAuthDataSource.login(email, password)
    {
        val result = firebaseAuthDataSource.login(email, password)
        return when (result.status) {
            Status.SUCCESS -> Resource.success(Unit)
            Status.ERROR -> {
                // Универсальное сообщение для безопасности
                val userFriendlyMessage = when {
                    result.message?.contains("invalid-credential") == true ->
                        "Invalid email or password"

                    result.message?.contains("user-not-found") == true ->
                        "Invalid email or password"

                    result.message?.contains("wrong-password") == true ->
                        "Invalid email or password"

                    result.message?.contains("too-many-requests") == true ->
                        "Too many attempts. Try again later"

                    else -> result.message ?: "Login failed"
                }
                Resource.error(userFriendlyMessage, null)
            }

            else -> result
        }
    }

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
package com.foxynest.testappforonboarding.data.repository

import com.foxynest.testappforonboarding.data.dataSource.firebase.FirebaseAuthDataSource
import com.foxynest.testappforonboarding.domain.entities.ErrorType
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
    ): Resource<ErrorType>
    {
        val result = firebaseAuthDataSource.register(email, password, user)
        return when (result.status) {
            Status.SUCCESS -> Resource.success(null)
            Status.ERROR -> {
                // Специфичная обработка ошибок для UI
                var userFriendlyMessage = ""
                var errorType: ErrorType? = null
                when {
                    result.message?.contains("email-already-in-use") == true -> {
                        userFriendlyMessage = "This email is already registered"
                        errorType = ErrorType.EMAIL_ALREADY_REGISTERED_ERROR
                    }

                    result.message?.contains("weak-password") == true -> {
                        userFriendlyMessage = "Password is too weak"
                        errorType = ErrorType.PASSWORD_TOO_WEAK_ERROR
                    }

                    result.message?.contains("invalid-email") == true -> {
                        userFriendlyMessage = "Invalid email format"
                        errorType = ErrorType.EMAIL_FORMAT_ERROR
                    }

                    else -> {
                        userFriendlyMessage = result.message ?: "Registration failed"
                        errorType = ErrorType.REGISTER_FAILED_ERROR
                    }
                }

                Resource.error(userFriendlyMessage, errorType)
            }

            else -> result
        }
    }

    override suspend fun login(email: String, password: String): Resource<ErrorType>
    {
        val result = firebaseAuthDataSource.login(email, password)
        return when (result.status) {
            Status.SUCCESS -> Resource.success(null)
            Status.ERROR -> {
                // Универсальное сообщение для безопасности
                var userFriendlyMessage = ""
                var errorType: ErrorType? = null
                when {
                    result.message?.contains("invalid-credential") == true -> {
                        userFriendlyMessage = "Invalid email or password"
                        errorType = ErrorType.AUTH_DATA_ERROR
                    }

                    result.message?.contains("user-not-found") == true -> {
                        userFriendlyMessage = "Invalid email or password"
                        errorType = ErrorType.AUTH_DATA_ERROR
                    }

                    result.message?.contains("wrong-password") == true -> {
                        userFriendlyMessage = "Invalid email or password"
                        errorType = ErrorType.AUTH_DATA_ERROR
                    }

                    result.message?.contains("too-many-requests") == true -> {
                        userFriendlyMessage = "Too many attempts. Try again later"
                        errorType = ErrorType.TOO_MANY_ATTEMPTS_ERROR
                    }

                    else -> {
                        userFriendlyMessage = result.message ?: "Login failed"
                        errorType = ErrorType.LOGIN_FAILED_ERROR
                    }
                }

                Resource.error(userFriendlyMessage, errorType)
            }

            else -> result
        }
    }

    override suspend fun logout(): Resource<ErrorType> = firebaseAuthDataSource.logout()

    override suspend fun getCurrentUser(): Resource<UserPresentation?> =
        firebaseAuthDataSource.getCurrentUser()

    override fun getAuthState(): Flow<FirebaseUser?> = firebaseAuthDataSource.getAuthState()

    override suspend fun updateProfile(user: UserPresentation): Resource<ErrorType> {
        val result = firebaseAuthDataSource.updateProfile(user)
        return when (result.status) {
            Status.SUCCESS -> Resource.success(null)
            Status.ERROR -> {
                var userFriendlyMessage = ""
                var errorType: ErrorType? = null

                when {
                    result.message?.contains("network") == true -> {
                        userFriendlyMessage = "Проблемы с интернет-соединением"
                        errorType = ErrorType.INTERNET_ERROR
                    }

                    else -> {
                        userFriendlyMessage = result.message ?: "Не удалось сохранить данные"
                        errorType = ErrorType.DATA_NOT_SAVED_ERROR
                    }
                }
                Resource.error(userFriendlyMessage, errorType)
            }

            else -> result
        }
    }

    override suspend fun sendEmailVerification(): Resource<ErrorType> =
        firebaseAuthDataSource.sendEmailVerification()

    override suspend fun checkEmailVerified(): Boolean =
        firebaseAuthDataSource.checkEmailVerified()
}
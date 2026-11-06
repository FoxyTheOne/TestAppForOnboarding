package com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login

import com.foxynest.testappforonboarding.domain.entities.ErrorType
import com.foxynest.testappforonboarding.domain.iRepository.IAuthRepository
import com.foxynest.testappforonboarding.other.Resource
import javax.inject.Inject

/**
 * Вход по почте и паролю. Пустые поля отсекаются здесь, без запроса к серверу.
 * Подробная проверка формата – в LoginValidateEmailUC (её вызывает LoginViewModel до этого use case).
 */
class LoginUC @Inject constructor(
    private val authRepository: IAuthRepository
) : ILoginUC {
    override suspend fun execute(email: String, password: String): Resource<ErrorType> {
        if (email.isBlank() || password.isBlank()) {
            return Resource.error("Поле не может быть пустым", ErrorType.FIELD_EMPTY_ERROR)
        }
        return authRepository.login(email, password)
    }
}
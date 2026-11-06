package com.foxynest.testappforonboarding.domain.use_cases.authUseCases.register

import com.foxynest.testappforonboarding.domain.entities.ErrorType
import com.foxynest.testappforonboarding.domain.entities.user.UserPresentation
import com.foxynest.testappforonboarding.domain.iRepository.IAuthRepository
import com.foxynest.testappforonboarding.other.Resource
import javax.inject.Inject

/**
 * Регистрация: создаёт аккаунт и сразу сохраняет в него анкету [user], заполненную до регистрации.
 * Проверки формата почты и пароля делает RegistrationViewModel до вызова (через Register*ValidateUC).
 */
class RegisterUC @Inject constructor(
    private val authRepository: IAuthRepository
) : IRegisterUC {
    override suspend fun execute(
        email: String,
        password: String,
        user: UserPresentation
    ): Resource<ErrorType> {
        if (email.isBlank() || password.isBlank()) {
            return Resource.error("Поле не может быть пустым", ErrorType.FIELD_EMPTY_ERROR)
        }
        return authRepository.register(email, password, user)
    }
}
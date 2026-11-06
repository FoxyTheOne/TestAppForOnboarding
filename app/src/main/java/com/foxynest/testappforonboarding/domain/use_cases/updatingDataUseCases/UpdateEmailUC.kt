package com.foxynest.testappforonboarding.domain.use_cases.updatingDataUseCases

import com.foxynest.testappforonboarding.domain.entities.ErrorType
import com.foxynest.testappforonboarding.domain.iRepository.IAuthRepository
import com.foxynest.testappforonboarding.other.Resource
import javax.inject.Inject

/**
 * Смена почты для входа. Нужен текущий пароль (Firebase требует свежий вход).
 * Письмо уходит на новый адрес; почта сменится, когда человек перейдёт по ссылке, после чего нужно войти заново.
 */
class UpdateEmailUC @Inject constructor(
    private val authRepository: IAuthRepository
) : IUpdateEmailUC {
    override suspend fun updateAuthEmail(newEmail: String, password: String): Resource<ErrorType> {
        return authRepository.updateAuthEmail(newEmail, password)
    }
}
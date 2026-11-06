package com.foxynest.testappforonboarding.domain.use_cases.authUseCases.email

import com.foxynest.testappforonboarding.domain.iRepository.IAuthRepository
import javax.inject.Inject

/** Подтвердил ли пользователь почту (спрашивает сервер – человек мог подтвердить её в браузере только что). */
class CheckEmailVerifiedUC @Inject constructor(
    private val authRepository: IAuthRepository
) : ICheckEmailVerifiedUC {
    override suspend fun execute() = authRepository.checkEmailVerified()
}
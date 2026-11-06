package com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login

import com.foxynest.testappforonboarding.domain.iRepository.IAuthRepository
import javax.inject.Inject

/** Вошёл ли пользователь – мгновенно и без сети (Firebase хранит сессию на устройстве). */
class IsUserLoggedInUC @Inject constructor(
    private val authRepository: IAuthRepository
) : IIsUserLoggedInUC {
    override fun execute(): Boolean = authRepository.isLoggedIn()
}
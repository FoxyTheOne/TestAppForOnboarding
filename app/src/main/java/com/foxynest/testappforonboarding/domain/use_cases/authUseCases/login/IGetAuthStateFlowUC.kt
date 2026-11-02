package com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login

import com.foxynest.testappforonboarding.domain.entities.user.UserPresentation
import kotlinx.coroutines.flow.Flow

interface IGetAuthStateFlowUC {
    /** Только данные авторизации (uid, почта, подтверждение), без запроса профиля. null – не авторизован. */
    fun execute(): Flow<UserPresentation?>
}
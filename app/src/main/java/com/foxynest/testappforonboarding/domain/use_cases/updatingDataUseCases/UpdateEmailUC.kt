package com.foxynest.testappforonboarding.domain.use_cases.updatingDataUseCases

import com.foxynest.testappforonboarding.data.repository.AuthRepository
import com.foxynest.testappforonboarding.domain.entities.ErrorType
import com.foxynest.testappforonboarding.other.Resource
import javax.inject.Inject

class UpdateEmailUC @Inject constructor(
    private val authRepository: AuthRepository
) : IUpdateEmailUC {
    override suspend fun updateDisplayEmail(newEmail: String): Resource<ErrorType> {
        return authRepository.updateDisplayEmail(newEmail)
    }
    override suspend fun updateAuthEmail(newEmail: String, password: String): Resource<ErrorType> {
        return authRepository.updateAuthEmail(newEmail, password)
    }
}
package com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login

import com.foxynest.testappforonboarding.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAuthStateFlowUC @Inject constructor(
    private val authRepository: AuthRepository
) : IGetAuthStateFlowUC {
    override fun execute(): Flow<FirebaseUser?> = authRepository.getAuthState()
}
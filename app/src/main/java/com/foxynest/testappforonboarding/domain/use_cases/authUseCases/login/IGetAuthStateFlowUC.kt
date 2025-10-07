package com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login

import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow

interface IGetAuthStateFlowUC {
    fun execute(): Flow<FirebaseUser?>
}
package com.foxynest.testappforonboarding.domain.use_cases.updatingDataUseCases

import com.foxynest.testappforonboarding.domain.entities.ErrorType
import com.foxynest.testappforonboarding.other.Resource

interface IUpdateEmailUC {
//    suspend fun updateDisplayEmail(newEmail: String): Resource<ErrorType>
    suspend fun updateAuthEmail(newEmail: String, password: String): Resource<ErrorType>
}
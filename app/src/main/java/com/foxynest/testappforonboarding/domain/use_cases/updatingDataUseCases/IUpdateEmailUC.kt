package com.foxynest.testappforonboarding.domain.use_cases.updatingDataUseCases

import com.foxynest.testappforonboarding.domain.entities.ErrorType
import com.foxynest.testappforonboarding.other.Resource

interface IUpdateEmailUC {
//    suspend fun execute(newEmail: String, password: String): Resource<ErrorType>
    suspend fun updateDisplayEmail(newEmail: String): Resource<ErrorType>
}
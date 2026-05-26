package com.foxynest.testappforonboarding.domain.use_cases.migrate

import com.foxynest.testappforonboarding.domain.iRepository.IMigrateUserDataRepository
import javax.inject.Inject

class MigrateUserDataUC @Inject constructor(
    private val migrateUserDataRepository: IMigrateUserDataRepository
) : IMigrateUserDataUC {
    override suspend fun migrateIfNeeded() = migrateUserDataRepository.migrateIfNeeded()
}
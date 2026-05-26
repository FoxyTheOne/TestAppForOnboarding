package com.foxynest.testappforonboarding.domain.use_cases.migrate

interface IMigrateUserDataUC {
    suspend fun migrateIfNeeded()
}
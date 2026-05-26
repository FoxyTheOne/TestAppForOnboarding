package com.foxynest.testappforonboarding.domain.iRepository

interface IMigrateUserDataRepository {
    suspend fun migrateIfNeeded()
}
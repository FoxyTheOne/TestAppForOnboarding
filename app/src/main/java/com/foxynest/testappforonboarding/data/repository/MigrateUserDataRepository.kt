package com.foxynest.testappforonboarding.data.repository

import android.util.Log
import com.foxynest.testappforonboarding.data.dataSource.datastorePreferences.IAppDatastorePreferences
import com.foxynest.testappforonboarding.data.dataSource.firebase.FirebaseAuthDataSource
import com.foxynest.testappforonboarding.domain.entities.DataMigrationHelper
import com.foxynest.testappforonboarding.domain.iRepository.IMigrateUserDataRepository
import jakarta.inject.Inject
import kotlinx.coroutines.flow.first

class MigrateUserDataRepository @Inject constructor(
    private val localDataStore: IAppDatastorePreferences,
    private val firebaseDataSource: FirebaseAuthDataSource
) : IMigrateUserDataRepository {
    companion object {
        private const val CURRENT_SCHEMA_VERSION = 1
    }

//    override suspend fun migrateIfNeeded() {
//        migrateLocalData()
//        migrateFirebaseDataIfNeeded()
//    }

    override suspend fun migrateIfNeeded() {
        val currentVersion = localDataStore.getSchemaVersion()
        if (currentVersion >= CURRENT_SCHEMA_VERSION) {
            // Миграция уже выполнена
            return
        }

//        when (currentVersion) {
//            0 -> {
//                // Миграция с версии 0 на 1
//                migrateLocalData()
//                migrateFirebaseDataIfNeeded()
//                localDataStore.setSchemaVersion(1)
//            }
//            // если понадобится версия 2:
//            // 1 -> {
//            //     migrateToVersion2()
//            //     localDataStore.setSchemaVersion(2)
//            // }
//        }

        // Выполняем миграцию
        migrateLocalData()
        migrateFirebaseDataIfNeeded()

        // Обновляем версию
        localDataStore.setSchemaVersion(CURRENT_SCHEMA_VERSION)
    }

    private suspend fun migrateLocalData() {
        val currentAnswers = localDataStore.getOnboardingAnswersFlow().first()
        Log.d(
            "Migration",
            "Before migration: gender=${currentAnswers.childGender}, hobbies=${currentAnswers.childHobbies}"
        )
        val newGender = DataMigrationHelper.migrateGender(currentAnswers.childGender)
        val newHobbies = DataMigrationHelper.migrateHobbies(currentAnswers.childHobbies)

        if (newGender != currentAnswers.childGender || newHobbies != currentAnswers.childHobbies) {
            val migratedAnswers = currentAnswers.copy(
                childGender = newGender,
                childHobbies = newHobbies
            )
            localDataStore.saveOnboardingAnswers(migratedAnswers)
            Log.d("Migration", "After migration: gender=$newGender, hobbies=$newHobbies")
        } else {
            Log.d("Migration", "No migration needed for local data")
        }
    }

    private suspend fun migrateFirebaseDataIfNeeded() {
        val userResult = firebaseDataSource.getCurrentUser()
        val user = userResult.data ?: return

        val newGender = DataMigrationHelper.migrateGender(user.childGender)
        val newHobbies = DataMigrationHelper.migrateHobbies(user.childHobbies)

        val updatedUser = user.copy(
            childGender = newGender,
            childHobbies = newHobbies
        )
        if (updatedUser != user) {
            firebaseDataSource.updateProfile(updatedUser)
        }
    }
}
package com.foxynest.testappforonboarding.data.repository

import android.util.Log
import com.foxynest.testappforonboarding.data.dataSource.datastorePreferences.IAppDatastorePreferences
import com.foxynest.testappforonboarding.data.dataSource.firebase.FirebaseAuthDataSource
import com.foxynest.testappforonboarding.domain.entities.DataMigrationHelper
import com.foxynest.testappforonboarding.domain.entities.onboarding.YesNoAnswer
import com.foxynest.testappforonboarding.domain.iRepository.IMigrateUserDataRepository
import com.foxynest.testappforonboarding.other.Status
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Миграция – перевод уже сохранённых данных в новый формат, когда формат хранения меняется.
 * Пример: раньше пол хранился как «Мальчик» (текст на русском), теперь как ключ `BOY`.
 * Без миграции у тех, кто установил приложение раньше, профиль показывался бы неправильно.
 *
 * Как устроено: в DataStore лежит номер версии схемы данных. При запуске (SplashViewModel) сравниваем его
 * с [CURRENT_SCHEMA_VERSION]; если меньше – переводим данные и в DataStore, и в Firestore, потом записываем новый номер.
 *
 * Если формат данных снова изменится: увеличить [CURRENT_SCHEMA_VERSION] и добавить шаг перевода.
 * Шаги должны быть идемпотентными: повторный перевод уже переведённых данных ничего не меняет.
 */
class MigrateUserDataRepository @Inject constructor(
    private val localDataStore: IAppDatastorePreferences,
    private val firebaseDataSource: FirebaseAuthDataSource
) : IMigrateUserDataRepository {
    companion object {
        private const val TAG = "Migration"

        // 1 – пол и хобби хранятся ключами; 2 – ответ «да/нет» хранится ключом
        private const val CURRENT_SCHEMA_VERSION = 2
    }

    override suspend fun migrateIfNeeded() {
        if (localDataStore.getSchemaVersion() >= CURRENT_SCHEMA_VERSION) return

        // Все шаги миграции идемпотентны, поэтому их безопасно повторять при каждом запуске до успеха
        migrateLocalData()
        val isFirebaseMigrated = migrateFirebaseDataIfNeeded()

        // Раньше версия проставлялась всегда: без сети данные в Firebase так и оставались не мигрированными
        if (isFirebaseMigrated) {
            localDataStore.setSchemaVersion(CURRENT_SCHEMA_VERSION)
        }
    }

    private suspend fun migrateLocalData() {
        val currentAnswers = localDataStore.getOnboardingAnswersFlow().first()
        val migratedAnswers = currentAnswers.copy(
            childGender = DataMigrationHelper.migrateGender(currentAnswers.childGender),
            childHobbies = DataMigrationHelper.migrateHobbies(currentAnswers.childHobbies),
            isItABoy = YesNoAnswer.migrate(currentAnswers.isItABoy)
        )
        if (migratedAnswers != currentAnswers) {
            localDataStore.saveOnboardingAnswers(migratedAnswers)
            Log.d(TAG, "Local data migrated")
        }
    }

    /** @return true – мигрировать нечего или миграция прошла успешно */
    private suspend fun migrateFirebaseDataIfNeeded(): Boolean {
        val userResult = firebaseDataSource.getCurrentUser()
        if (userResult.status != Status.SUCCESS) return false
        // Не залогинен: мигрировать в Firebase нечего
        val user = userResult.data ?: return true

        val updatedUser = user.copy(
            childGender = DataMigrationHelper.migrateGender(user.childGender),
            childHobbies = DataMigrationHelper.migrateHobbies(user.childHobbies),
            isItABoy = YesNoAnswer.migrate(user.isItABoy)
        )
        if (updatedUser == user) return true

        return firebaseDataSource.updateProfile(updatedUser).status == Status.SUCCESS
    }
}
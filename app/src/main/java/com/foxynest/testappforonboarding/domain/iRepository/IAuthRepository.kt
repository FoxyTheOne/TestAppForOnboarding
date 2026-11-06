package com.foxynest.testappforonboarding.domain.iRepository

import com.foxynest.testappforonboarding.domain.entities.ErrorType
import com.foxynest.testappforonboarding.domain.entities.user.UserPresentation
import com.foxynest.testappforonboarding.other.Resource
import kotlinx.coroutines.flow.Flow

/**
 * Аккаунт и профиль пользователя на сервере.
 * Без типов Firebase (никаких FirebaseUser, Task, DocumentSnapshot): domain не знает, какой бэкенд используется.
 *
 * Результат операций – `Resource<ErrorType>`: статус SUCCESS / ERROR, а в `data` при ошибке лежит её тип.
 */
interface IAuthRepository {
    suspend fun register(
        email: String,
        password: String,
        user: UserPresentation
    ): Resource<ErrorType>

    suspend fun login(email: String, password: String): Resource<ErrorType>

    /** Не выходит, если есть неотправленные без сети изменения – вернёт UNSYNCED_DATA_LOGOUT_ERROR. */
    suspend fun logout(): Resource<ErrorType>

    /** Нужен пароль: перед удалением Firebase требует повторный вход. */
    suspend fun deleteAccount(password: String): Resource<ErrorType>
    suspend fun sendPasswordResetEmail(email: String): Resource<ErrorType>

    /** Мгновенно и без сети – по сохранённой на устройстве сессии. */
    fun isLoggedIn(): Boolean

    /** Только вход/выход (uid, почта, подтверждение), без чтения базы. null – не вошёл. */
    fun observeAuthUser(): Flow<UserPresentation?>

    /** Профиль целиком (Auth + анкета из базы), обновляется сам при любом изменении документа. */
    fun observeCurrentUser(): Flow<Resource<UserPresentation?>>

    /** Разовое чтение профиля (для миграции данных). */
    suspend fun getCurrentUser(): Resource<UserPresentation?>

    suspend fun updateFirebaseProfile(user: UserPresentation): Resource<ErrorType>

    // Email
    suspend fun sendEmailVerification(): Resource<ErrorType>
    suspend fun checkEmailVerified(): Boolean

    /** Отправляет письмо на НОВЫЙ адрес; почта для входа сменится после перехода по ссылке. */
    suspend fun updateAuthEmail(newEmail: String, password: String): Resource<ErrorType>
}
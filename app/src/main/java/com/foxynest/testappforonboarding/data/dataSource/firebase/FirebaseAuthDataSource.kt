package com.foxynest.testappforonboarding.data.dataSource.firebase

import android.util.Log
import com.foxynest.testappforonboarding.domain.entities.ErrorType
import com.foxynest.testappforonboarding.domain.entities.user.UserPresentation
import com.foxynest.testappforonboarding.other.Resource
import com.foxynest.testappforonboarding.other.constants.CommonConstants.USERS_COLLECTION
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/**
 * Lowercase the email but not the password!!
 *
 * Все ошибки Firebase превращаются в ErrorType здесь, по типу исключения.
 */
class FirebaseAuthDataSource @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val TAG = "FirebaseAuthDataSource"

        /**
         * Запись в Firestore без сети не падает, а ждёт подтверждения сервера сколько угодно.
         * При этом запись уже сохранена в локальном кэше Firestore и отправится сама, когда появится сеть
         * (даже после перезапуска приложения). Поэтому ждём ограниченное время и считаем запись принятой.
         */
        private const val FIRESTORE_WRITE_TIMEOUT_MILLIS = 10_000L
        private const val PENDING_WRITES_TIMEOUT_MILLIS = 5_000L
    }

    private fun userDocument(uid: String): DocumentReference =
        firestore.collection(USERS_COLLECTION).document(uid)

    private fun FirebaseUser.toAuthOnlyUser() = UserPresentation(
        uid = uid,
        authEmail = email.orEmpty(),
        isEmailVerified = isEmailVerified,
        userName = displayName.orEmpty()
    )

    // REGISTRATION / LOGIN / LOGOUT

    suspend fun register(
        authEmail: String,
        password: String,
        user: UserPresentation
    ): Resource<ErrorType> {
        val firebaseUser = try {
            auth.createUserWithEmailAndPassword(authEmail.lowercase().trim(), password).await().user
        } catch (e: Exception) {
            return e.toErrorResource("Registration failed", ErrorType.REGISTER_FAILED_ERROR)
        } ?: return Resource.error("Registration failed", ErrorType.REGISTER_FAILED_ERROR)

        // Аккаунт уже создан и пользователь вошёл. Ошибка записи профиля НЕ должна превращаться в «регистрация
        // не удалась»: повторная попытка дала бы «email уже занят». Незаписанный профиль досылается из
        // MainViewModel при следующей загрузке профиля.
        try {
            userDocument(firebaseUser.uid)
                .set(UserFirestoreMapper.toFirestoreMapForCreate(user))
                .awaitWrite()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Account created, but profile was not saved to Firestore", e)
        }

        // Мягкая верификация: письмо уходит сразу, но пользоваться приложением можно и без подтверждения
        try {
            firebaseUser.sendEmailVerification().await()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.w(TAG, "Verification email was not sent after registration", e)
        }
        return Resource.success(null)
    }

    suspend fun login(email: String, password: String): Resource<ErrorType> {
        return try {
            auth.signInWithEmailAndPassword(email.lowercase().trim(), password).await()
            Resource.success(null)
        } catch (e: Exception) {
            e.toErrorResource("Login failed", ErrorType.LOGIN_FAILED_ERROR)
        }
    }

    suspend fun logout(): Resource<ErrorType> {
        return try {
            // Неотправленные изменения после выхода будут отклонены правилами безопасности (auth == null) и потеряны
            if (!awaitPendingWrites()) {
                return Resource.error(
                    "Pending writes are not synced",
                    ErrorType.UNSYNCED_DATA_LOGOUT_ERROR
                )
            }
            auth.signOut()
            Resource.success(null)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Resource.error(e.message ?: "Logout failed", ErrorType.LOGOUT_FAILED_ERROR)
        }
    }

    /**
     * Удаление аккаунта: повторный вход (Firebase требует «свежий» вход) → документ профиля → пользователь Auth.
     * Документ удаляется первым: после удаления пользователя правила безопасности уже не пустят к документу.
     */
    suspend fun deleteAccount(password: String): Resource<ErrorType> {
        val user = auth.currentUser
            ?: return Resource.error("User not authenticated", ErrorType.NOT_AUTHENTICATED_ERROR)
        val email = user.email
            ?: return Resource.error("User has no email", ErrorType.NOT_AUTHENTICATED_ERROR)
        return try {
            user.reauthenticate(EmailAuthProvider.getCredential(email, password)).await()
            userDocument(user.uid).delete().await()
            user.delete().await()
            Resource.success(null)
        } catch (e: Exception) {
            Log.e(TAG, "Account deletion failed", e)
            e.toErrorResource(
                "Account deletion failed",
                ErrorType.ACCOUNT_DELETION_FAILED_ERROR,
                invalidCredentials = ErrorType.WRONG_PASSWORD_ERROR
            )
        }
    }

    /**
     * При включённой защите от перебора email (по умолчанию в новых проектах) Firebase не сообщает,
     * существует ли аккаунт – письмо просто не придёт. Поэтому UI показывает нейтральное сообщение.
     */
    suspend fun sendPasswordResetEmail(email: String): Resource<ErrorType> {
        return try {
            auth.sendPasswordResetEmail(email.lowercase().trim()).await()
            Resource.success(null)
        } catch (e: Exception) {
            e.toErrorResource("Password reset failed", ErrorType.PASSWORD_RESET_FAILED_ERROR)
        }
    }

    // CURRENT USER

    fun isLoggedIn(): Boolean = auth.currentUser != null

    // Реактивное отслеживание входа/выхода (в том числе принудительного – например, после смены почты)
    fun observeAuthUser(): Flow<UserPresentation?> = authStateFlow().map { it?.toAuthOnlyUser() }

    /**
     * Профиль через snapshot-листенер вместо разовых get() на каждом экране:
     * одно чтение при подписке и по одному на каждое реальное изменение (в том числе с другого устройства).
     *
     * SUCCESS + null – не авторизован.
     * LOADING + данные Auth – документа нет в локальном кэше, ответ сервера ещё не пришёл.
     * ERROR + данные Auth – документ прочитать не удалось.
     * Данные Auth без полей анкеты НЕЛЬЗЯ записывать обратно в Firestore.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeCurrentUser(): Flow<Resource<UserPresentation?>> = authStateFlow()
        // Слушатель Auth срабатывает и при обновлении токена (раз в час) – не переподписываемся без смены пользователя
        .distinctUntilChangedBy { it?.uid }
        .flatMapLatest { firebaseUser ->
            if (firebaseUser == null) flowOf(Resource.success(null)) else profileFlow(firebaseUser)
        }

    private fun profileFlow(firebaseUser: FirebaseUser): Flow<Resource<UserPresentation?>> =
        callbackFlow {
            val registration =
                userDocument(firebaseUser.uid).addSnapshotListener { snapshot, error ->
                    val authUser = firebaseUser.toAuthOnlyUser()
                    when {
                        error != null || snapshot == null ->
                            trySend(
                                Resource.error(
                                    error?.message ?: "Profile loading failed",
                                    authUser
                                )
                            )

                        // Документа нет только в кэше (новое устройство без сети) – это ещё не значит, что его нет на сервере
                        !snapshot.exists() && snapshot.metadata.isFromCache ->
                            trySend(Resource.loading(authUser))

                        else -> trySend(
                            Resource.success(
                                UserFirestoreMapper.fromDocument(
                                    snapshot,
                                    uid = firebaseUser.uid,
                                    // Берём актуальные значения: почта и статус подтверждения могли измениться
                                    authEmail = auth.currentUser?.email ?: authUser.authEmail,
                                    isEmailVerified = auth.currentUser?.isEmailVerified
                                        ?: authUser.isEmailVerified
                                )
                            )
                        )
                    }
                }
            awaitClose { registration.remove() }
        }

    /** Разовое чтение (миграция данных). */
    suspend fun getCurrentUser(): Resource<UserPresentation?> {
        val firebaseUser = auth.currentUser ?: return Resource.success(null)
        return try {
            val document = userDocument(firebaseUser.uid).get().await()
            Resource.success(
                UserFirestoreMapper.fromDocument(
                    document,
                    firebaseUser.uid,
                    firebaseUser.email.orEmpty(),
                    firebaseUser.isEmailVerified
                )
            )
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Resource.error(e.message ?: "Current user failure", firebaseUser.toAuthOnlyUser())
        }
    }

    private fun authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(authStateListener)
        awaitClose { auth.removeAuthStateListener(authStateListener) }
    }

    // PROFILE

    suspend fun updateProfile(user: UserPresentation): Resource<ErrorType> {
        val currentUser = auth.currentUser
            ?: return Resource.error("User not authenticated", ErrorType.NOT_AUTHENTICATED_ERROR)
        return try {
            // Сначала основное – Firestore. set + merge обновляет только переданные поля и создаёт документ, если его нет
            userDocument(currentUser.uid)
                .set(UserFirestoreMapper.toFirestoreMapForUpdate(user), SetOptions.merge())
                .awaitWrite()

            // displayName в Auth – вторично, его ошибка не должна отменять сохранение профиля
            if (currentUser.displayName != user.userName) {
                try {
                    currentUser.updateProfile(
                        UserProfileChangeRequest.Builder().setDisplayName(user.userName).build()
                    ).await()
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Log.w(TAG, "Failed to update displayName", e)
                }
            }
            Resource.success(null)
        } catch (e: Exception) {
            e.toErrorResource("Profile update failed", ErrorType.DATA_NOT_SAVED_ERROR)
        }
    }

    // EMAIL

    suspend fun sendEmailVerification(): Resource<ErrorType> {
        val currentUser = auth.currentUser
            ?: return Resource.error("User not authenticated", ErrorType.NOT_AUTHENTICATED_ERROR)
        return try {
            currentUser.sendEmailVerification().await()
            Resource.success(null)
        } catch (e: Exception) {
            e.toErrorResource(
                "Failed to send verification email",
                ErrorType.SENDING_EMAIL_VERIFICATION_ERROR
            )
        }
    }

    suspend fun checkEmailVerified(): Boolean {
        val currentUser = auth.currentUser ?: return false
        return try {
            currentUser.reload().await()
            auth.currentUser?.isEmailVerified ?: false
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Error checking email verification", e)
            false
        }
    }

    /**
     * verifyBeforeUpdateEmail НЕ требует подтверждённой текущей почты – нужен только недавний вход (reauthenticate).
     * Письмо уходит на НОВЫЙ адрес, почта для входа меняется после перехода по ссылке.
     * После смены почты Firebase отзывает сессии, и пользователь должен войти заново уже с новой почтой.
     */
    suspend fun updateAuthEmail(newEmail: String, password: String): Resource<ErrorType> {
        val user = auth.currentUser
            ?: return Resource.error("User not authenticated", ErrorType.NOT_AUTHENTICATED_ERROR)
        val currentEmail = user.email
            ?: return Resource.error("User has no email", ErrorType.NOT_AUTHENTICATED_ERROR)

        return try {
            user.reauthenticate(EmailAuthProvider.getCredential(currentEmail, password)).await()
            user.verifyBeforeUpdateEmail(newEmail).await()
            Resource.success(null)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating auth email", e)
            e.toErrorResource(
                "Failed to update auth email",
                ErrorType.DATA_NOT_SAVED_ERROR,
                invalidCredentials = ErrorType.WRONG_PASSWORD_WHILE_CHANGING_EMAIL_ERROR
            )
        }
    }

    // HELPERS

    /** Task<Void>.await() возвращает null, поэтому явно возвращаем true: null = таймаут */
    private suspend fun awaitPendingWrites(): Boolean =
        withTimeoutOrNull(PENDING_WRITES_TIMEOUT_MILLIS) {
            firestore.waitForPendingWrites().await()
            true
        } ?: false

    private suspend fun Task<Void>.awaitWrite() {
        val isAcknowledged = withTimeoutOrNull(FIRESTORE_WRITE_TIMEOUT_MILLIS) {
            await()
            true
        } ?: false
        if (!isAcknowledged) {
            Log.w(TAG, "Firestore write is queued offline and will be synced later")
        }
    }

    private fun Exception.toErrorResource(
        fallbackMessage: String,
        fallback: ErrorType,
        invalidCredentials: ErrorType = ErrorType.AUTH_DATA_ERROR
    ): Resource<ErrorType> {
        if (this is CancellationException) throw this
        val errorType = when (this) {
            is FirebaseNetworkException -> ErrorType.INTERNET_ERROR
            is FirebaseTooManyRequestsException -> ErrorType.TOO_MANY_ATTEMPTS_ERROR
            // WeakPassword – подкласс InvalidCredentials, поэтому проверяется раньше
            is FirebaseAuthWeakPasswordException -> ErrorType.PASSWORD_TOO_WEAK_ERROR
            is FirebaseAuthUserCollisionException -> ErrorType.EMAIL_ALREADY_REGISTERED_ERROR
            is FirebaseAuthInvalidCredentialsException ->
                if (errorCode == "ERROR_INVALID_EMAIL") ErrorType.EMAIL_FORMAT_ERROR else invalidCredentials

            is FirebaseAuthRecentLoginRequiredException -> ErrorType.LOGIN_FAILED_ERROR
            is FirebaseAuthInvalidUserException -> ErrorType.AUTH_DATA_ERROR
            else -> fallback
        }
        return Resource.error(message ?: fallbackMessage, errorType)
    }
}
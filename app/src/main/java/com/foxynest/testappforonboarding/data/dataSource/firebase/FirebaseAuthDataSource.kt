package com.foxynest.testappforonboarding.data.dataSource.firebase

import android.util.Log
import com.foxynest.testappforonboarding.domain.entities.ErrorType
import com.foxynest.testappforonboarding.domain.entities.user.UserPresentation
import com.foxynest.testappforonboarding.other.Resource
import com.foxynest.testappforonboarding.other.constants.CommonConstants.USERS_COLLECTION
import com.foxynest.testappforonboarding.other.extensions.toFirestoreMapWithCreationTimestamp
import com.foxynest.testappforonboarding.other.extensions.toFirestoreMapWithUpdatedAtTimestamp
import com.foxynest.testappforonboarding.other.extensions.toUserPresentation
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
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/**
 * Lowercase the email but not the password!!
 *
 * Все ошибки Firebase превращаются в ErrorType здесь, по типу исключения.
 * Раньше репозиторий дополнительно искал подстроки в тексте ошибки ("email-already-in-use"),
 * которых в тексте нет, и затирал правильный тип ошибки общим REGISTER_FAILED / LOGIN_FAILED.
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
        // MainViewModel.repairFirebaseProfileFromLocalData при следующей загрузке профиля.
        try {
            firestore.collection(USERS_COLLECTION)
                .document(firebaseUser.uid)
                .set(user.toFirestoreMapWithCreationTimestamp())
                .awaitWrite()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Account created, but profile was not saved to Firestore", e)
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
            // Task<Void>.await() возвращает null, поэтому явно возвращаем true: null = таймаут
            val isSynced = withTimeoutOrNull(PENDING_WRITES_TIMEOUT_MILLIS) {
                firestore.waitForPendingWrites().await()
                true
            } ?: false
            if (!isSynced) {
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
     * SUCCESS + null – пользователь не авторизован.
     * ERROR + базовые данные – авторизован, но профиль из Firestore прочитать не удалось.
     * Такие данные НЕЛЬЗЯ записывать обратно в Firestore: поля анкеты в них пустые.
     */
    suspend fun getCurrentUser(): Resource<UserPresentation?> {
        val firebaseUser = auth.currentUser ?: return Resource.success(null)
        return try {
            val document = firestore
                .collection(USERS_COLLECTION)
                .document(firebaseUser.uid)
                .get()
                .await()
            Resource.success(document.toUserPresentation(firebaseUser))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Resource.error(
                e.message ?: "Current user failure", UserPresentation(
                    uid = firebaseUser.uid,
                    authEmail = firebaseUser.email ?: "",
                    isEmailVerified = firebaseUser.isEmailVerified,
                    userName = firebaseUser.displayName ?: ""
                )
            )
        }
    }

    // Используется для реактивного отслеживания изменений. Возвращает Flow, который эмитит события при изменении состояния аутентификации (вход, выход, обновление токена)
    fun getAuthState(): Flow<FirebaseUser?> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        auth.addAuthStateListener(authStateListener)
        awaitClose {
            auth.removeAuthStateListener(authStateListener)
        }
    }

    suspend fun updateProfile(user: UserPresentation): Resource<ErrorType> {
        val currentUser = auth.currentUser
            ?: return Resource.error("User not authenticated", ErrorType.NOT_AUTHENTICATED_ERROR)
        return try {
            // Сначала основное – Firestore. set + merge обновляет только переданные поля и создаёт документ, если его нет
            firestore.collection(USERS_COLLECTION)
                .document(currentUser.uid)
                .set(
                    user.toFirestoreMapWithUpdatedAtTimestamp(FieldValue.serverTimestamp()),
                    SetOptions.merge()
                )
                .awaitWrite()

            // displayName в Auth – вторично. Раньше этот сетевой вызов шёл первым, и без сети
            // падало всё сохранение, хотя Firestore смог бы поставить запись в очередь
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
            if (e is CancellationException) throw e
            e.toErrorResource("Profile update failed", ErrorType.DATA_NOT_SAVED_ERROR)
        }
    }

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
     * Раньше здесь была собственная проверка isEmailVerified, из-за которой пользователь с опечаткой в почте
     * не мог её исправить никогда.
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
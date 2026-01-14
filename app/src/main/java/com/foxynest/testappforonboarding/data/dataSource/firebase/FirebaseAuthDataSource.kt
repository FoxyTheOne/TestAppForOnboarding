package com.foxynest.testappforonboarding.data.dataSource.firebase

import android.util.Log
import com.foxynest.testappforonboarding.domain.entities.ErrorType
import com.foxynest.testappforonboarding.domain.entities.user.UserFirestoreFields
import com.foxynest.testappforonboarding.domain.entities.user.UserPresentation
import com.foxynest.testappforonboarding.other.Resource
import com.foxynest.testappforonboarding.other.constants.CommonConstants.USERS_COLLECTION
import com.foxynest.testappforonboarding.other.extensions.toFirestoreMapWithCreationTimestamp
import com.foxynest.testappforonboarding.other.extensions.toFirestoreMapWithUpdatedAtTimestamp
import com.foxynest.testappforonboarding.other.extensions.toUserPresentation
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Lowercase the email but not the password!!
 * val emailLowerCase = email.lowercase().trim() - наверное нужно в Use Case, но для страховки оставила здесь. Всё в одном месте и перед глазами, чтобы быть уверенным, что всё сделано
 */
class FirebaseAuthDataSource @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val TAG = "FirebaseAuthDataSource"
    }

    // await() - это часть библиотеки kotlinx-coroutines-play-services, которая предоставляет suspend-функции для Firebase операций
    // Код с await() более читаемый и легче поддерживается. Позволяет избежать "callback hell" (вложенных колбэков). Лучше интегрируется с архитектурой приложения на корутинах
    suspend fun register(
        authEmail: String,
        password: String,
        user: UserPresentation
    ): Resource<ErrorType> {
        return try {
            val authEmailLowerCase = authEmail.lowercase().trim()
            val result = auth.createUserWithEmailAndPassword(authEmailLowerCase, password).await()

            // Создаем документ пользователя в Firestore; uid создаётся автоматически, верификацтю почты тоже настраивать будем позже
            result.user?.let { nonNullFirebaseUser ->
//                val userEmailLowerCase = nonNullFirebaseUser.email?.lowercase()?.trim() ?: ""

                val userData = user.toFirestoreMapWithCreationTimestamp()
                    .toMutableMap()
//                    .apply {
//                        put(UserFirestoreFields.EMAIL, userEmailLowerCase)
//                    }

                firestore
                    .collection(USERS_COLLECTION)
                    .document(nonNullFirebaseUser.uid)
                    .set(userData)
                    .await()
            }

            Resource.success(null)
        } catch (e: Exception) {
            when (e) {
                is FirebaseAuthWeakPasswordException -> Resource.error(
                    "Password is too weak",
                    ErrorType.PASSWORD_TOO_WEAK_ERROR
                )

                is FirebaseAuthInvalidCredentialsException -> Resource.error(
                    "Invalid email format",
                    ErrorType.EMAIL_FORMAT_ERROR
                )

                is FirebaseAuthUserCollisionException -> Resource.error(
                    "User already exists",
                    ErrorType.EMAIL_ALREADY_REGISTERED_ERROR
                )

                else -> Resource.error(
                    e.message ?: "Registration failed",
                    ErrorType.REGISTER_FAILED_ERROR
                )
            }
        }
    }

    suspend fun login(email: String, password: String): Resource<ErrorType> {
        return try {
            val emailLowerCase = email.lowercase().trim()
            auth.signInWithEmailAndPassword(emailLowerCase, password).await()
            Resource.success(null)
        } catch (e: Exception) {
            when (e) {
                is FirebaseAuthInvalidCredentialsException -> Resource.error(
                    "Invalid email or password",
                    ErrorType.AUTH_DATA_ERROR
                )

                is FirebaseAuthInvalidUserException -> Resource.error(
                    "User not found",
                    ErrorType.AUTH_DATA_ERROR
                )

                else -> Resource.error(e.message ?: "Login failed", ErrorType.LOGIN_FAILED_ERROR)
            }
        }
    }

    suspend fun logout(): Resource<ErrorType> {
        return try {
            auth.signOut()
            Resource.success(null)
        } catch (e: Exception) {
            Resource.error(e.message ?: "Logout failed", ErrorType.LOGOUT_FAILED_ERROR)
        }
    }

    // Используется для разовых проверок состояния аутентификации. Синхронный метод, возвращает текущего пользователя сразу
    suspend fun getCurrentUser(): Resource<UserPresentation?> {
        try {
            val firebaseUser = auth.currentUser

            return if (firebaseUser != null) {
                try {
                    // Получаем дополнительные данные из Firestore
                    val document = firestore
                        .collection(USERS_COLLECTION)
                        .document(firebaseUser.uid)
                        .get()
                        .await()

                    Resource.success(document.toUserPresentation(firebaseUser))
                } catch (e: Exception) {
                    // Если не удалось получить данные из Firestore, возвращаем базовые данные
                    Resource.error(
                        e.message ?: "Current user failure", UserPresentation(
                            uid = firebaseUser.uid,
                            authEmail = firebaseUser.email ?: "",
                            isEmailVerified = firebaseUser.isEmailVerified,
                            userName = firebaseUser.displayName ?: ""
                        )
                    )
                }
            } else {
                Resource.success(null)
            }
        } catch (e: Exception) {
            // Если не удалось получить firebaseUser
            return Resource.error(e.message ?: "Current user failure", null)
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
        return try {
            // Проверяем, что пользователь авторизован
            val currentUser =
                auth.currentUser ?: return Resource.error(
                    "User not authenticated",
                    ErrorType.NOT_AUTHENTICATED_ERROR
                )

            // Обновляем базовые данные в FirebaseAuth
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(user.userName)
                .build()
            currentUser.updateProfile(profileUpdates).await()

            // Обновляем дополнительные данные в Firestore
            val userData = user.toFirestoreMapWithUpdatedAtTimestamp(FieldValue.serverTimestamp())

//            update(): Обновляет только указанные поля в существующем документе. Если документа не существует, выбросит исключение. Используется, когда вы уверены, что документ уже создан
//            set() с SetOptions.merge(): Создает документ, если он не существует. Если документ существует, обновляет только указанные поля. Более безопасный подход, так как не выбросит исключение если документ не найден
//            auth.currentUser?.uid?.let { uid ->
//                firestore.collection("users")
//                    .document(uid)
//                    .update(userData as Map<String, Any>)
//                    .await()
//            }

            firestore.collection(USERS_COLLECTION)
                .document(currentUser.uid)
                .set(userData, SetOptions.merge())
                .await()

            Resource.success(null)
        } catch (e: Exception) {
            Resource.error(e.message ?: "Profile update failed", ErrorType.DATA_NOT_SAVED_ERROR)
        }
    }

    suspend fun sendEmailVerification(): Resource<ErrorType> {
        return try {
            auth.currentUser?.sendEmailVerification()?.await()
            Resource.success(null)
        } catch (e: Exception) {
            Resource.error(
                e.message ?: "Failed to send verification email",
                ErrorType.SENDING_EMAIL_VERIFICATION_ERROR
            )
        }
    }

    suspend fun checkEmailVerified(): Boolean {
        return try {
            auth.currentUser?.reload()?.await()
            auth.currentUser?.isEmailVerified ?: false
        } catch (e: Exception) {
            false
        }
    }

//    suspend fun updateDisplayEmail(newEmail: String): Resource<ErrorType> {
//        return try {
//            val currentUser = auth.currentUser
//                ?: return Resource.error("User not authenticated", ErrorType.NOT_AUTHENTICATED_ERROR)
//
//            // Обновляем только display email в Firestore
//            val userData = mapOf(
//                UserFirestoreFields.DISPLAY_EMAIL to newEmail,
//                UserFirestoreFields.UPDATED_AT to FieldValue.serverTimestamp()
//            )
//
//            firestore.collection(USERS_COLLECTION)
//                .document(currentUser.uid)
//                .update(userData)
//                .await()
//
//            Resource.success(null)
//        } catch (e: Exception) {
//            Resource.error(
//                e.message ?: "Failed to update display email",
//                ErrorType.DATA_NOT_SAVED_ERROR
//            )
//        }
//    }

//    suspend fun updateAuthEmail(newEmail: String, password: String): Resource<ErrorType> {
//        return try {
//            val user = auth.currentUser
//                ?: return Resource.error("User not authenticated", ErrorType.NOT_AUTHENTICATED_ERROR)
//
//            // Reauthenticate пользователя
//            val credential = EmailAuthProvider.getCredential(user.email ?: "", password)
//            user.reauthenticate(credential).await()
//
//            // Обновляем email в Firebase Auth
//            user.updateEmail(newEmail).await()
//
//            // Отправляем email verification
//            user.sendEmailVerification().await()
//
//            Resource.success(null)
//        } catch (e: Exception) {
//            when (e) {
//                is FirebaseAuthInvalidCredentialsException -> Resource.error(
//                    "Invalid password",
//                    ErrorType.AUTH_DATA_ERROR
//                )
//                is FirebaseAuthInvalidUserException -> Resource.error(
//                    "User not found",
//                    ErrorType.AUTH_DATA_ERROR
//                )
//                is FirebaseAuthUserCollisionException -> Resource.error(
//                    "Email already in use",
//                    ErrorType.EMAIL_ALREADY_REGISTERED_ERROR
//                )
//                else -> Resource.error(
//                    e.message ?: "Failed to update auth email",
//                    ErrorType.DATA_NOT_SAVED_ERROR
//                )
//            }
//        }
//    }

//    suspend fun updateAuthEmail(newEmail: String, password: String): Resource<ErrorType> {
//        return try {
//            val user = auth.currentUser
//                ?: return Resource.error("User not authenticated", ErrorType.NOT_AUTHENTICATED_ERROR)
//
//            val currentUserEmail = user.email ?: ""
//
//            // Reauthenticate пользователя
//            val credential = EmailAuthProvider.getCredential(currentUserEmail, password)
//            user.reauthenticate(credential).await()
//            Log.d(TAG, "Reauthentication successful")
//
//            // Обновляем email в Firebase Auth
//            user.updateEmail(newEmail).await()
//            Log.d(TAG, "Email updated in Firebase Auth")
//
//            // ОБНОВЛЯЕМ EMAIL В FIRESTORE ТОЖЕ!
//            // Получаем текущие данные пользователя из Firestore
//            val document = firestore.collection(USERS_COLLECTION)
//                .document(user.uid)
//                .get()
//                .await()
//
//            if (document.exists()) {
//                // Обновляем displayEmail в Firestore
//                val updateData = mapOf(
//                    UserFirestoreFields.DISPLAY_EMAIL to newEmail,
//                    UserFirestoreFields.UPDATED_AT to FieldValue.serverTimestamp()
//                )
//
//                firestore.collection(USERS_COLLECTION)
//                    .document(user.uid)
//                    .update(updateData)
//                    .await()
//                Log.d(TAG, "Email updated in Firestore")
//            }
//
//            // Отправляем email verification
//            user.sendEmailVerification().await()
//            Log.d(TAG, "Verification email sent")
//
//            Resource.success(null)
//        } catch (e: Exception) {
//            Log.e(TAG, "Error updating auth email: ${e.message}", e)
//            when (e) {
//                is FirebaseAuthInvalidCredentialsException -> Resource.error(
//                    "Invalid password",
//                    ErrorType.AUTH_DATA_ERROR
//                )
//                is FirebaseAuthInvalidUserException -> Resource.error(
//                    "User not found",
//                    ErrorType.AUTH_DATA_ERROR
//                )
//                is FirebaseAuthUserCollisionException -> Resource.error(
//                    "Email already in use",
//                    ErrorType.EMAIL_ALREADY_REGISTERED_ERROR
//                )
//                is FirebaseFirestoreException -> Resource.error(
//                    "Failed to update email in database",
//                    ErrorType.DATA_NOT_SAVED_ERROR
//                )
//                else -> Resource.error(
//                    e.message ?: "Failed to update auth email",
//                    ErrorType.DATA_NOT_SAVED_ERROR
//                )
//            }
//        }
//    }

//    suspend fun updateAuthEmail(newEmail: String, password: String): Resource<ErrorType> {
//        return try {
//            val user = auth.currentUser
//                ?: return Resource.error("User not authenticated", ErrorType.NOT_AUTHENTICATED_ERROR)
//
//            val currentUserEmail = user.email ?: ""
//
//            // Reauthenticate пользователя
//            val credential = EmailAuthProvider.getCredential(currentUserEmail, password)
//            user.reauthenticate(credential).await()
//            Log.d(TAG, "Reauthentication successful")
//
//            // ВАЖНО: Используем verifyBeforeUpdateEmail вместо updateEmail
//            user.verifyBeforeUpdateEmail(newEmail).await()
//            Log.d(TAG, "Verification email sent to new email")
//
//            // Обновляем displayEmail в Firestore (но authEmail останется старым до верификации)
//            val updateData = mapOf(
//                UserFirestoreFields.DISPLAY_EMAIL to newEmail,
//                UserFirestoreFields.UPDATED_AT to FieldValue.serverTimestamp()
//            )
//
//            firestore.collection(USERS_COLLECTION)
//                .document(user.uid)
//                .update(updateData)
//                .await()
//            Log.d(TAG, "Display email updated in Firestore")
//
//            Resource.success(null)
//        } catch (e: Exception) {
//            Log.e(TAG, "Error updating auth email: ${e.message}", e)
//            when (e) {
//                is FirebaseAuthInvalidCredentialsException -> Resource.error(
//                    "Invalid password",
//                    ErrorType.AUTH_DATA_ERROR
//                )
//                is FirebaseAuthInvalidUserException -> Resource.error(
//                    "User not found",
//                    ErrorType.AUTH_DATA_ERROR
//                )
//                is FirebaseAuthUserCollisionException -> Resource.error(
//                    "Email already in use",
//                    ErrorType.EMAIL_ALREADY_REGISTERED_ERROR
//                )
//                is FirebaseFirestoreException -> Resource.error(
//                    "Failed to update email in database",
//                    ErrorType.DATA_NOT_SAVED_ERROR
//                )
//                else -> Resource.error(
//                    e.message ?: "Failed to update auth email",
//                    ErrorType.DATA_NOT_SAVED_ERROR
//                )
//            }
//        }
//    }

//    suspend fun updateAuthEmail(newEmail: String, password: String): Resource<ErrorType> {
//        return try {
//            val user = auth.currentUser
//                ?: return Resource.error("User not authenticated", ErrorType.NOT_AUTHENTICATED_ERROR)
//
//            val currentUserEmail = user.email ?: ""
//
//            // ПРОВЕРЯЕМ, ВЕРИФИЦИРОВАН ЛИ ТЕКУЩИЙ EMAIL
//            if (!user.isEmailVerified) {
//                return Resource.error(
//                    "Your current email address is not verified. Please verify your current email before changing it.",
//                    ErrorType.CURRENT_EMAIL_NOT_VERIFIED
//                )
//            }
//
//            // Reauthenticate пользователя
//            val credential = EmailAuthProvider.getCredential(currentUserEmail, password)
//            user.reauthenticate(credential).await()
//            Log.d(TAG, "Reauthentication successful")
//
//            // Используем verifyBeforeUpdateEmail
//            user.verifyBeforeUpdateEmail(newEmail).await()
//            Log.d(TAG, "Verification email sent to new email")
//
//            // Обновляем displayEmail в Firestore
//            val updateData = mapOf(
//                UserFirestoreFields.DISPLAY_EMAIL to newEmail,
//                UserFirestoreFields.UPDATED_AT to FieldValue.serverTimestamp()
//            )
//
//            firestore.collection(USERS_COLLECTION)
//                .document(user.uid)
//                .update(updateData)
//                .await()
//            Log.d(TAG, "Display email updated in Firestore")
//
//            Resource.success(null)
//        } catch (e: Exception) {
//            Log.e(TAG, "Error updating auth email: ${e.message}", e)
//            when (e) {
//                is FirebaseAuthInvalidCredentialsException -> Resource.error(
//                    "Invalid password",
//                    ErrorType.AUTH_DATA_ERROR
//                )
//                is FirebaseAuthInvalidUserException -> Resource.error(
//                    "User not found",
//                    ErrorType.AUTH_DATA_ERROR
//                )
//                is FirebaseAuthUserCollisionException -> Resource.error(
//                    "Email already in use",
//                    ErrorType.EMAIL_ALREADY_REGISTERED_ERROR
//                )
//                is FirebaseFirestoreException -> Resource.error(
//                    "Failed to update email in database",
//                    ErrorType.DATA_NOT_SAVED_ERROR
//                )
//                else -> Resource.error(
//                    e.message ?: "Failed to update auth email",
//                    ErrorType.DATA_NOT_SAVED_ERROR
//                )
//            }
//        }
//    }

    suspend fun updateAuthEmail(newEmail: String, password: String): Resource<ErrorType> {
        return try {
            val user = auth.currentUser
                ?: return Resource.error("User not authenticated", ErrorType.NOT_AUTHENTICATED_ERROR)

            val currentUserEmail = user.email ?: ""

            // 1. СНАЧАЛА ПРОВЕРЯЕМ ПАРОЛЬ (reauthenticate)
            val credential = EmailAuthProvider.getCredential(currentUserEmail, password)
            user.reauthenticate(credential).await()
            Log.d(TAG, "Reauthentication successful")

            // 2. ТЕПЕРЬ ПРОВЕРЯЕМ ВЕРИФИКАЦИЮ ТЕКУЩЕГО EMAIL
            if (!user.isEmailVerified) {
                return Resource.error(
                    "Your current email address is not verified. Please verify your current email before changing it.",
                    ErrorType.CURRENT_EMAIL_NOT_VERIFIED
                )
            }

            // 3. Используем verifyBeforeUpdateEmail
            user.verifyBeforeUpdateEmail(newEmail).await()
            Log.d(TAG, "Verification email sent to new email")

//            // 4. Обновляем displayEmail в Firestore
//            val updateData = mapOf(
//                UserFirestoreFields.DISPLAY_EMAIL to newEmail,
//                UserFirestoreFields.UPDATED_AT to FieldValue.serverTimestamp()
//            )

//            firestore.collection(USERS_COLLECTION)
//                .document(user.uid)
//                .update(updateData)
//                .await()
//            Log.d(TAG, "Display email updated in Firestore")

            Resource.success(null)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating auth email: ${e.message}", e)
            when (e) {
                is FirebaseAuthInvalidCredentialsException -> Resource.error(
                    "Invalid password",
                    ErrorType.AUTH_DATA_ERROR
                )
                is FirebaseAuthInvalidUserException -> Resource.error(
                    "User not found",
                    ErrorType.AUTH_DATA_ERROR
                )
                is FirebaseAuthUserCollisionException -> Resource.error(
                    "Email already in use",
                    ErrorType.EMAIL_ALREADY_REGISTERED_ERROR
                )
                is FirebaseFirestoreException -> Resource.error(
                    "Failed to update email in database",
                    ErrorType.DATA_NOT_SAVED_ERROR
                )
                else -> Resource.error(
                    e.message ?: "Failed to update auth email",
                    ErrorType.DATA_NOT_SAVED_ERROR
                )
            }
        }
    }
}
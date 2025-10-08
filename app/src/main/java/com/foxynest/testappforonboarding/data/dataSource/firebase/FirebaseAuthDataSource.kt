package com.foxynest.testappforonboarding.data.dataSource.firebase

import com.foxynest.testappforonboarding.domain.entities.UserFirestoreFields
import com.foxynest.testappforonboarding.domain.entities.UserPresentation
import com.foxynest.testappforonboarding.other.Resource
import com.foxynest.testappforonboarding.other.constants.CommonConstants.USERS_COLLECTION
import com.foxynest.testappforonboarding.other.extensions.toFirestoreMapWithCreationTimestamp
import com.foxynest.testappforonboarding.other.extensions.toFirestoreMapWithUpdatedAtTimestamp
import com.foxynest.testappforonboarding.other.extensions.toUserPresentation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
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
    suspend fun register(email: String, password: String, user: UserPresentation): Resource<Unit> {
        return try {
            val emailLowerCase = email.lowercase().trim()
            val result = auth.createUserWithEmailAndPassword(emailLowerCase, password).await()

            // Создаем документ пользователя в Firestore; uid создаётся автоматически, верификацтю почты тоже настраивать будем позже
            result.user?.let { nonNullFirebaseUser ->
                val userEmailLowerCase = nonNullFirebaseUser.email?.lowercase()?.trim() ?: ""

                val userData = user.toFirestoreMapWithCreationTimestamp()
                    .toMutableMap()
                    .apply {
                        put(UserFirestoreFields.EMAIL, userEmailLowerCase)
                    }

                firestore
                    .collection(USERS_COLLECTION)
                    .document(nonNullFirebaseUser.uid)
                    .set(userData)
                    .await()
            }

            Resource.success(Unit)
        } catch (e: Exception) {
            when (e) {
                is FirebaseAuthWeakPasswordException -> Resource.error("Password is too weak", null)
                is FirebaseAuthInvalidCredentialsException -> Resource.error(
                    "Invalid email format",
                    null
                )
                is FirebaseAuthUserCollisionException -> Resource.error("User already exists", null)
                else -> Resource.error(e.message ?: "Registration failed", null)
            }
        }
    }

    suspend fun login(email: String, password: String): Resource<Unit> {
        return try {
            val emailLowerCase = email.lowercase().trim()
            auth.signInWithEmailAndPassword(emailLowerCase, password).await()
            Resource.success(Unit)
        } catch (e: Exception) {
            when (e) {
                is FirebaseAuthInvalidCredentialsException -> Resource.error(
                    "Invalid email or password",
                    null
                )

                is FirebaseAuthInvalidUserException -> Resource.error("User not found", null)
                else -> Resource.error(e.message ?: "Login failed", null)
            }
        }
    }

    suspend fun logout(): Resource<Unit> {
        return try {
            auth.signOut()
            Resource.success(Unit)
        } catch (e: Exception) {
            Resource.error(e.message ?: "Logout failed", null)
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
                            email = firebaseUser.email ?: "",
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

    suspend fun updateProfile(user: UserPresentation): Resource<Unit> {
        return try {
            // Проверяем, что пользователь авторизован
            val currentUser =
                auth.currentUser ?: return Resource.error("User not authenticated", null)

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

            Resource.success(Unit)
        } catch (e: Exception) {
            Resource.error(e.message ?: "Profile update failed", null)
        }
    }

    suspend fun sendEmailVerification(): Resource<Unit> {
        return try {
            auth.currentUser?.sendEmailVerification()?.await()
            Resource.success(Unit)
        } catch (e: Exception) {
            Resource.error(e.message ?: "Failed to send verification email", null)
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
}
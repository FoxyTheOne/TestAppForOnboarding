package com.foxynest.testappforonboarding.domain.entities

import android.os.Parcelable
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import kotlinx.parcelize.Parcelize

@Parcelize
data class UserPresentation(
    val uid: String = "",
    val email: String = "",
    val isEmailVerified: Boolean = false,
    val userName: String = "",
    val childGender: String = "",
    val isItABoy: String = "",
    val childName: String = "",
    val childHobby: String = "",
    val childBirthDate: String = ""
) :
    Parcelable
//{
//    companion object {

//        Вы можете записать методы в companion object класса UserPresentation, но это не рекомендуется по нескольким причинам. Давайте разберем плюсы и минусы:
//
//        Проблемы вашего подхода:
//        1. Нарушение принципа единой ответственности (Single Responsibility Principle)
//        UserPresentation должен быть только моделью данных
//
//        Добавление логики преобразования усложняет класс
//
//        2. Зависимость от Firebase в domain слое
//        Domain слой должен быть независимым от инфраструктуры (Firebase)
//
//        Это нарушает принципы Clean Architecture
//
//        3. Сложность тестирования

//        fun firebaseUserToUser(
//            firebaseUid: String,
//            firebaseEmail: String,
//            firebaseIsEmailVerified: Boolean,
//            firebaseUserName: String,
//            firebaseChildGender: String,
//            firebaseIsItABoy: String,
//            firebaseChildName: String,
//            firebaseChildHobby: String,
//            firebaseChildAge: Int,
//            firebaseChildBirthDate: String
//        ): UserPresentation = UserPresentation(
//            firebaseUid,
//            firebaseEmail,
//            firebaseIsEmailVerified,
//            firebaseUserName,
//            firebaseChildGender,
//            firebaseChildName,
//            firebaseChildHobby,
//            firebaseIsItABoy,
//            firebaseChildAge,
//            firebaseChildBirthDate
//        )

//        fun userToFirestoreMap(user: UserPresentation): Map<String, Any> {
//            return mapOf(
//                UserFirestoreFields.EMAIL to user.email.lowercase().trim(),
//                UserFirestoreFields.USERNAME to user.userName,
//                UserFirestoreFields.CHILD_GENDER to user.childGender,
//                UserFirestoreFields.CHILD_IS_IT_A_BOY to user.isItABoy,
//                UserFirestoreFields.CHILD_NAME to user.childName,
//                UserFirestoreFields.CHILD_HOBBY to user.childHobby,
//                UserFirestoreFields.CHILD_AGE to user.childAge,
//                UserFirestoreFields.CHILD_BIRTH_DATE to user.childBirthDate
//            )
//        }

//        fun userToFirestoreMapWithTimestamp(user: UserPresentation, timestamp: FieldValue): Map<String, Any> {
//            return userToFirestoreMap(user).toMutableMap().apply {
//                put(UserFirestoreFields.UPDATED_AT, timestamp)
//            }
//        }
//
//        fun userToFirestoreMapWithCreationTimestamp(user: UserPresentation): Map<String, Any> {
//            return userToFirestoreMap(user).toMutableMap().apply {
//                put(UserFirestoreFields.CREATED_AT, FieldValue.serverTimestamp())
//            }
//        }

//        fun DocumentSnapshot.toUserPresentation(firebaseUser: FirebaseUser): UserPresentation {
//            return UserPresentation(
//                uid = firebaseUser.uid,
//                email = getString(UserFirestoreFields.EMAIL) ?: firebaseUser.email ?: "",
//                isEmailVerified = firebaseUser.isEmailVerified,
//                userName = getString(UserFirestoreFields.USERNAME) ?: "",
//                childGender = getString(UserFirestoreFields.CHILD_GENDER) ?: "",
//                isItABoy = getString(UserFirestoreFields.CHILD_IS_IT_A_BOY) ?: "",
//                childName = getString(UserFirestoreFields.CHILD_NAME) ?: "",
//                childHobby = getString(UserFirestoreFields.CHILD_HOBBY) ?: "",
//                childAge = getLong(UserFirestoreFields.CHILD_AGE)?.toInt() ?: 0,
//                childBirthDate = getString(UserFirestoreFields.CHILD_BIRTH_DATE) ?: ""
//            )
//        }

//    }
//}

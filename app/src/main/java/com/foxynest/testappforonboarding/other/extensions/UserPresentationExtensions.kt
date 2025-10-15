package com.foxynest.testappforonboarding.other.extensions

import com.foxynest.testappforonboarding.domain.entities.UserFirestoreFields
import com.foxynest.testappforonboarding.domain.entities.UserPresentation
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue

// Extension функции для UserPresentation
fun FirebaseUser.toUserPresentation(
    firebaseUid: String,
    firebaseEmail: String,
    firebaseIsEmailVerified: Boolean,
    firebaseUserName: String,
    firebaseChildGender: String,
    firebaseIsItABoy: String,
    firebaseChildName: String,
    firebaseChildHobby: String,
//    firebaseChildAge: Int,
    firebaseChildBirthDate: String
): UserPresentation = UserPresentation(
    firebaseUid,
    firebaseEmail,
    firebaseIsEmailVerified,
    firebaseUserName,
    firebaseChildGender,
    firebaseChildName,
    firebaseChildHobby,
    firebaseIsItABoy,
//    firebaseChildAge,
    firebaseChildBirthDate
)

fun UserPresentation.toFirestoreMap(): Map<String, Any> {
    return mapOf(
        UserFirestoreFields.EMAIL to this.email.lowercase().trim(),
        UserFirestoreFields.USERNAME to this.userName,
        UserFirestoreFields.CHILD_GENDER to this.childGender,
        UserFirestoreFields.CHILD_IS_IT_A_BOY to this.isItABoy,
        UserFirestoreFields.CHILD_NAME to this.childName,
        UserFirestoreFields.CHILD_HOBBY to this.childHobby,
//        UserFirestoreFields.CHILD_AGE to this.childAge,
        UserFirestoreFields.CHILD_BIRTH_DATE to this.childBirthDate
    )
}

fun UserPresentation.toFirestoreMapWithUpdatedAtTimestamp(timestamp: FieldValue): Map<String, Any> {
    return this.toFirestoreMap().toMutableMap().apply {
        put(UserFirestoreFields.UPDATED_AT, timestamp)
    }
}

fun UserPresentation.toFirestoreMapWithCreationTimestamp(): Map<String, Any> {
    return this.toFirestoreMap().toMutableMap().apply {
        put(UserFirestoreFields.CREATED_AT, FieldValue.serverTimestamp())
    }
}

// Extension функция для DocumentSnapshot
fun DocumentSnapshot.toUserPresentation(firebaseUser: FirebaseUser): UserPresentation {
    return UserPresentation(
        uid = firebaseUser.uid,
        email = getString(UserFirestoreFields.EMAIL) ?: firebaseUser.email ?: "",
        isEmailVerified = firebaseUser.isEmailVerified,
        userName = getString(UserFirestoreFields.USERNAME) ?: "",
        childGender = getString(UserFirestoreFields.CHILD_GENDER) ?: "",
        isItABoy = getString(UserFirestoreFields.CHILD_IS_IT_A_BOY) ?: "",
        childName = getString(UserFirestoreFields.CHILD_NAME) ?: "",
        childHobby = getString(UserFirestoreFields.CHILD_HOBBY) ?: "",
//        childAge = getLong(UserFirestoreFields.CHILD_AGE)?.toInt() ?: 0,
        childBirthDate = getString(UserFirestoreFields.CHILD_BIRTH_DATE) ?: ""
    )
}
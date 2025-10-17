package com.foxynest.testappforonboarding.other.extensions

import android.app.Activity
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import com.foxynest.testappforonboarding.R
import com.foxynest.testappforonboarding.domain.entities.onboarding.AnswerNames
import com.foxynest.testappforonboarding.domain.entities.onboarding.OnboardingAnswers
import com.foxynest.testappforonboarding.domain.entities.user.UserFirestoreFields
import com.foxynest.testappforonboarding.domain.entities.user.UserPresentation
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
        childBirthDate = getString(UserFirestoreFields.CHILD_BIRTH_DATE) ?: ""
    )
}

fun UserPresentation.isValidToSaveToFirebase(): Boolean =
    this.userName.isNotBlank() && this.childName.isNotBlank() && this.childHobby.isNotBlank() && this.childBirthDate.isNotBlank()

fun LiveData<OnboardingAnswers>.createUserFromLocalDataInFragment(activity: FragmentActivity?): UserPresentation {
    // Только для передачи данных для регистрации! Не для передачи в firebase
    return UserPresentation(
        "",
        this.value?.email ?: "",
        false, // Тут надо быть аккуратным, false - потому что ещё не зарегистрирован. Но на страницу регистрации может перейти и уже зарегистрированный пользователь. Не использовать эти данные для чего-либо, кроме регистрации!
        this.value?.userName ?: "",
        this.value?.childGender ?: "",
        this.value?.isItABoy ?: activity?.getString(R.string.no) ?: "",
        this.value?.childName ?: "",
        this.value?.childHobby ?: "",
        this.value?.childBirthDate ?: ""
    )
}

// Для answers: Map<String, String> в MainViewModel
fun Map<String, String>.createUserFromAnswersInViewModel(currentUserUid: String): UserPresentation {
    return UserPresentation(
        uid = currentUserUid,
        email = this[AnswerNames.EMAIL_ANSWER.toString()] ?: "",
        isEmailVerified = this[AnswerNames.IS_EMAIL_VERIFIED_ANSWER.toString()].toBoolean(),
        userName = this[AnswerNames.USER_NAME_ANSWER.toString()] ?: "",
        childGender = this[AnswerNames.CHILD_GENDER_ANSWER.toString()] ?: "",
        isItABoy = this[AnswerNames.IS_IT_A_BOY_ANSWER.toString()] ?: "",
        childName = this[AnswerNames.CHILD_NAME_ANSWER.toString()] ?: "",
        childHobby = this[AnswerNames.CHILD_HOBBY_ANSWER.toString()] ?: "",
        childBirthDate = this[AnswerNames.CHILD_BIRTH_DATE_ANSWER.toString()] ?: ""
    )
}
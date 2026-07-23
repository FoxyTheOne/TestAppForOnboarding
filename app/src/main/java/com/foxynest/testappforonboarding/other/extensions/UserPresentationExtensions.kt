package com.foxynest.testappforonboarding.other.extensions

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

fun UserPresentation.toFirestoreMap(): Map<String, Any> {
    return mapOf(
        UserFirestoreFields.USERNAME to this.userName,
        UserFirestoreFields.CHILD_GENDER to this.childGender,
        UserFirestoreFields.CHILD_IS_IT_A_BOY to this.isItABoy,
        UserFirestoreFields.CHILD_NAME to this.childName,
        UserFirestoreFields.CHILD_HOBBIES to this.childHobbies,
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
        authEmail = firebaseUser.email ?: "", // Email для входа
        isEmailVerified = firebaseUser.isEmailVerified, // Верификация для входа
        userName = getString(UserFirestoreFields.USERNAME) ?: "",
        childGender = getString(UserFirestoreFields.CHILD_GENDER) ?: "",
        isItABoy = getString(UserFirestoreFields.CHILD_IS_IT_A_BOY) ?: "",
        childName = getString(UserFirestoreFields.CHILD_NAME) ?: "",
        childHobbies = getString(UserFirestoreFields.CHILD_HOBBIES) ?: "",
        childBirthDate = getString(UserFirestoreFields.CHILD_BIRTH_DATE) ?: ""
    )
}

fun UserPresentation.isUserDataValid(): Boolean =
    this.userName.isNotBlank() && this.childName.isNotBlank() && this.childHobbies.isNotBlank() && this.childBirthDate.isNotBlank()

fun LiveData<OnboardingAnswers>.createUserFromLocalDataInFragment(activity: FragmentActivity?): UserPresentation {
    // Только для передачи данных для регистрации! Не для передачи в firebase
    // Тут надо быть аккуратным, false - потому что ещё не зарегистрирован. Но на страницу регистрации может перейти и уже зарегистрированный пользователь. Не использовать эти данные для чего-либо, кроме регистрации!
    return UserPresentation(
        userName = this.value?.userName ?: "",
        childGender = this.value?.childGender ?: "",
        isItABoy = this.value?.isItABoy ?: activity?.getString(R.string.no) ?: "",
        childName = this.value?.childName ?: "",
        childHobbies = this.value?.childHobbies ?: "",
        childBirthDate = this.value?.childBirthDate ?: ""
    )
}

// Для answers: Map<String, String> в MainViewModel
fun Map<String, String>.createUpdatedUserFromAnswersInViewModel(currentUser: UserPresentation): UserPresentation {
    return UserPresentation(
        uid = currentUser.uid,
        authEmail = currentUser.authEmail,
        isEmailVerified = this[AnswerNames.IS_EMAIL_VERIFIED_ANSWER.toString()].toBoolean(),
        userName = this[AnswerNames.USER_NAME_ANSWER.toString()] ?: "",
        childGender = this[AnswerNames.CHILD_GENDER_ANSWER.toString()] ?: "",
        isItABoy = this[AnswerNames.IS_IT_A_BOY_ANSWER.toString()] ?: "",
        childName = this[AnswerNames.CHILD_NAME_ANSWER.toString()] ?: "",
        childHobbies = this[AnswerNames.CHILD_HOBBIES_ANSWER.toString()] ?: "",
        childBirthDate = this[AnswerNames.CHILD_BIRTH_DATE_ANSWER.toString()] ?: ""
    )
}
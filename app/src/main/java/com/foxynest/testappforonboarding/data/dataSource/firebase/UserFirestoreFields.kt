package com.foxynest.testappforonboarding.data.dataSource.firebase

/**
 * Имена полей документа users/{uid} в Firestore.
 * Если добавится новое поле:
 * 1. Добавьте константу сюда (без uid и isEmailVerified – они из Firebase Auth)
 * 2. Добавьте поле в UserPresentation и OnboardingAnswers
 * 3. Обновите UserFirestoreMapper и firestore.rules
 * 4. Обновите тесты
 */
object UserFirestoreFields {
    const val USERNAME = "username"
    const val CHILD_GENDER = "childGender"
    const val CHILD_IS_IT_A_BOY = "childIsItABoy"
    const val CHILD_NAME = "childName"
    const val CHILD_HOBBIES = "childHobbies"
    const val CHILD_BIRTH_DATE = "childBirthDate"
    const val CREATED_AT = "createdAt"
    const val UPDATED_AT = "updatedAt"
}
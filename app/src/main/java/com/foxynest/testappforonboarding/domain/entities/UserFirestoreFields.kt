package com.foxynest.testappforonboarding.domain.entities

/**
 * Если добавится новое поле:
 * 1. Добавьте константу в FirestoreFields (без uid и isEmailVerified), а так же в AnswerNames
 * 2. Добавьте поле в UserPresentation
 * 3. Обновите toFirestoreMap() и toUserPresentation()
 * 4. Обновите тесты
 * Все изменения будут сосредоточены в одном месте, что значительно снижает риск ошибок.
 */
object UserFirestoreFields {
    const val EMAIL = "email"
    const val USERNAME = "username"
    const val CHILD_GENDER = "childGender"
    const val CHILD_IS_IT_A_BOY = "childIsItABoy"
    const val CHILD_NAME = "childName"
    const val CHILD_HOBBY = "childHobby"
    const val CHILD_AGE = "childAge"
    const val CHILD_BIRTH_DATE = "childBirthDate"
    const val CREATED_AT = "createdAt"
    const val UPDATED_AT = "updatedAt"
}
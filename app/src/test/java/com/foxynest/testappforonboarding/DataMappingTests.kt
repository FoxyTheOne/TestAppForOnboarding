package com.foxynest.testappforonboarding

import com.foxynest.testappforonboarding.domain.entities.user.UserFirestoreFields
import com.foxynest.testappforonboarding.domain.entities.user.UserPresentation
import com.foxynest.testappforonboarding.other.extensions.toFirestoreMap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class DataMappingTests {
    @Test
    fun `user presentation to firestore map contains profile fields`() {
        val user = UserPresentation(
            uid = "123",
            authEmail = "test@test.com",
            userName = "Test User",
            childGender = "BOY",
            childName = "Child Name",
            childBirthDate = "2020-05-19"
        )

        val firestoreMap = user.toFirestoreMap()

        assertEquals("Test User", firestoreMap[UserFirestoreFields.USERNAME])
        assertEquals("BOY", firestoreMap[UserFirestoreFields.CHILD_GENDER])
        assertEquals("2020-05-19", firestoreMap[UserFirestoreFields.CHILD_BIRTH_DATE])
        // uid и почта для входа не дублируются в документе профиля
        assertFalse(firestoreMap.values.contains("test@test.com"))
    }
}
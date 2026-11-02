package com.foxynest.testappforonboarding

import com.foxynest.testappforonboarding.data.dataSource.firebase.UserFirestoreFields
import com.foxynest.testappforonboarding.data.dataSource.firebase.UserFirestoreMapper
import com.foxynest.testappforonboarding.domain.entities.user.UserPresentation
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
            childHobbies = "chess, books",
            childBirthDate = "2020-05-19"
        )

        val firestoreMap = UserFirestoreMapper.toFirestoreMap(user)

        assertEquals("Test User", firestoreMap[UserFirestoreFields.USERNAME])
        assertEquals("BOY", firestoreMap[UserFirestoreFields.CHILD_GENDER])
        assertEquals("2020-05-19", firestoreMap[UserFirestoreFields.CHILD_BIRTH_DATE])
        assertEquals(listOf("chess", "books"), firestoreMap[UserFirestoreFields.CHILD_HOBBIES])
        // uid и почта для входа не дублируются в документе профиля
        assertFalse(firestoreMap.values.contains("test@test.com"))
    }
}
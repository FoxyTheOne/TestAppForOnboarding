package com.foxynest.testappforonboarding

import com.foxynest.testappforonboarding.domain.entities.user.UserPresentation
import com.foxynest.testappforonboarding.other.extensions.convertDateMillisToSaveFormat
import com.foxynest.testappforonboarding.other.extensions.toFirestoreMap
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test

class DataMappingTests {
    @Test
    fun `user presentation to firestore map contains all fields`() {
        val user = UserPresentation(
            uid = "123",
            email = "test@test.com",
            userName = "Test User",
            childGender = "Мальчик",
            childName = "Child Name"
        )

        val firestoreMap = user.toFirestoreMap()

        assertEquals("test@test.com", firestoreMap["email"])
        assertEquals("Test User", firestoreMap["username"])
        assertEquals("Мальчик", firestoreMap["childGender"])
    }

    @Test
    fun `date conversion works correctly`() {
        val timestamp = System.currentTimeMillis()
        val dateString = timestamp.convertDateMillisToSaveFormat()

        assertTrue(dateString.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
    }
}
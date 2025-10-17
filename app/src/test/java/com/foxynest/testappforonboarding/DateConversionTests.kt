package com.foxynest.testappforonboarding

import com.foxynest.testappforonboarding.other.extensions.convertDateMillisToSaveFormat
import com.foxynest.testappforonboarding.presentation.profile.ProfileFragment
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test

class DateConversionTests {
    @Test
    fun `convertDateMillisToSaveFormat returns correct format`() {
        // Given
        val timestamp = 1609459200000L // 2021-01-01

        // When
        val result = timestamp.convertDateMillisToSaveFormat()

        // Then
        assertEquals("2021-01-01", result)
    }

//    @Test
//    fun `calculateAgeFromBirthDate returns correct age`() {
//        // Given
//        val fragment = ProfileFragment()
//        val birthDate = "2020-01-01" // 3 года назад (если сейчас 2023)
//
//        // When
//        val age = fragment.calculateAgeFromBirthDate(birthDate)
//
//        // Then
//        assertTrue(age > 0)
//    }
}
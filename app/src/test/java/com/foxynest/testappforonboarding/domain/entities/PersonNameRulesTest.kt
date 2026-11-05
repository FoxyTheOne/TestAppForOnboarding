package com.foxynest.testappforonboarding.domain.entities

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonNameRulesTest {

    private fun valid(raw: String) = PersonNameRules.validate(PersonNameRules.normalize(raw))

    @Test
    fun `names with space hyphen and apostrophe are valid`() {
        listOf(
            "Анна",
            "Анна-Мария",
            "Mary Ann",
            "Д'Артаньян",
            "O’Brien",
            "Жан-Поль Сартр"
        ).forEach {
            assertNull("«$it» должно быть допустимым", valid(it))
        }
    }

    @Test
    fun `letters of any alphabet are allowed`() {
        listOf("Zoë", "Ľubica", "Өмір", "ნინო", "Ёжик").forEach {
            assertNull("«$it» должно быть допустимым", valid(it))
        }
    }

    @Test
    fun `digits emoji and symbols are not allowed`() {
        assertFalse(PersonNameRules.isAllowedChar('1'))
        assertFalse(PersonNameRules.isAllowedChar('@'))
        assertFalse(PersonNameRules.isAllowedChar('_'))
        assertTrue(PersonNameRules.isAllowedChar('-'))
        assertEquals(ErrorType.NAME_INVALID_ERROR, valid("Анна1"))
    }

    @Test
    fun `separators only between letters`() {
        listOf("-Анна", "Анна-", "Анна--Мария", "Анна -Мария", "'", "- -").forEach {
            assertEquals("«$it» должно быть недопустимым", ErrorType.NAME_INVALID_ERROR, valid(it))
        }
    }

    @Test
    fun `empty name has its own error`() {
        assertEquals(ErrorType.FIELD_EMPTY_ERROR, valid("   "))
    }

    @Test
    fun `extra spaces are collapsed`() {
        assertEquals("Анна Мария", PersonNameRules.normalize("  Анна    Мария  "))
    }

    @Test
    fun `length limit matches firestore rules`() {
        assertNull(valid("а".repeat(PersonNameRules.MAX_LENGTH)))
        assertEquals(
            ErrorType.NAME_INVALID_ERROR,
            valid("а".repeat(PersonNameRules.MAX_LENGTH + 1))
        )
        // Если меняете это число – поменяйте и firestore.rules (username/childName.size() <= ...)
        assertEquals(100, PersonNameRules.MAX_LENGTH)
    }
}
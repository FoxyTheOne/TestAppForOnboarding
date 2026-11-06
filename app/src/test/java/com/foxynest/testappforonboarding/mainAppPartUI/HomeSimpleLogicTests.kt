package com.foxynest.testappforonboarding.mainAppPartUI

import junit.framework.TestCase.assertTrue
import org.junit.Test

/**
 * Учебный пример теста «логики без фрагмента». Проверяет функцию, объявленную в самом тесте,
 * а не код приложения – на настоящий HomeFragment он не влияет.
 */
class HomeSimpleLogicTests {
    @Test
    fun `format home title for boy with name`() {
        // Given
        val gender = "Мальчик"
        val childName = "Алексей"

        // When - тестируем только логику, без фрагмента
        val result = formatHomeTitleLogic(gender, childName)

        // Then
        assertTrue(result.contains("Алексей"))
        assertTrue(result.contains("мальчик"))
    }

    // Выносим логику в отдельную функцию для тестирования
    private fun formatHomeTitleLogic(gender: String, childName: String): String {
        val textToShow = StringBuilder()
        textToShow.append("Приветствую ") // замените на реальные ресурсы

        when (gender) {
            "Мальчик" -> textToShow.append("мальчик")
            "Девочка" -> textToShow.append("девочка")
            else -> textToShow.append("малыш")
        }

        if (childName.isNotBlank() && childName != "null") {
            textToShow.append(" $childName")
        } else {
            textToShow.append(" малыш")
        }

        textToShow.append("?")
        return textToShow.toString()
    }
}
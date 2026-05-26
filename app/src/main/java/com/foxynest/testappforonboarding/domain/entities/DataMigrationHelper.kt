package com.foxynest.testappforonboarding.domain.entities

import com.foxynest.testappforonboarding.other.constants.CommonConstants

object DataMigrationHelper {
    private val genderMapping = mapOf(
        "Мальчик" to "BOY",
        "Девочка" to "GIRL"
    )

    private val hobbyMapping = mapOf(
        "рисование" to "drawing",
        "конструирование" to "construction",
        "музыкальные инструменты" to "musical_instruments",
        "футбол" to "football",
        "книги" to "books",
        "изучение иностранных языков" to "learning_languages",
        "математика" to "math",
        "шахматы" to "chess",
        "плавание" to "swimming",
        "единоборства" to "martial_arts",
        "хореография" to "choreography",
    )

    private val validHobbyKeys = hobbyMapping.values.toSet()

    fun migrateGender(oldGender: String): String = genderMapping[oldGender] ?: oldGender

    fun migrateHobbies(oldHobbies: String): String {
        if (oldHobbies.isBlank()) return oldHobbies
        val parts = oldHobbies.split(CommonConstants.HOBBIES_DELIMITER)
        val newParts = parts.map { hobbyMapping[it] ?: it }
        return newParts.joinToString(CommonConstants.HOBBIES_DELIMITER)
    }

    fun needsHobbyMigration(hobbiesString: String): Boolean {
        if (hobbiesString.isBlank()) return false
        val parts = hobbiesString.split(CommonConstants.HOBBIES_DELIMITER)
        return parts.any { it !in validHobbyKeys }
    }
}
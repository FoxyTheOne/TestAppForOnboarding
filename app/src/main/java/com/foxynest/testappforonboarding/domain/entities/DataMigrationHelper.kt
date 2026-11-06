package com.foxynest.testappforonboarding.domain.entities

import com.foxynest.testappforonboarding.other.constants.CommonConstants

/**
 * Перевод старых значений анкеты в ключи. Раньше пол и хобби хранились текстом на русском («Мальчик», «шахматы»),
 * и при смене языка в профиле показывался русский текст. Теперь хранятся ключи (`BOY`, `chess`),
 * а текст подбирается по языку при показе (`presentation/common/DomainDisplay.kt`).
 * Вызывается из `MigrateUserDataRepository`. Уже переведённые значения остаются как есть.
 */
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

    fun migrateGender(oldGender: String): String = genderMapping[oldGender] ?: oldGender

    fun migrateHobbies(oldHobbies: String): String {
        if (oldHobbies.isBlank()) return oldHobbies
        val parts = oldHobbies.split(CommonConstants.HOBBIES_DELIMITER)
        val newParts = parts.map { hobbyMapping[it] ?: it }
        return newParts.joinToString(CommonConstants.HOBBIES_DELIMITER)
    }
}
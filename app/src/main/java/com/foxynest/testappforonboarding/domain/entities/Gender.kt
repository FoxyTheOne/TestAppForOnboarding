package com.foxynest.testappforonboarding.domain.entities

/** Пол ребёнка. Хранится ключом (`BOY` / `GIRL`), текст на нужном языке – `Gender.displayName()` в DomainDisplay. */
enum class Gender {
    BOY, GIRL;

    // DataMigrationHelper при первом запуске переводит старые русские строки в ключи "BOY"/"GIRL"
    companion object {
        fun genderFromString(key: String?): Gender = when (key?.uppercase()) {
            "GIRL" -> GIRL
            else -> BOY
        }
    }
}
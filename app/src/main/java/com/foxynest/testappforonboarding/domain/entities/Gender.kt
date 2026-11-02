package com.foxynest.testappforonboarding.domain.entities

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
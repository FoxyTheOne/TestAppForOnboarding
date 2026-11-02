package com.foxynest.testappforonboarding.domain.entities.language

enum class LanguageEnum {
    SYSTEM, EN, RU;

    companion object {
        fun languageFromString(value: String?): LanguageEnum = when (value?.lowercase()) {
            "en" -> EN
            "ru" -> RU
            else -> SYSTEM
        }

        fun stringFromLanguage(value: LanguageEnum?): String = when (value) {
            EN -> "en"
            RU -> "ru"
            else -> "system"
        }
    }
}
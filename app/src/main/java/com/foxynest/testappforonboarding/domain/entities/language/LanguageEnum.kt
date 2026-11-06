package com.foxynest.testappforonboarding.domain.entities.language

/**
 * Язык интерфейса, выбранный в профиле. SYSTEM – как в настройках телефона.
 * В DataStore хранится строкой ("system" / "en" / "ru"), переводы туда и обратно – в companion object.
 */
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
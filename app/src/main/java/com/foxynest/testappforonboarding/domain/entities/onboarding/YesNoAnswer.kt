package com.foxynest.testappforonboarding.domain.entities.onboarding

/**
 * Ответ «да/нет» хранится ключом, а не текстом кнопки.
 * Раньше сохранялся текст кнопки («Да»/«Yes»), и после смены языка в профиле показывался ответ на старом языке.
 */
enum class YesNoAnswer {
    YES, NO;

    companion object {
        private val legacyValues = mapOf(
            "да" to YES, "yes" to YES,
            "нет" to NO, "no" to NO
        )

        /** Понимает и новые ключи, и старые локализованные значения (данные до миграции, в т.ч. в Firebase). */
        fun fromStoredValue(value: String?): YesNoAnswer? {
            val normalized = value?.trim() ?: return null
            return entries.firstOrNull { it.name == normalized.uppercase() }
                ?: legacyValues[normalized.lowercase()]
        }

        fun migrate(value: String): String = fromStoredValue(value)?.name ?: value
    }
}
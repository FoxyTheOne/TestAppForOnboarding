package com.foxynest.testappforonboarding.domain.entities.language

import android.content.Context
import com.foxynest.testappforonboarding.R

enum class LanguageEnum {
    SYSTEM, EN, RU;

    fun getDisplayName(context: Context): String = when (this) {
        SYSTEM -> context.getString(R.string.profile_lang_system)
        EN -> context.getString(R.string.profile_lang_en)
        RU -> context.getString(R.string.profile_lang_ru)
    }

    companion object {
        fun languageFromString(value: String?): LanguageEnum = when (value?.lowercase()) {
            "system" -> SYSTEM
            "en" -> EN
            "ru" -> RU
            else -> SYSTEM
        }

        fun stringFromLanguage(value: LanguageEnum?): String = when (value) {
            SYSTEM -> "system"
            EN -> "en"
            RU -> "ru"
            else -> "system"
        }
    }
}
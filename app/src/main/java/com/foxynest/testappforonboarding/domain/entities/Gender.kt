package com.foxynest.testappforonboarding.domain.entities


import android.content.Context
import com.foxynest.testappforonboarding.R

enum class Gender {
    BOY, GIRL;

    // Функция-расширение для получения локализованного названия
    fun getDisplayName(context: Context): String = when (this) {
        BOY -> context.getString(R.string.text_boy)
        GIRL -> context.getString(R.string.text_girl)
    }

    companion object {
        fun fromString(key: String?): Gender = when (key?.uppercase()) {
            "BOY" -> BOY
            "GIRL" -> GIRL
            else -> BOY // или бросить исключение, но для безопасности используем BOY
        }
    }
}
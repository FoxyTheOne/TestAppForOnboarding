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

    // Вы уже создали DataMigrationHelper, который преобразует старые русские строки в ключи "BOY"/"GIRL" при первом запуске. После миграции все данные в DataStore и Firebase будут хранить только ключи. Поэтому в нормальной работе fromString никогда не получит русское значение. Это правильно и безопасно
    companion object {
        fun genderFromString(key: String?): Gender = when (key?.uppercase()) {
            "BOY" -> BOY
            "GIRL" -> GIRL
            else -> BOY // или бросить исключение, но для безопасности используем BOY
        }
    }
}
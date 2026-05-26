package com.foxynest.testappforonboarding.domain.entities

import android.content.Context
import com.foxynest.testappforonboarding.R

object HobbyLocalizer {
    private val hobbyKeyToResId = mapOf(
        "drawing" to R.string.hobby_drawing,
        "construction" to R.string.hobby_construction,
        "musical_instruments" to R.string.hobby_musical_instruments,
        "football" to R.string.hobby_football,
        "books" to R.string.hobby_books,
        "learning_languages" to R.string.hobby_learning_languages,
        "math" to R.string.hobby_math,
        "chess" to R.string.hobby_chess,
        "swimming" to R.string.hobby_swimming,
        "martial_arts" to R.string.hobby_martial_arts,
        "choreography" to R.string.hobby_choreography
    )

    fun getDisplayName(context: Context, key: String): String {
        val resId = hobbyKeyToResId[key]
        return if (resId != null) context.getString(resId) else key
    }
}
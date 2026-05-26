package com.foxynest.testappforonboarding.other.extensions

import com.foxynest.testappforonboarding.other.Resource
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun Date.calculateChildAge(): Resource<Int> {
    val birthDate = this
    try {
        val today = Calendar.getInstance()
        val birthCalendar = Calendar.getInstance().apply { time = birthDate }

        var age = today.get(Calendar.YEAR) - birthCalendar.get(Calendar.YEAR)

        // Более точная проверка дня рождения
        val todayMonth = today.get(Calendar.MONTH)
        val birthMonth = birthCalendar.get(Calendar.MONTH)

        if (todayMonth < birthMonth) {
            age--
        } else if (todayMonth == birthMonth) {
            val todayDay = today.get(Calendar.DAY_OF_MONTH)
            val birthDay = birthCalendar.get(Calendar.DAY_OF_MONTH)
            if (todayDay < birthDay) {
                age--
            }
        }

        return Resource.success(age)
    } catch (e: Exception) {
        return Resource.error(
            "Error calculating age from birth date: $birthDate : ${e.message}",
            null
        )
    }
}

fun Long.convertDateMillisToMySaveFormat(): String {
    return try {
        // Используем стандартный ISO-подобный формат для хранения: yyyy-MM-dd
        val saveFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        saveFormatter.format(Date(this))
    } catch (e: Exception) {
        ""
    }
}
package com.foxynest.testappforonboarding.other.extensions

import com.foxynest.testappforonboarding.other.Resource
import com.foxynest.testappforonboarding.other.constants.BirthdayNotificationConstants.BIRTHDAY_NOTIF_TARGET_HOUR
import com.foxynest.testappforonboarding.other.constants.BirthdayNotificationConstants.BIRTHDAY_NOTIF_TARGET_MINUTE
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

fun String.parseBirthDateStringToCalendarDate(): Calendar? {
    val birthDate = this
    return try {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val date = formatter.parse(birthDate) ?: return null
        Calendar.getInstance().apply { time = date }
    } catch (_: Exception) {
        null
    }
}

fun String.isTodayBirthday(today: Date): Boolean {
    val birthDate = this

    val calendar = Calendar.getInstance().apply { time = today }
    val birthCalendar = Calendar.getInstance().apply {
        // парсим birthDate
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        time = formatter.parse(birthDate) ?: return false
    }
    // Чтобы не было ошибки в високосный год, лучше сравнивать месяца
    return calendar.get(Calendar.MONTH) == birthCalendar.get(Calendar.MONTH) &&
            calendar.get(Calendar.DAY_OF_MONTH) == birthCalendar.get(Calendar.DAY_OF_MONTH)

//    return try {
//        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
//        val date = formatter.parse(birthDate) ?: return false
//        val calendar = Calendar.getInstance()
//        val today = Calendar.getInstance()
//        calendar.time = date
////        today.get(Calendar.DAY_OF_YEAR) == calendar.get(Calendar.DAY_OF_YEAR)
//        // Чтобы не было ошибки в високосный год, лучше сравнивать месяца
//        today.get(Calendar.MONTH) == calendar.get(Calendar.MONTH) &&
//                today.get(Calendar.DAY_OF_MONTH) == calendar.get(Calendar.DAY_OF_MONTH)
//    } catch (e: Exception) {
//        false
//    }
}

fun Date.calculateWorkerDelayToNextMorning(): Long {
    val now = this
    val calendar = Calendar.getInstance().apply { time = now }
    calendar.set(Calendar.HOUR_OF_DAY, BIRTHDAY_NOTIF_TARGET_HOUR)
    calendar.set(Calendar.MINUTE, BIRTHDAY_NOTIF_TARGET_MINUTE)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)

    return if (calendar.timeInMillis > now.time) {
        // Сегодняшнее утро ещё не наступило
        calendar.timeInMillis - now.time
    } else {
        // Следующее утро
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        calendar.timeInMillis - now.time
    }
}
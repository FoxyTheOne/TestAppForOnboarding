package com.foxynest.testappforonboarding.other.extensions

import android.util.Log
import com.foxynest.testappforonboarding.other.Resource
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Парсит строку в формате "yyyy-MM-dd" в LocalDate.
 * Возвращает null при ошибке.
 */
fun String.parseDateToLocalDate(): LocalDate? {
    val dateStr = this
    return try {
        // формат yyyy-MM-dd
        LocalDate.parse(
            dateStr,
            DateTimeFormatter.ISO_LOCAL_DATE
        )
    } catch (e: Exception) {
        Log.e("DateExtensions", "Ошибка парсинга даты: $dateStr", e)
        null
    }
}

/**
 * Форматирует LocalDate в строку "yyyy-MM-dd".
 */
fun LocalDate.toMySaveFormatString(): String = this.format(DateTimeFormatter.ISO_LOCAL_DATE)

/**
 * Вычисляет возраст (целое число лет) на текущую дату.
 * Возвращает Resource.success(age) или Resource.error.
 */
fun LocalDate.calculateAge(now: LocalDate = LocalDate.now()): Resource<Int> {
    return try {
        val age = ChronoUnit.YEARS.between(this, now).toInt()
        Resource.success(age)
    } catch (e: Exception) {
        Resource.error("Error calculating age: ${e.message}", null)
    }
}

//fun Long.convertDateMillisToMySaveFormat(): String {
//    return try {
//        // Используем стандартный ISO-подобный формат для хранения: yyyy-MM-dd
//        val saveFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
//        saveFormatter.format(Date(this))
//    } catch (e: Exception) {
//        ""
//    }
//}

/**
 * Преобразует миллисекунды (timestamp) в LocalDate.
 */
fun Long.toLocalDate(): LocalDate? {
    return try {
        Instant.ofEpochMilli(this)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    } catch (e: Exception) {
        null
    }
}

/**
 * Преобразует LocalDateTime в миллисекунды для AlarmManager.
 */
fun LocalDateTime.toEpochMillis(): Long =
    this.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

/**
 * Проверяет, является ли дата сегодня или вчера (относительно текущей даты).
 */
fun LocalDate.isTodayOrYesterday(now: LocalDate = LocalDate.now()): Pair<Boolean, Boolean> {
    val today = now
    val yesterday = now.minusDays(1)
    return (this == today) to (this == yesterday)
}
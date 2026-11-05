package com.foxynest.testappforonboarding.other.extensions

import android.util.Log
import com.foxynest.testappforonboarding.other.Resource
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

/**
 * Преобразует LocalDateTime в миллисекунды для AlarmManager.
 */
fun LocalDateTime.toEpochMillis(): Long =
    this.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
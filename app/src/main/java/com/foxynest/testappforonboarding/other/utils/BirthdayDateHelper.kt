package com.foxynest.testappforonboarding.other.utils

import android.util.Log
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object BirthdayDateHelper {

//    /**
//     * Возвращает день месяца с учётом високосности.
//     * Если 29 февраля в невисокосный год – возвращает 28.
//     */
//    fun getAdjustedDayOfMonth(year: Int, month: Int, day: Int): Int {
//        if (month == Calendar.FEBRUARY && day == 29) {
//            val isLeap = java.time.Year.of(year).isLeap // или GregorianCalendar().isLeapYear(year)
//            return if (isLeap) 29 else 28
//        }
//        return day
//    }

//    /**
//     * Возвращает календарь для дня рождения в указанном году с учётом корректировки 29 февраля.
//     */
//    fun getBirthdayCalendarForYear(year: Int, birthMonth: Int, birthDay: Int): Calendar {
//        val adjustedDay = getAdjustedDayOfMonth(year, birthMonth, birthDay)
//        return Calendar.getInstance().apply {
//            set(Calendar.YEAR, year)
//            set(Calendar.MONTH, birthMonth)
//            set(Calendar.DAY_OF_MONTH, adjustedDay)
//            set(Calendar.HOUR_OF_DAY, 0)
//            set(Calendar.MINUTE, 0)
//            set(Calendar.SECOND, 0)
//            set(Calendar.MILLISECOND, 0)
//        }
//    }

    /**
     * Проверяет, является ли дата сегодня или вчера днём рождения.
     * Возвращает пару (isToday, isYesterday) с учётом корректировки високосного года.
     * @param nowDate текущая дата
     * @param birthDate дата рождения в формате "yyyy-MM-dd" (или Calendar)
     */
//    fun checkIfBirthdayTodayOrYesterday(nowDate: Date, birthDate: Calendar): Pair<Boolean, Boolean> {
//        val nowCal = Calendar.getInstance().apply { time = nowDate }
//        val birthMonth = birthDate.get(Calendar.MONTH)
//        val birthDayOfMonth = birthDate.get(Calendar.DAY_OF_MONTH)
//        val currentYear = nowCal.get(Calendar.YEAR)
//
//        // Скорректированная дата дня рождения в текущем году
//        val birthdayThisYear = getBirthdayCalendarForYear(currentYear, birthMonth, birthDayOfMonth)
//
//        val isToday = birthdayThisYear.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
//                birthdayThisYear.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)
//
//        val yesterdayCal = Calendar.getInstance().apply {
//            time = nowDate
//            add(Calendar.DAY_OF_YEAR, -1)
//        }
//        val isYesterday = birthdayThisYear.get(Calendar.YEAR) == yesterdayCal.get(Calendar.YEAR) &&
//                birthdayThisYear.get(Calendar.DAY_OF_YEAR) == yesterdayCal.get(Calendar.DAY_OF_YEAR)
//
//        return Pair(isToday, isYesterday)
//    }

//    // Перегрузка для удобства (принимает строку с датой)
//    fun checkIfBirthdayTodayOrYesterday(nowDate: Date, birthDateString: String): Pair<Boolean, Boolean>? {
//        val birthCal = birthDateString.parseBirthDateStringToCalendar() ?: return null
//        return checkIfBirthdayTodayOrYesterday(nowDate, birthCal)
//    }

//    /**
//    * Проверяет, является ли дата сегодня или вчера днём рождения.
//    * Возвращает пару isToday to isYesterday с учётом корректировки високосного года.
//    * @param nowDate текущая дата
//    * @param birthDate дата рождения в формате "yyyy-MM-dd" (или Calendar)
//    */
//    fun checkIfBirthdayTodayOrYesterday(nowDate: Date, birthDate: Calendar): Pair<Boolean, Boolean> {
//        val nowCal = Calendar.getInstance().apply { time = nowDate }
//        val birthMonth = birthDate.get(Calendar.MONTH)
//        val birthDay = birthDate.get(Calendar.DAY_OF_MONTH)
//        val currentYear = nowCal.get(Calendar.YEAR)
//
//        val birthdayThisYear = getBirthdayCalendarForYear(currentYear, birthMonth, birthDay)
//
//        val isToday = birthdayThisYear.get(Calendar.MONTH) == nowCal.get(Calendar.MONTH) &&
//                birthdayThisYear.get(Calendar.DAY_OF_MONTH) == nowCal.get(Calendar.DAY_OF_MONTH)
//
//        val yesterdayCal = Calendar.getInstance().apply {
//            time = nowDate
//            add(Calendar.DAY_OF_YEAR, -1)
//        }
//        val isYesterday = birthdayThisYear.get(Calendar.MONTH) == yesterdayCal.get(Calendar.MONTH) &&
//                birthdayThisYear.get(Calendar.DAY_OF_MONTH) == yesterdayCal.get(Calendar.DAY_OF_MONTH)
//
//        return isToday to isYesterday
//    }

//    fun isBirthdayTodayOrYesterday(birthDate: LocalDate, now: LocalDate): Pair<Boolean, Boolean> {
//        val birthdayThisYear = birthDate.withYear(now.year)
//        val isToday = birthdayThisYear == now
//        val isYesterday = birthdayThisYear == now.minusDays(1)
//        return isToday to isYesterday
//    }
//
//    fun nextBirthdayMorning(birthDate: LocalDate, now: LocalDateTime): Long {
//        var birthdayThisYear = birthDate.withYear(now.year)
//        if (birthdayThisYear.isBefore(now.toLocalDate()) ||
//            (birthdayThisYear == now.toLocalDate() && now.toLocalTime() >= LocalTime.of(9, 0))) {
//            birthdayThisYear = birthdayThisYear.plusYears(1)
//        }
//        val target = LocalDateTime.of(birthdayThisYear, LocalTime.of(9, 0))
//        return target.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
//    }
//
//    /**
//     * Вычисляет ближайшее 9:00 утра дня рождения.
//     */
//    fun calculateNextBirthdayMorning(birthCal: Calendar, now: Date): Long {
//        val nowCal = Calendar.getInstance().apply { time = now }
//        var targetYear = nowCal.get(Calendar.YEAR)
//        val birthMonth = birthCal.get(Calendar.MONTH)
//        val birthDay = birthCal.get(Calendar.DAY_OF_MONTH)
//
//        var targetCal = getBirthdayCalendarForYear(targetYear, birthMonth, birthDay)
//        // Устанавливаем время 9:00
//        targetCal.set(Calendar.HOUR_OF_DAY, BIRTH_NOTIF_TARGET_HOUR)
//        targetCal.set(Calendar.MINUTE, BIRTH_NOTIF_TARGET_MINUTE)
//        targetCal.set(Calendar.SECOND, 0)
//        targetCal.set(Calendar.MILLISECOND, 0)
//
//        // Если время в прошлом (или сегодня до 9:00 уже прошло), переходим на следующий год
//        while (targetCal.timeInMillis <= now.time) {
//            targetYear++
//            targetCal = getBirthdayCalendarForYear(targetYear, birthMonth, birthDay)
//            targetCal.set(Calendar.HOUR_OF_DAY, BIRTH_NOTIF_TARGET_HOUR)
//            targetCal.set(Calendar.MINUTE, BIRTH_NOTIF_TARGET_MINUTE)
//            targetCal.set(Calendar.SECOND, 0)
//            targetCal.set(Calendar.MILLISECOND, 0)
//        }
//        return targetCal.timeInMillis
//    }

    /**
     * Проверяет, является ли переданная дата сегодня или вчера.
     * Возвращает пару (isToday, isYesterday).
     */
    fun isBirthdayTodayOrYesterday(birthDate: LocalDate, now: LocalDate): Pair<Boolean, Boolean> {
        val birthdayThisYear = birthDate.withYear(now.year)
        val isToday = birthdayThisYear == now
        val isYesterday = birthdayThisYear == now.minusDays(1)
        return isToday to isYesterday
    }

    /**
     * Вычисляет timestamp (в миллисекундах) ближайшего 9:00 утра дня рождения.
     * Если сегодня день рождения и время уже >= 9:00 – берёт следующий год.
     */
    fun nextBirthdayMorning(birthDate: LocalDate, now: LocalDateTime): Long {
        var birthdayThisYear = birthDate.withYear(now.year)
        // Если день рождения в этом году уже прошёл (или сегодня, но уже после 9:00) – берём следующий год
        if (birthdayThisYear.isBefore(now.toLocalDate()) ||
            (birthdayThisYear == now.toLocalDate() && now.toLocalTime() >= LocalTime.of(9, 0))
        ) {
            birthdayThisYear = birthdayThisYear.plusYears(1)
        }
        val target = LocalDateTime.of(birthdayThisYear, LocalTime.of(9, 0))
        return target.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

//    fun parseBirthDate(dateStr: String): LocalDate? {
//        return try {
//            LocalDate.parse(dateStr) // формат yyyy-MM-dd
//        } catch (e: Exception) {
//            Log.e("BirthdayDateHelper", "Ошибка парсинга даты: $dateStr", e)
//            null
//        }
//    }
}
package com.foxynest.testappforonboarding.domain.birthday

import com.foxynest.testappforonboarding.domain.birthday.BirthdayRules.NOTIFY_TIME
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Чистая бизнес-логика дня рождения (без Android), полностью покрыта unit-тестами.
 *
 * Правила:
 * - 29 февраля в невисокосный год празднуем 28 февраля (так работает LocalDate.withYear).
 * - Поздравляем только в сам день рождения. Если устройство было выключено весь день – поздравления нет.
 * - В фоне (перезагрузка, смена времени, срабатывание будильника) не поздравляем раньше [NOTIFY_TIME],
 *   чтобы не беспокоить человека в 6 утра при включении телефона.
 * - Если пользователь сам в приложении (сохранил анкету, открыл приложение) – поздравляем сразу, в любое время дня.
 */
object BirthdayRules {
    val NOTIFY_TIME: LocalTime = LocalTime.of(9, 0)

    fun birthdayInYear(birthDate: LocalDate, year: Int): LocalDate = birthDate.withYear(year)

    fun isBirthdayToday(birthDate: LocalDate, today: LocalDate): Boolean =
        !birthDate.isAfter(today) && birthdayInYear(birthDate, today.year) == today

    /**
     * Ключ «уже поздравили»: дата рождения + дата праздника.
     * Исправление опечатки в имени не приводит к повторному поздравлению,
     * а смена ребёнка на другого (с другой датой рождения) – приводит.
     */
    fun occurrenceKey(birthDate: LocalDate, today: LocalDate): String = "$birthDate@$today"

    /**
     * Ближайший момент для будильника строго после [now]:
     * сегодня в [NOTIFY_TIME], если сегодня день рождения и время ещё не наступило, иначе – следующий день рождения.
     */
    fun nextNotificationTime(birthDate: LocalDate, now: LocalDateTime): LocalDateTime {
        val today = now.toLocalDate()
        val thisYear = birthdayInYear(birthDate, today.year)
        val date = when {
            thisYear.isAfter(today) -> thisYear
            thisYear == today && now.toLocalTime().isBefore(NOTIFY_TIME) -> thisYear
            // Берём год от исходной даты, а не thisYear.plusYears(1): иначе 29.02 после невисокосного года «залипнет» на 28.02
            else -> birthdayInYear(birthDate, today.year + 1)
        }
        return LocalDateTime.of(date, NOTIFY_TIME)
    }

    fun shouldNotifyNow(
        birthDate: LocalDate,
        now: LocalDateTime,
        lastNotifiedKey: String?,
        userIsInApp: Boolean
    ): Boolean {
        val today = now.toLocalDate()
        if (!isBirthdayToday(birthDate, today)) return false
        if (lastNotifiedKey == occurrenceKey(birthDate, today)) return false
        return userIsInApp || !now.toLocalTime().isBefore(NOTIFY_TIME)
    }
}
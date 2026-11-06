package com.foxynest.testappforonboarding.domain.birthday

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Все сценарии из постановки задачи о поздравлении – по одному тесту на сценарий.
 * Названия тестов – это и есть правила: если поменяется поведение, сначала меняется тест.
 * Время «сейчас» передаётся параметром, поэтому любой день проверяется мгновенно, без эмулятора.
 */
class BirthdayRulesTest {

    private val birthDate = LocalDate.of(2006, 9, 15)

    private fun at(date: String, time: String) = LocalDateTime.parse("${date}T$time")

    // ---------- nextNotificationTime ----------

    @Test
    fun `saved in the evening before birthday - notify next morning, not in 24 hours`() {
        val next = BirthdayRules.nextNotificationTime(birthDate, at("2026-09-14", "21:30"))
        assertEquals(at("2026-09-15", "09:00"), next)
    }

    @Test
    fun `birthday today before 9 - schedule today at 9`() {
        val next = BirthdayRules.nextNotificationTime(birthDate, at("2026-09-15", "06:00"))
        assertEquals(at("2026-09-15", "09:00"), next)
    }

    @Test
    fun `birthday today after 9 - schedule next year`() {
        val next = BirthdayRules.nextNotificationTime(birthDate, at("2026-09-15", "09:00"))
        assertEquals(at("2027-09-15", "09:00"), next)
    }

    @Test
    fun `birthday already passed this year - schedule next year`() {
        val next = BirthdayRules.nextNotificationTime(birthDate, at("2026-12-01", "12:00"))
        assertEquals(at("2027-09-15", "09:00"), next)
    }

    @Test
    fun `leap day birthday after non-leap year goes back to 29 february`() {
        val leapBirthDate = LocalDate.of(2020, 2, 29)
        // В 2027 празднуем 28.02, следующий – 29.02.2028 (прежний код через plusYears(1) давал 28.02.2028)
        val next = BirthdayRules.nextNotificationTime(leapBirthDate, at("2027-02-28", "10:00"))
        assertEquals(at("2028-02-29", "09:00"), next)
    }

    @Test
    fun `leap day birthday in non-leap year is celebrated on 28 february`() {
        val leapBirthDate = LocalDate.of(2020, 2, 29)
        val next = BirthdayRules.nextNotificationTime(leapBirthDate, at("2027-01-10", "10:00"))
        assertEquals(at("2027-02-28", "09:00"), next)
    }

    @Test
    fun `new year eve birthday schedules correctly across the year boundary`() {
        val next = BirthdayRules.nextNotificationTime(
            LocalDate.of(2010, 12, 31),
            at("2026-12-31", "23:00")
        )
        assertEquals(at("2027-12-31", "09:00"), next)
    }

    // ---------- shouldNotifyNow ----------

    @Test
    fun `user saves form on birthday - notify immediately at any time`() {
        assertTrue(
            BirthdayRules.shouldNotifyNow(
                birthDate,
                at("2026-09-15", "07:00"),
                null,
                userIsInApp = true
            )
        )
        assertTrue(
            BirthdayRules.shouldNotifyNow(
                birthDate,
                at("2026-09-15", "22:00"),
                null,
                userIsInApp = true
            )
        )
    }

    @Test
    fun `phone turned on at 6 on birthday - wait until 9`() {
        assertFalse(
            BirthdayRules.shouldNotifyNow(
                birthDate,
                at("2026-09-15", "06:00"),
                null,
                userIsInApp = false
            )
        )
    }

    @Test
    fun `phone was off at 9 and turned on at 10 - notify`() {
        assertTrue(
            BirthdayRules.shouldNotifyNow(
                birthDate,
                at("2026-09-15", "10:00"),
                null,
                userIsInApp = false
            )
        )
    }

    @Test
    fun `already notified today - do not notify again`() {
        val key = BirthdayRules.occurrenceKey(birthDate, LocalDate.of(2026, 9, 15))
        assertFalse(
            BirthdayRules.shouldNotifyNow(
                birthDate,
                at("2026-09-15", "12:00"),
                key,
                userIsInApp = true
            )
        )
    }

    @Test
    fun `notified last year - notify this year`() {
        val lastYearKey = BirthdayRules.occurrenceKey(birthDate, LocalDate.of(2025, 9, 15))
        assertTrue(
            BirthdayRules.shouldNotifyNow(
                birthDate,
                at("2026-09-15", "12:00"),
                lastYearKey,
                userIsInApp = false
            )
        )
    }

    @Test
    fun `another child with the same birthday is notified even if first one was`() {
        val firstChildKey = BirthdayRules.occurrenceKey(birthDate, LocalDate.of(2026, 9, 15))
        val secondChild = LocalDate.of(2015, 9, 15)
        assertTrue(
            BirthdayRules.shouldNotifyNow(
                secondChild,
                at("2026-09-15", "12:00"),
                firstChildKey,
                userIsInApp = true
            )
        )
    }

    @Test
    fun `day after birthday - no belated notification`() {
        assertFalse(
            BirthdayRules.shouldNotifyNow(
                birthDate,
                at("2026-09-16", "00:05"),
                null,
                userIsInApp = false
            )
        )
        assertFalse(
            BirthdayRules.shouldNotifyNow(
                birthDate,
                at("2026-09-16", "10:00"),
                null,
                userIsInApp = true
            )
        )
    }

    @Test
    fun `old birth year works`() {
        assertTrue(
            BirthdayRules.isBirthdayToday(
                LocalDate.of(1950, 9, 15),
                LocalDate.of(2026, 9, 15)
            )
        )
    }

    @Test
    fun `future birth date is never a birthday`() {
        assertFalse(
            BirthdayRules.isBirthdayToday(
                LocalDate.of(2027, 9, 15),
                LocalDate.of(2026, 9, 15)
            )
        )
    }
}
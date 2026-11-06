package com.foxynest.testappforonboarding.other.timeProvider

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * «Часы» приложения. Код берёт текущее время отсюда, а не из `LocalDateTime.now()` напрямую.
 *
 * Зачем: в тестах можно подставить [FixedTimeProvider] и сказать «сейчас 15 сентября 06:00» –
 * так проверяется логика дня рождения без ожидания реальной даты и без перевода часов на телефоне.
 * В приложении Hilt подставляет [SystemTimeProvider] (см. `SingletonModule.provideTimeProvider`).
 */
interface ITimeProvider {
    fun getCurrentDate(): LocalDate
    fun getCurrentDateTime(): LocalDateTime
}
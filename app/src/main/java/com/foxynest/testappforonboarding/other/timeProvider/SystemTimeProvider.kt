package com.foxynest.testappforonboarding.other.timeProvider

import org.jetbrains.annotations.VisibleForTesting
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

/** Настоящее время телефона (в его часовом поясе). */
class SystemTimeProvider @Inject constructor() : ITimeProvider {
    override fun getCurrentDate(): LocalDate = LocalDate.now()
    override fun getCurrentDateTime(): LocalDateTime = LocalDateTime.now()
}

/**
 * «Остановленные часы» для тестов: всегда возвращают переданное время.
 * Используется в `BirthdayNotificationRepositoryTest`. `@VisibleForTesting` – пометка «только для тестов»,
 * Android Studio предупредит, если вызвать его из обычного кода.
 */
@VisibleForTesting
class FixedTimeProvider(private val fixedDate: LocalDate, private val fixedTime: LocalDateTime) :
    ITimeProvider {
    override fun getCurrentDate(): LocalDate = fixedDate
    override fun getCurrentDateTime(): LocalDateTime = fixedTime
}
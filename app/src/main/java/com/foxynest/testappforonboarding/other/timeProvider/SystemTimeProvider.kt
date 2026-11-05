package com.foxynest.testappforonboarding.other.timeProvider

import org.jetbrains.annotations.VisibleForTesting
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

class SystemTimeProvider @Inject constructor() : ITimeProvider {
    override fun getCurrentDate(): LocalDate = LocalDate.now()
    override fun getCurrentDateTime(): LocalDateTime = LocalDateTime.now()
}

// Для тестов можно использовать фиксированное время
@VisibleForTesting
class FixedTimeProvider(private val fixedDate: LocalDate, private val fixedTime: LocalDateTime) :
    ITimeProvider {
    override fun getCurrentDate(): LocalDate = fixedDate
    override fun getCurrentDateTime(): LocalDateTime = fixedTime
}
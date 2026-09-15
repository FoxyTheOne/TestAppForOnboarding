package com.foxynest.testappforonboarding.other.timeProvider

import org.jetbrains.annotations.VisibleForTesting
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Date
import javax.inject.Inject

//Полный переход на LocalDate/LocalDateTime в репозитории и воркере – теперь учёт високосных годов
// выполняется автоматически через withYear(), без ручных корректировок
class SystemTimeProvider @Inject constructor() : ITimeProvider {
    //    override fun getCurrentTime(): Date = Date()
    override fun getCurrentDate(): LocalDate = LocalDate.now()
    override fun getCurrentDateTime(): LocalDateTime = LocalDateTime.now()
}

// Для тестов можно использовать фиксированное время
@VisibleForTesting
class FixedTimeProvider(private val fixedDate: LocalDate, private val fixedTime: LocalDateTime) : ITimeProvider {
    //    override fun getCurrentTime(): Date = fixedDate
    override fun getCurrentDate(): LocalDate = fixedDate
    override fun getCurrentDateTime(): LocalDateTime = fixedTime
}

//// В тестах:
//val testProvider = FixedTimeProvider(Date()) // можно подставить любую дату

//// Фиктивный TimeProvider для тестов
//class FakeTimeProvider(private var currentTime: Date) : ITimeProvider {
//    override fun getCurrentTime(): Date = currentTime
//    fun advanceDays(days: Int) {
//        currentTime = Date(currentTime.time + days * 24 * 60 * 60 * 1000L)
//    }
//}
package com.foxynest.testappforonboarding.other.timeProvider

import org.jetbrains.annotations.VisibleForTesting
import java.util.Date
import javax.inject.Inject

class SystemTimeProvider @Inject constructor() : ITimeProvider {
    override fun getCurrentTime(): Date = Date()
}

// Для тестов можно использовать фиксированное время
@VisibleForTesting
class FixedTimeProvider(private val fixedDate: Date) : ITimeProvider {
    override fun getCurrentTime(): Date = fixedDate
}

//// В тестах:
//val testProvider = FixedTimeProvider(Date()) // можно подставить любую дату
package com.foxynest.testappforonboarding.other.timeProvider

import java.time.LocalDate
import java.time.LocalDateTime

//interface ITimeProvider {
//    fun getCurrentTime(): Date
//}

interface ITimeProvider {
    fun getCurrentDate(): LocalDate
    fun getCurrentDateTime(): LocalDateTime
}
package com.foxynest.testappforonboarding.other.timeProvider

import java.util.Date

interface ITimeProvider {
    fun getCurrentTime(): Date
}